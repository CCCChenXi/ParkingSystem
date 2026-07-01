package com.xigeandwillian.parkingsystem.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xigeandwillian.parkingsystem.admin.dto.parkinglot.LotSaveDTO;
import com.xigeandwillian.parkingsystem.admin.mapper.ParkingLotMapper;
import com.xigeandwillian.parkingsystem.admin.mapper.ParkingSpotMapper;
import com.xigeandwillian.parkingsystem.admin.service.Service.ParkingLotService;
import com.xigeandwillian.parkingsystem.admin.vo.parkinglot.LotListVO;
import com.xigeandwillian.parkingsystem.admin.vo.parkinglot.LotNameListVO;
import com.xigeandwillian.parkingsystem.client.mapper.ParkingOrderMapper;
import com.xigeandwillian.parkingsystem.common.cache.ParkingCache;
import com.xigeandwillian.parkingsystem.common.constant.CacheConstant;
import com.xigeandwillian.parkingsystem.common.constant.OrderConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import cn.hutool.json.JSONUtil;
import com.xigeandwillian.parkingsystem.common.entity.ParkingOrder;
import com.xigeandwillian.parkingsystem.common.entity.ParkingSpot;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.result.PageResult;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingLotServiceImpl implements ParkingLotService {

    private final ParkingLotMapper parkingLotMapper;
    private final ParkingOrderMapper parkingOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ParkingCache parkingCache;
    private final ParkingSpotMapper parkingSpotMapper;
    private final RedissonClient redissonClient;
    private final CacheManager cacheManager;
    private static final DefaultRedisScript<Long> REBUILD_SCRIPT;

    static {
        REBUILD_SCRIPT = new DefaultRedisScript<>();
        REBUILD_SCRIPT.setLocation(new ClassPathResource("lua/rebuildLot.lua"));
        REBUILD_SCRIPT.setResultType(Long.class);
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.PARKING_LOT_NAME_LIST)
    public Result listParkingLotNames() {
        log.info("开始从数据库查询停车场名称列表...");
        LambdaQueryWrapper<ParkingLot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(ParkingLot::getId, ParkingLot::getName);
        List<ParkingLot> lots = parkingLotMapper.selectList(queryWrapper);
        List<LotNameListVO> voList = lots.stream().map(lot -> {
            LotNameListVO vo = new LotNameListVO();
            vo.setId(lot.getId());
            vo.setName(lot.getName());
            return vo;
        }).collect(Collectors.toList());
        return Result.ok(voList);
    }

    @Override
    @CacheEvict(cacheNames = {CacheConstant.PARKING_LOT_NAME_LIST, CacheConstant.PARKING_LOT_LIST},
            allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public Result createParkingLot(LotSaveDTO lotSaveDTO) {
        try {
            ParkingLot parkingLot = new ParkingLot();
            BeanUtils.copyProperties(lotSaveDTO, parkingLot);
            parkingLot.setTotalSpots(RedisConstant.Parking.DEFAULT_TOTAL_SPOTS);
            parkingLot.setAvailableSpots(RedisConstant.Parking.DEFAULT_TOTAL_SPOTS);
            parkingLotMapper.insert(parkingLot);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    stringRedisTemplate.opsForGeo().add(RedisConstant.Parking.PARKING_GEO, new Point(parkingLot.getLongitude().doubleValue(), parkingLot.getLatitude().doubleValue()), parkingLot.getId().toString());
                }
            });
            log.info("新增停车场: lotId={}", parkingLot.getId());
            return Result.ok();
        } catch (Exception e) {
            log.error("新增停车场失败: name={}", lotSaveDTO.getName(), e);
            throw new BusinessException(ResultConstant.SYSTEM_ERROR, "新增停车场失败");
        }
    }

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
                return Result.ok();
            }

            ParkingLot parkingLot = new ParkingLot();
            BeanUtils.copyProperties(lotSaveDTO, parkingLot);
            parkingLot.setId(id);
            parkingLotMapper.updateById(parkingLot);

            //确保只有在事务提交之后才去删除缓存
            //删除停车场信息列表失败，那缓存也不会被删除，之后在用户访问这个停车场时不需要重建缓存
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            if (geoChanged) {
                                stringRedisTemplate.opsForGeo().add(RedisConstant.Parking.PARKING_GEO,
                                        new Point(lotSaveDTO.getLongitude().doubleValue(), lotSaveDTO.getLatitude().doubleValue()),
                                        id.toString());
                            }

                            Cache lotListCache = cacheManager.getCache(CacheConstant.PARKING_LOT_LIST);
                            if (lotListCache != null) {
                                lotListCache.clear();
                            }
                            if (nameChanged) {
                                Cache nameListCache = cacheManager.getCache(CacheConstant.PARKING_LOT_NAME_LIST);
                                if (nameListCache != null) {
                                    nameListCache.clear();
                                }
                            }
                            stringRedisTemplate.delete(RedisConstant.Parking.PARKING_LOT_INFO + id);
                            log.info("更新停车场: lotId={}", id);
                        }
                    }
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新停车场失败: lotId={}", id, e);
            throw new BusinessException(ResultConstant.SYSTEM_ERROR, "更新停车场失败");
        }
        return Result.ok();
    }

    /**
     * 删除停车场，并清理其所有缓存
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConstant.PARKING_LOT_NAME_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConstant.PARKING_SPOT_LIST, key = "#id"),
            @CacheEvict(cacheNames = CacheConstant.PARKING_LOT_LIST, allEntries = true),
    })
    public Result deleteParkingLot(Long id) {
        //不需要担心在我们删除的时候会有新的车辆进入，因为在删除一个停车场之前会先将停车场的状态设置为关闭
        String s = stringRedisTemplate.opsForValue().get(RedisConstant.Parking.PARKING_LOT_INFO + id);
        Object count = stringRedisTemplate.opsForHash().get(RedisConstant.Parking.PARKING_LOT_AVAILABLE, id.toString());
        if (s == null || count == null) {
            LambdaQueryWrapper<ParkingOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(ParkingOrder::getLotId, id)
                    .eq(ParkingOrder::getStatus, OrderConstant.ORDER_STATUS_IN_PROGRESS);
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
        try {
            parkingLotMapper.deleteById(id);
            stringRedisTemplate.delete(RedisConstant.Parking.PARKING_LOT_INFO + id);
            stringRedisTemplate.opsForGeo().remove(RedisConstant.Parking.PARKING_GEO, id.toString());
            stringRedisTemplate.opsForHash().delete(RedisConstant.Parking.PARKING_LOT_AVAILABLE, id.toString());
            stringRedisTemplate.delete(RedisConstant.Parking.PARKING_SPOT_STATUS + id);
            log.info("删除停车场: lotId={}", id);
            return Result.ok();
        } catch (Exception e) {
            log.error("删除停车场失败: lotId={}", id, e);
            throw new BusinessException(ResultConstant.SYSTEM_ERROR, "删除停车场失败");
        }
    }


    @Override
    public Result listParkingLots(Integer page, Integer size, String keyword, Integer status) {
        log.info("分页查询停车场: page={}, size={}, keyword={}, status={}", page, size, keyword, status);
        if (keyword != null && keyword.length() > RedisConstant.Parking.KEYWORD_MAX_LENGTH) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "关键字过长");
        }
        Page<ParkingLot> p = new Page<>(page != null ? page : 1, size != null ? size : 15);
        LambdaQueryWrapper<ParkingLot> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(ParkingLot::getName, keyword)
                    .or().like(ParkingLot::getAddress, keyword));
        }
        if (status != null) {
            wrapper.eq(ParkingLot::getStatus, status);
        }
        Page<ParkingLot> result = parkingLotMapper.selectPage(p, wrapper);

        Map<String, Integer> availableMap = new HashMap<>();
        try {
            Map<Object, Object> entries = stringRedisTemplate.opsForHash()
                    .entries(RedisConstant.Parking.PARKING_LOT_AVAILABLE);
            entries.forEach((k, v) -> availableMap.put(k.toString(), Integer.parseInt(v.toString())));
        } catch (Exception e) {
            log.warn("Redis获取可用车位失败，降级到数据库", e);
        }

        List<LotListVO> voList = result.getRecords().stream().map(lot -> {
            LotListVO vo = new LotListVO();
            BeanUtils.copyProperties(lot, vo);
            Integer available = availableMap.get(lot.getId().toString());
            if (available != null) {
                vo.setAvailableSpots(available);
            } else {
                LambdaQueryWrapper<ParkingOrder> countWrapper = new LambdaQueryWrapper<>();
                countWrapper.eq(ParkingOrder::getLotId, lot.getId())
                        .in(ParkingOrder::getStatus, OrderConstant.ORDER_STATUS_RESERVED, OrderConstant.ORDER_STATUS_IN_PROGRESS);
                long orderCount = parkingOrderMapper.selectCount(countWrapper);
                vo.setAvailableSpots(lot.getTotalSpots() - (int) orderCount);
            }
            return vo;
        }).collect(Collectors.toList());
        PageResult<LotListVO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setDataList(voList);
        return Result.ok(pageResult);
    }
}
