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
import com.xigeandwillian.parkingsystem.common.result.Result;
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
        }).toList();
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
    public Result listParkingLots() {
        log.info("查询停车场列表");
        try {
            //动静分离,静态文件存储成一个大json
            //不分离时，每次去更新车位状态都需要取出json->解析->修改->tojson->存入redis，造成了很多的时间浪费
            List<ParkingLot> list = parkingCache.getLotList();
            Map<Object, Object> map = stringRedisTemplate.opsForHash()
                    .entries(RedisConstant.Parking.PARKING_LOT_AVAILABLE);
            List<LotListVO> voList = list.stream().map(lot -> {
                LotListVO vo = new LotListVO();
                BeanUtils.copyProperties(lot, vo);
                Object availableSpots = map.get(lot.getId().toString());
                if (availableSpots == null) {
                    // 缓存缺失：total=0 则预期跳过，否则按 status 状态降级重建
                    if (lot.getTotalSpots() == 0) {
                        vo.setAvailableSpots(0);
                    } else {
                        Map<Object, Object> status = stringRedisTemplate.opsForHash()
                                .entries(RedisConstant.Parking.PARKING_SPOT_STATUS + lot.getId());
                        //根据车位状态表是否null将重建方法分成了全量重建和只重建可用车位
                        //为了使得查询和重建解耦，重建过程没有写着查询方法内
                        if (status.isEmpty()) {
                            if (fullRebuild(lot.getId())) {
                                Object count = stringRedisTemplate.opsForHash().get(
                                        RedisConstant.Parking.PARKING_LOT_AVAILABLE, lot.getId().toString());
                                vo.setAvailableSpots(count != null ? Integer.parseInt(count.toString()) : 0);
                                log.info("全量重建成功: lotId={}", lot.getId());
                            } else {
                                LambdaQueryWrapper<ParkingOrder> fallbackQuery = new LambdaQueryWrapper<>();
                                fallbackQuery.eq(ParkingOrder::getLotId, lot.getId())
                                        .between(ParkingOrder::getStatus,
                                                OrderConstant.ORDER_STATUS_RESERVED,
                                                OrderConstant.ORDER_STATUS_IN_PROGRESS);
                                long occupiedCount = parkingOrderMapper.selectCount(fallbackQuery);
                                int available = lot.getTotalSpots() - (int) occupiedCount;
                                log.warn("全量重建失败，DB 降级: lotId={}, available={}", lot.getId(), available);
                                vo.setAvailableSpots(Math.max(0, available));
                            }
                        } else {
                            availableRebuild(lot.getId(), vo, status);
                        }
                    }
                } else {
                    vo.setAvailableSpots(Integer.parseInt(availableSpots.toString()));
                }
                return vo;
            }).collect(Collectors.toList());
            return Result.ok(voList);
        } catch (Exception e) {
            log.error("查询停车场列表失败", e);
            throw new BusinessException(ResultConstant.SYSTEM_ERROR, "查询停车场列表失败");
        }
    }

    private void availableRebuild(Long lotId, LotListVO vo, Map<Object, Object> status) {
        long freeCount = status.values().stream().filter(RedisConstant.Parking.SPOT_STATUS_FREE::equals).count();

        RLock lock = redissonClient.getLock(RedisConstant.Parking.PARKING_LOT_REBUILD_LOCK + lotId);

        try {
            //加锁防止并发修改可用车位数导致覆盖问题

            //怎么保证可用车位数一定和车位状态表的数据一致呢？
            //在停车出车时，我们不仅需要修改车位状态表也需要修改可用车位数，当前因为可用车位数是null
            //导致了无法停车出车，使得在重建可用车位数之前，车位状态表不会发生改变也就杜绝了脏数据的出现

            //用户是否会因为可用车位数为null导致无法进出车辆？
            //不会，因为在用户发现了可用车位数为null时也将会去重建可用车位数，所以我们只需要将重建时的key统一即可
            if (lock.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    if (!stringRedisTemplate.opsForHash().hasKey(
                            RedisConstant.Parking.PARKING_LOT_AVAILABLE, lotId.toString())) {
                        stringRedisTemplate.opsForHash().put(
                                RedisConstant.Parking.PARKING_LOT_AVAILABLE,
                                lotId.toString(), String.valueOf(freeCount));
                        log.info("可用车位重建成功: lotId={}, count={}", lotId, freeCount);
                    } else {
                        Object val = stringRedisTemplate.opsForHash().get(
                                RedisConstant.Parking.PARKING_LOT_AVAILABLE, lotId.toString());
                        freeCount = val != null ? Long.parseLong(val.toString()) : freeCount;
                    }
                } finally {
                    lock.unlock();
                }
            }
            vo.setAvailableSpots((int) freeCount);
        } catch (Exception e) {
            log.error("可用车位重建异常: lotId={}", lotId, e);
            vo.setAvailableSpots((int) freeCount);
        }
    }

    private Boolean fullRebuild(Long id) {
        //共用一把锁，全量重建或者只重建可用车位，两个需求是矛盾的，不能同时存在
        RLock lock = redissonClient.getLock(RedisConstant.Parking.PARKING_LOT_REBUILD_LOCK + id);
        int tryTime = 3;
        try {
            while (tryTime != 0) {
                if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
                    try {
                        log.info("全量重建: lotId={}", id);
                        LambdaQueryWrapper<ParkingOrder> queryOrder = new LambdaQueryWrapper<>();
                        queryOrder.select(ParkingOrder::getSpotId)
                                .eq(ParkingOrder::getLotId, id)
                                .between(ParkingOrder::getStatus, OrderConstant.ORDER_STATUS_RESERVED, OrderConstant.ORDER_STATUS_IN_PROGRESS);
                        List<ParkingOrder> orderList = parkingOrderMapper.selectList(queryOrder);
                        LambdaQueryWrapper<ParkingSpot> querySpot = new LambdaQueryWrapper<>();
                        querySpot.select(ParkingSpot::getId)
                                .eq(ParkingSpot::getLotId, id);
                        List<ParkingSpot> spotList = parkingSpotMapper.selectList(querySpot);
                        Map<String, String> status = new HashMap<>();
                        spotList.forEach(spot -> {
                            status.put(spot.getId().toString(), RedisConstant.Parking.SPOT_STATUS_FREE);
                        });
                        orderList.forEach(order -> {
                            status.put(order.getSpotId().toString(), RedisConstant.Parking.SPOT_STATUS_OCCUPIED);
                        });
                        int count = spotList.size() - orderList.size();
                        //为了在重建完成前，用户看不见车位状态表，将车位状态表存入临时表中，随后将其重命名成约定的状态表并设置可用车位数
                        //为什么不直接使用lua脚本将车位状态表和可用车位数一同写入redis？
                        //lua脚本是原子执行的，在执行时其他请求不能插入进来
                        //如果状态表有500条数据，其他进程需要等待lua脚本插入完这500条，系统会因为lua脚本被卡住
                        //先写入临时表在重命名的办法使得lua只需要执行俩条指令，系统不会因为lua脚本被卡住
                        stringRedisTemplate.opsForHash()
                                .putAll(RedisConstant.Parking.PARKING_SPOT_STATUS
                                        + id + RedisConstant.Parking.SPOT_STATUS_TEMP_SUFFIX, status);
                        Long result = stringRedisTemplate.execute(REBUILD_SCRIPT,
                                Arrays.asList(RedisConstant.Parking.PARKING_SPOT_STATUS + id
                                                + RedisConstant.Parking.SPOT_STATUS_TEMP_SUFFIX,
                                        RedisConstant.Parking.PARKING_SPOT_STATUS + id,
                                        RedisConstant.Parking.PARKING_LOT_AVAILABLE
                                ), id, count
                        );
                        return result == 1;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    Thread.sleep(200);
                    if (stringRedisTemplate.opsForHash().hasKey(RedisConstant.Parking.PARKING_LOT_AVAILABLE, id.toString())) {
                        break;
                    }
                    if (--tryTime == 0) {
                        break;
                    }
                    log.warn("全量重建获取锁失败,将重新尝试获取锁");
                }
            }
        } catch (Exception e) {
            log.error("全量重建发生异常: lotId={}", id, e);
        }
        return false;
    }
}
