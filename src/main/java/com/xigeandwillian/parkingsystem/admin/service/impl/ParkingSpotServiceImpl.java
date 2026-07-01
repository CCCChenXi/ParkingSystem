package com.xigeandwillian.parkingsystem.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xigeandwillian.parkingsystem.admin.dto.parkingspot.SpotInsertDTO;
import com.xigeandwillian.parkingsystem.admin.dto.parkingspot.SpotUpdateDTO;
import com.xigeandwillian.parkingsystem.admin.mapper.ParkingLotMapper;
import com.xigeandwillian.parkingsystem.admin.mapper.ParkingSpotMapper;
import com.xigeandwillian.parkingsystem.admin.service.Service.ParkingSpotService;
import com.xigeandwillian.parkingsystem.admin.vo.parkingspot.SpotListVO;
import com.xigeandwillian.parkingsystem.client.mapper.ParkingOrderMapper;
import com.xigeandwillian.parkingsystem.common.cache.ParkingCache;
import com.xigeandwillian.parkingsystem.common.constant.CacheConstant;
import com.xigeandwillian.parkingsystem.common.constant.OrderConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.entity.ParkingOrder;
import com.xigeandwillian.parkingsystem.common.entity.ParkingSpot;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.result.Result;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingSpotServiceImpl implements ParkingSpotService {
    private final ParkingSpotMapper parkingSpotMapper;
    private final ParkingLotMapper parkingLotMapper;
    private final ParkingOrderMapper parkingOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ParkingCache parkingCache;
    private final RedissonClient redissonClient;
    private final CacheManager cacheManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteParkingSpot(Long lotId, Long id) {
        try {
            parkingSpotMapper.delete(
                    new QueryWrapper<ParkingSpot>().eq("lot_id", lotId).eq("id", id));

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            stringRedisTemplate.opsForHash().delete(
                                    RedisConstant.Parking.PARKING_SPOT_STATUS + lotId,
                                    id.toString());
                            Cache cache = cacheManager.getCache(CacheConstant.PARKING_SPOT_LIST);
                            if (cache != null) {
                                cache.evict(lotId);
                            }
                        }
                    });
            return Result.ok();
        } catch (Exception e) {
            log.error("删除车位失败: lotId={}, id={}", lotId, id, e);
            throw new BusinessException(ResultConstant.SYSTEM_ERROR, "删除车位失败");
        }
    }

    @Override
    public Result listSpotsByLotId(Long lotId) {
        //执行流程，先获取车位状态表，如果车位状态表为null，则去查询缓存，如果缓存也不存在则去查询数据库是否存在停车场
        //如果存在停车场，开始重建车位状态表
        //如果不存在停车场，在缓存中记录停车场信息为""
        //如果try失败了，说明redis异常了，我们直接降级去查询数据库
        log.info("查询车位列表: lotId={}", lotId);
        Map<Object, Object> spotStatus;
        try {
            spotStatus = stringRedisTemplate.opsForHash()
                    .entries(RedisConstant.Parking.PARKING_SPOT_STATUS + lotId);
        } catch (Exception e) {
            log.warn("从缓存中获取车位状态失败，将查询数据库: lotId={}", lotId, e);
            spotStatus = buildSpotStatusFromDb(lotId);
        }
        if (spotStatus.isEmpty()) {
            spotStatus = rebuildSpotCacheIfNeeded(lotId);
        }
        List<SpotListVO> voList = parkingCache.getSpotsByLotId(lotId);
        Map<Object, Object> finalSpotStatus = spotStatus;
        voList.forEach(item -> {
            Object val = finalSpotStatus.get(item.getId().toString());
            item.setStatus(val != null ? Integer.parseInt(val.toString())
                    : Integer.parseInt(RedisConstant.Parking.SPOT_STATUS_OCCUPIED));
        });
        return Result.ok(voList);
    }

    private Map<Object, Object> buildSpotStatusFromDb(Long lotId) {
        LambdaQueryWrapper<ParkingOrder> queryOrder = new LambdaQueryWrapper<>();
        queryOrder.select(ParkingOrder::getSpotId)
                .eq(ParkingOrder::getLotId, lotId)
                .between(ParkingOrder::getStatus,
                        OrderConstant.ORDER_STATUS_RESERVED,
                        OrderConstant.ORDER_STATUS_IN_PROGRESS);
        List<ParkingOrder> orderList = parkingOrderMapper.selectList(queryOrder);
        Set<Long> occupiedSpotIds = orderList.stream()
                .map(ParkingOrder::getSpotId)
                .collect(Collectors.toSet());
        List<SpotListVO> allSpots = parkingCache.getSpotsByLotId(lotId);
        Map<Object, Object> status = new HashMap<>();
        allSpots.forEach(spot ->
                status.put(spot.getId().toString(),
                        occupiedSpotIds.contains(spot.getId())
                                ? RedisConstant.Parking.SPOT_STATUS_OCCUPIED
                                : RedisConstant.Parking.SPOT_STATUS_FREE));
        return status;
    }

    private Map<Object, Object> rebuildSpotCacheIfNeeded(Long lotId) {
        String lotInfo = stringRedisTemplate.opsForValue()
                .get(RedisConstant.Parking.PARKING_LOT_INFO + lotId);

        if (lotInfo == null) {
            ParkingLot lot = parkingLotMapper.selectById(lotId);
            if (lot == null) {
                stringRedisTemplate.opsForValue()
                        .set(RedisConstant.Parking.PARKING_LOT_INFO + lotId, "");
                return new HashMap<>();
            }
            rebuildSpotStatus(lotId);
            stringRedisTemplate.opsForValue()
                    .set(RedisConstant.Parking.PARKING_LOT_INFO + lotId,
                            JSONUtil.toJsonStr(lot));
            return stringRedisTemplate.opsForHash()
                    .entries(RedisConstant.Parking.PARKING_SPOT_STATUS + lotId);
        }

        if (lotInfo.isEmpty()) {
            return new HashMap<>();
        }

        rebuildSpotStatus(lotId);
        return stringRedisTemplate.opsForHash()
                .entries(RedisConstant.Parking.PARKING_SPOT_STATUS + lotId);
    }

    private void rebuildSpotStatus(Long lotId) {
        RLock lock = redissonClient.getLock(
                RedisConstant.Parking.PARKING_LOT_REBUILD_LOCK + lotId);
        int tryTime = 3;
        for (int i = 0; i < tryTime; i++) {
            try {
                if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
                    try {
                        LambdaQueryWrapper<ParkingOrder> queryOrder = new LambdaQueryWrapper<>();
                        queryOrder.select(ParkingOrder::getSpotId)
                                .eq(ParkingOrder::getLotId, lotId)
                                .between(ParkingOrder::getStatus,
                                        OrderConstant.ORDER_STATUS_RESERVED,
                                        OrderConstant.ORDER_STATUS_IN_PROGRESS);
                        List<ParkingOrder> orderList = parkingOrderMapper.selectList(queryOrder);
                        Set<Long> occupiedIds = orderList.stream()
                                .map(ParkingOrder::getSpotId)
                                .collect(Collectors.toSet());

                        List<ParkingSpot> spotList = parkingSpotMapper.selectList(
                                new QueryWrapper<ParkingSpot>().eq("lot_id", lotId));
                        Map<String, String> status = new HashMap<>();
                        spotList.forEach(spot ->
                                status.put(spot.getId().toString(),
                                        occupiedIds.contains(spot.getId())
                                                ? RedisConstant.Parking.SPOT_STATUS_OCCUPIED
                                                : RedisConstant.Parking.SPOT_STATUS_FREE));

                        String tempKey = RedisConstant.Parking.PARKING_SPOT_STATUS + lotId
                                + RedisConstant.Parking.SPOT_STATUS_TEMP_SUFFIX;
                        String realKey = RedisConstant.Parking.PARKING_SPOT_STATUS + lotId;
                        if (status.isEmpty()) {
                            stringRedisTemplate.delete(realKey);
                            log.info("车位状态为空，直接清除: lotId={}", lotId);
                        } else {
                            stringRedisTemplate.opsForHash().putAll(tempKey, status);
                            stringRedisTemplate.rename(tempKey, realKey);
                            log.info("车位状态表重建成功: lotId={}", lotId);
                        }
                    } finally {
                        lock.unlock();
                    }
                    return;
                }
                Thread.sleep(200);
                if (stringRedisTemplate.opsForHash().size(
                        RedisConstant.Parking.PARKING_SPOT_STATUS + lotId) > 0) {
                    log.info("车位状态表已被其他线程重建: lotId={}", lotId);
                    return;
                }
            } catch (Exception e) {
                log.error("车位状态表重建异常: lotId={}", lotId, e);
                if (i == tryTime - 1) return;
            }
        }
    }

    @Override
    public Result init() {
        log.info("初始化全部停车场车位信息");
        try {
            List<ParkingLot> lots = parkingLotMapper.selectList(null);
            lots.forEach(lot -> {
                List<ParkingSpot> spots = parkingSpotMapper.selectList(
                        new QueryWrapper<ParkingSpot>().eq("lot_id", lot.getId()));
                Map<Object, Object> status = new HashMap<>();
                spots.forEach(spot ->
                        status.put(spot.getId().toString(),
                                RedisConstant.Parking.SPOT_STATUS_FREE));
                stringRedisTemplate.opsForHash().putAll(
                        RedisConstant.Parking.PARKING_SPOT_STATUS + lot.getId(), status);
            });
            return Result.ok();
        } catch (Exception e) {
            log.error("初始化全部停车场车位信息失败", e);
            throw new BusinessException(ResultConstant.SYSTEM_ERROR, "初始化停车场车位信息失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateParkingSpot(Long lotId, Long id, SpotUpdateDTO spotUpdateDTO) {
        log.info("更新车位: lotId={}, id={}", lotId, id);
        try {
            ParkingSpot parkingSpot = new ParkingSpot();
            BeanUtils.copyProperties(spotUpdateDTO, parkingSpot);
            parkingSpot.setId(id);
            parkingSpot.setLotId(lotId);
            parkingSpotMapper.update(parkingSpot,
                    new QueryWrapper<ParkingSpot>().eq("lot_id", lotId).eq("id", id));

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            Cache cache = cacheManager.getCache(CacheConstant.PARKING_SPOT_LIST);
                            if (cache != null) {
                                cache.evict(lotId);
                            }
                        }
                    });
            return Result.ok();
        } catch (Exception e) {
            log.error("更新车位失败: lotId={}, id={}", lotId, id, e);
            throw new BusinessException(ResultConstant.SYSTEM_ERROR, "更新车位失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result batchCreateSpots(SpotInsertDTO spotInsertDTO) {
        log.info("批量新增车位: lotId={}, count={}",
                spotInsertDTO.getLotId(), spotInsertDTO.getSpotNumbers().size());
        try {
            List<ParkingSpot> spots = spotInsertDTO.getSpotNumbers().stream().map(number -> {
                ParkingSpot spot = new ParkingSpot();
                spot.setLotId(spotInsertDTO.getLotId());
                spot.setSpotNumber(number);
                spot.setType(spotInsertDTO.getType());
                return spot;
            }).collect(Collectors.toList());

            spots.forEach(item -> {
                try {
                    parkingSpotMapper.insert(item);
                } catch (Exception e) {
                    log.warn("车位插入失败: lotId={}, spotNumber={}",
                            spotInsertDTO.getLotId(), item.getSpotNumber(), e);
                }
            });

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            stringRedisTemplate.delete(
                                    RedisConstant.Parking.PARKING_SPOT_STATUS + spotInsertDTO.getLotId());
                            stringRedisTemplate.opsForHash().delete(
                                    RedisConstant.Parking.PARKING_LOT_AVAILABLE,
                                    spotInsertDTO.getLotId().toString());
                            Cache cache = cacheManager.getCache(CacheConstant.PARKING_SPOT_LIST);
                            if (cache != null) cache.evict(spotInsertDTO.getLotId());
                        }
                    });
            return Result.ok();
        } catch (Exception e) {
            log.error("批量新增车位失败: lotId={}", spotInsertDTO.getLotId(), e);
            throw new BusinessException(ResultConstant.SYSTEM_ERROR, "批量新增车位失败");
        }
    }
}
