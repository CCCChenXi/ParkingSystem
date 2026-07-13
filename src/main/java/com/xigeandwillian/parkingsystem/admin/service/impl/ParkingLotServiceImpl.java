package com.xigeandwillian.parkingsystem.admin.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xigeandwillian.parkingsystem.admin.dto.parkinglot.LotSaveDTO;
import com.xigeandwillian.parkingsystem.admin.service.ParkingLotService;
import com.xigeandwillian.parkingsystem.admin.vo.parkinglot.LotListVO;
import com.xigeandwillian.parkingsystem.admin.vo.parkinglot.LotNameListVO;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingLotConverter;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingLotMapper;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingOrderMapper;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingSpotMapper;
import com.xigeandwillian.parkingsystem.admin.mq.ParkingLotCacheInitEvent;
import com.xigeandwillian.parkingsystem.common.constant.CacheConstant;
import com.xigeandwillian.parkingsystem.common.constant.MQConstant;
import com.xigeandwillian.parkingsystem.common.constant.OrderConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.entity.ParkingOrder;
import com.xigeandwillian.parkingsystem.common.entity.ParkingSpot;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.result.PageResult;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.geo.Point;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingLotServiceImpl implements ParkingLotService {

    private final ParkingLotConverter parkingLotConverter;
    private final ParkingLotMapper parkingLotMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final ParkingOrderMapper parkingOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    private final CacheManager cacheManager;
    private static final DefaultRedisScript<Long> REBUILD_SCRIPT;

    static {
        REBUILD_SCRIPT = new DefaultRedisScript<>();
        REBUILD_SCRIPT.setLocation(new ClassPathResource("lua/rebuildLot.lua"));
        REBUILD_SCRIPT.setResultType(Long.class);
    }

    /**
     * 获取停车场名称列表
     */
    @Override
    @Cacheable(cacheNames = CacheConstant.PARKING_LOT_NAME_LIST)
    public Result listParkingLotNames() {
        log.info("开始从数据库查询停车场名称列表...");
        LambdaQueryWrapper<ParkingLot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(ParkingLot::getId, ParkingLot::getName);
        List<ParkingLot> lots = parkingLotMapper.selectList(queryWrapper);
        List<LotNameListVO> voList = lots.stream().map(lot -> parkingLotConverter.toNameListVO(lot)).collect(Collectors.toList());
        return Result.ok(voList);
    }

    /**
     * 新增停车场
     * DB 插入后发 MQ 消息异步初始化 GEO 缓存，失败走死信队列重试
     */
    @Override
    @CacheEvict(cacheNames = {CacheConstant.PARKING_LOT_NAME_LIST, CacheConstant.PARKING_LOT_LIST},
            allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public Result createParkingLot(LotSaveDTO lotSaveDTO) {
        try {
            ParkingLot parkingLot = parkingLotConverter.toEntity(lotSaveDTO);
            parkingLot.setTotalSpots(RedisConstant.Parking.DEFAULT_TOTAL_SPOTS);
            parkingLot.setAvailableSpots(RedisConstant.Parking.DEFAULT_TOTAL_SPOTS);
            parkingLotMapper.insert(parkingLot);
            rabbitTemplate.convertAndSend(MQConstant.PARKING_LOT_CACHE_INIT_DELAY_QUEUE,
                    new ParkingLotCacheInitEvent(parkingLot.getId(),
                            parkingLot.getLongitude().doubleValue(),
                            parkingLot.getLatitude().doubleValue(), 0));
            log.info("新增停车场: lotId={}", parkingLot.getId());
            return Result.ok();
        } catch (Exception e) {
            log.error("新增停车场失败: name={}", lotSaveDTO.getName(), e);
            throw new BusinessException(ResultConstant.SYSTEM_ERROR, "新增停车场失败");
        }
    }

    /**
     * 更新停车场信息
     * 更新成功后同步清理相关缓存，缓存操作失败则回滚事务
     * 如果清理缓存失败，我们将回滚事务，确保不让脏数据遗留缓存
     */
    @Override
    @Transactional
    public Result updateParkingLot(Long id, LotSaveDTO lotSaveDTO) {
        try {
            String cached = stringRedisTemplate.opsForValue()
                    .get(RedisConstant.Parking.PARKING_LOT_INFO + id);
            ParkingLot existing;
            if (cached != null) {
                existing = JSONUtil.toBean(cached, ParkingLot.class);
            } else {
                existing = parkingLotMapper.selectById(id);
            }
            if (existing == null) {
                throw new BusinessException(ResultConstant.BAD_REQUEST, "停车场不存在");
            }

            boolean nameChanged = !existing.getName().equals(lotSaveDTO.getName());
            boolean geoChanged = existing.getLongitude().compareTo(lotSaveDTO.getLongitude()) != 0
                    || existing.getLatitude().compareTo(lotSaveDTO.getLatitude()) != 0;
            boolean otherChanged = !existing.getAddress().equals(lotSaveDTO.getAddress())
                    || !existing.getStatus().equals(lotSaveDTO.getStatus());

            if (!nameChanged && !geoChanged && !otherChanged) {
                throw new BusinessException(ResultConstant.BAD_REQUEST, "没有信息被修改");
            }

            ParkingLot parkingLot = parkingLotConverter.toEntity(lotSaveDTO);
            parkingLot.setId(id);
            parkingLotMapper.updateById(parkingLot);

            if (nameChanged) {
                Cache nameListCache = cacheManager.getCache(CacheConstant.PARKING_LOT_NAME_LIST);
                if (nameListCache != null) {
                    nameListCache.clear();
                }
            }
            if (geoChanged) {
                stringRedisTemplate.opsForGeo().add(RedisConstant.Parking.PARKING_GEO,
                        new Point(lotSaveDTO.getLongitude().doubleValue(), lotSaveDTO.getLatitude().doubleValue()),
                        id.toString());
            }
            Cache lotListCache = cacheManager.getCache(CacheConstant.PARKING_LOT_LIST);
            if (lotListCache != null) {
                lotListCache.clear();
            }
            stringRedisTemplate.delete(RedisConstant.Parking.PARKING_LOT_INFO + id);

            log.info("更新停车场成功: lotId={}", id);
            return Result.ok();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新停车场失败: lotId={}", id, e);
            throw new BusinessException(ResultConstant.SYSTEM_ERROR, "更新停车场失败");
        }
    }

    /**
     * 删除停车场，并清理其所有缓存
     * 删除流程：
     * 1. 先检查 Redis 缓存判断是否有进行中订单或车位被占用
     * 2. 删除数据库记录
     * 3. 事务提交后清理 Redis 缓存和 Spring Cache
     * 在到达此方法前，停车场状态已被设置为关闭，无需担心删除过程中有新车辆进入
     * 即使缓存删除失败，也会因为ttl过期而被自动删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteParkingLot(Long id) {
        String s = stringRedisTemplate.opsForValue().get(RedisConstant.Parking.PARKING_LOT_INFO + id);
        Object count = stringRedisTemplate.opsForHash().get(RedisConstant.Parking.PARKING_AVAILABLE_SPOTS, id.toString());
        //删除停车场之前去查找缓存查找停车场信息和可用车位，如果都是有数据，可以直接去用车位总数和可用车位数比对判断能不能删除
        //如果没有数据，我们去查询订单信息，该停车场是否存在未结算的订单
        if (s == null || count == null) {
            LambdaQueryWrapper<ParkingOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            //获取停车场预约和进行中的订单，如果有订单则停车场不能被删除
            lambdaQueryWrapper.eq(ParkingOrder::getLotId, id)
                    .in(ParkingOrder::getStatus, OrderConstant.ORDER_STATUS_IN_PROGRESS, OrderConstant.ORDER_STATUS_RESERVED);
            Long progressOrders = parkingOrderMapper.selectCount(lambdaQueryWrapper);
            if (progressOrders != 0) {
                log.warn("存在进行中订单，无法删除停车场: lotId={}", id);
                throw new BusinessException(ResultConstant.BAD_REQUEST, "存在进行中订单，无法删除停车场");
            }
        } else {
            ParkingLot lot = JSONUtil.toBean(s, ParkingLot.class);
            if (!lot.getTotalSpots().equals(Integer.valueOf(count.toString()))) {
                log.warn("存在车位被占用，无法删除停车场: lotId={}", id);
                throw new BusinessException(ResultConstant.BAD_REQUEST, "存在车位被占用，无法删除停车场");
            }
        }
        //尝试去删除停车场信息和该停车场下所有车位，如果出现了异常导致删除失败，我们将异常抛出，不能让事务提交
        try {
            parkingSpotMapper.delete(new LambdaQueryWrapper<ParkingSpot>()
                    .eq(ParkingSpot::getLotId, id));
            parkingLotMapper.deleteById(id);
        } catch (Exception e) {
            log.error("删除停车场失败: lotId={}", id, e);
            throw new BusinessException(ResultConstant.SYSTEM_ERROR, "删除停车场失败");
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            //删除停车场涉及到的缓存有
                            //1.停车场名字列表(管理端用) 2.停车场车位状态表  3.停车场可用车位
                            //4.停车场geo  5.停车场静态信息缓存  6.车位静态信息缓存
                            stringRedisTemplate.delete(RedisConstant.Parking.PARKING_LOT_INFO + id);
                            stringRedisTemplate.opsForGeo().remove(RedisConstant.Parking.PARKING_GEO, id.toString());
                            stringRedisTemplate.opsForHash().delete(RedisConstant.Parking.PARKING_AVAILABLE_SPOTS, id.toString());
                            stringRedisTemplate.delete(RedisConstant.Parking.PARKING_SPOT_STATUS + id);
                            stringRedisTemplate.delete(RedisConstant.Parking.PARKING_LOT_LIST_ALL);
                            Cache nameListCache = cacheManager.getCache(CacheConstant.PARKING_LOT_NAME_LIST);
                            if (nameListCache != null) {
                                nameListCache.clear();
                            }
                            stringRedisTemplate.delete(RedisConstant.Parking.DASHBOARD_LOT_COUNT);
                            stringRedisTemplate.delete(RedisConstant.Parking.DASHBOARD_SPOT_COUNT);
                        } catch (Exception e) {
                            log.warn("删除停车场后清理缓存失败: lotId={}", id, e);
                        }
                    }
                }
        );

        log.info("删除停车场成功: lotId={}", id);
        return Result.ok();
    }


    /**
     * 分页查询停车场列表
     * 管理端数据不要求实时，允许延迟，所以不需要从redis获取实时可用车位
     * 分页模糊查询，管理端查询次数低频，不对数据进行缓存处理
     */
    @Override
    public Result listParkingLots(Integer page, Integer size, String keyword, Integer status) {
        log.info("分页查询停车场: page={}, size={}, keyword={}, status={}", page, size, keyword, status);
        if (keyword != null && keyword.length() > RedisConstant.Parking.KEYWORD_MAX_LENGTH) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "关键字过长");
        }
        Page<ParkingLot> p = new Page<>(page != null ? page : RedisConstant.Parking.DEFAULT_PAGE, size != null ? size : RedisConstant.Parking.DEFAULT_PAGE_SIZE);
        LambdaQueryWrapper<ParkingLot> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(ParkingLot::getName, keyword)
                    .or().like(ParkingLot::getAddress, keyword));
        }
        if (status != null) {
            wrapper.eq(ParkingLot::getStatus, status);
        }
        Page<ParkingLot> result = parkingLotMapper.selectPage(p, wrapper);
        List<LotListVO> voList = result.getRecords().stream().map(lot->{
            LotListVO vo = parkingLotConverter.toListVO(lot);
            return vo;
        }).collect(Collectors.toList());
        PageResult<LotListVO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setDataList(voList);
        return Result.ok(pageResult);
    }
}
