package com.xigeandwillian.parkingsystem.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import org.springframework.dao.DuplicateKeyException;
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
public class ParkingSpotServiceImpl extends ServiceImpl<ParkingSpotMapper, ParkingSpot> implements ParkingSpotService {
    private final ParkingLotMapper parkingLotMapper;
    private final ParkingOrderMapper parkingOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ParkingCache parkingCache;
    private final RedissonClient redissonClient;
    private final CacheManager cacheManager;

    /**
     * 为了保证同一停车场内车位seq连续，不允许删除车位
     * 不允许删除车位
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteParkingSpot(Long lotId, Long id) {
        log.warn("不允许删除车位!!!");
        throw new BusinessException(ResultConstant.BAD_REQUEST, "不允许删除车位!!!");
    }

    /**
     * 查询停车场所有车位列表
     * 流程：
     * 1.获取停车场信息
     * 2.获取车位状态表
     * 3.封装数据并返回
     * 需要判断停车场是否存在，车位数是否大于0，以及缓存过期时重建缓存
     */
    @Override
    public Result listSpotsByLotId(Long lotId) {
        log.info("查询车位列表: lotId={}", lotId);

        // 1. 获取停车场信息
        ParkingLot lot = null;
        try {
            String lotInfoStr = stringRedisTemplate.opsForValue()
                    .get(RedisConstant.Parking.PARKING_LOT_INFO + lotId);
            if (lotInfoStr == null) {
                lot = parkingLotMapper.selectById(lotId);
                if (lot == null) {
                    stringRedisTemplate.opsForValue()
                            .set(RedisConstant.Parking.PARKING_LOT_INFO + lotId, "");
                    return Result.ok(List.of());
                }
            } else if (lotInfoStr.isEmpty()) {
                return Result.ok(List.of());
            } else {
                lot = JSONUtil.toBean(lotInfoStr, ParkingLot.class);
            }
        } catch (Exception e) {
            log.info("缓存中获取停车场信息失败：{} 将降级查询数据库", lotId, e);
            lot = parkingLotMapper.selectById(lotId);
            if (lot == null) {
                return Result.ok(List.of());
            }
        }

        // 2. 判断总车位数
        if (lot.getTotalSpots() == 0) {
            return Result.ok(List.of());
        }

        // 3. 获取车位状态表
        Map<Object, Object> spotStatus;
        try {
            spotStatus = stringRedisTemplate.opsForHash()
                    .entries(RedisConstant.Parking.PARKING_SPOT_STATUS + lotId);
            if (spotStatus.isEmpty()) {
                rebuildSpotStatus(lotId);
                spotStatus = stringRedisTemplate.opsForHash()
                        .entries(RedisConstant.Parking.PARKING_SPOT_STATUS + lotId);
            }
        } catch (Exception e) {
            log.warn("从缓存获取车位状态失败，降级数据库: lotId={}", lotId, e);
            spotStatus = buildSpotStatusFromDb(lotId);
        }

        // 4. 组装返回结果
        List<SpotListVO> voList = parkingCache.getSpotsByLotId(lotId);
        Map<Object, Object> finalSpotStatus = spotStatus;
        voList.forEach(item -> {
            Object val = finalSpotStatus.get(item.getId().toString());
            item.setStatus(val != null ? Integer.parseInt(val.toString())
                    : Integer.parseInt(RedisConstant.Parking.SPOT_STATUS_OCCUPIED));
        });
        return Result.ok(voList);
    }

    /**
     * 从数据库中获取车位状态表
     * 1.获取该停车场的预约中和进行中订单
     * 2.根据订单得到被占用的车位id
     * 3.获取该停车场所有车位id
     * 4.对于被占用的车位，状态设置1，否则设置0
     */
    private Map<Object, Object> buildSpotStatusFromDb(Long lotId) {
        LambdaQueryWrapper<ParkingOrder> queryOrder = new LambdaQueryWrapper<>();
        queryOrder.select(ParkingOrder::getSpotId)
                .eq(ParkingOrder::getLotId, lotId)
                .in(ParkingOrder::getStatus, OrderConstant.ORDER_STATUS_RESERVED, OrderConstant.ORDER_STATUS_IN_PROGRESS);
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

    /**
     * 重建车位状态表
     * 1.从数据库中获取车位状态表，流程同buildSpotStatusFromDb方法
     * 2.将得到的车位状态表存在临时表中，使得重建完成前用户无法获取状态，避免了读取不完整的车位状态表
     * 3.将临时表改名成正式表名
     * 对重建过程加锁，只允许一个人重建，设置最大尝试加锁次数为3，防止因为获取不到锁而陷入死循环
     */
    private void rebuildSpotStatus(Long lotId) {
        RLock lock = redissonClient.getLock(
                RedisConstant.Parking.PARKING_LOT_REBUILD_LOCK + lotId);
        int tryTime = 3;
        //1.尝试获取锁
        for (int i = 0; i < tryTime; i++) {
            try {
                if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
                    try {
                        //2.根据预约和进行中的订单获取被占用的车位id
                        LambdaQueryWrapper<ParkingOrder> queryOrder = new LambdaQueryWrapper<>();
                        queryOrder.select(ParkingOrder::getSpotId)
                                .eq(ParkingOrder::getLotId, lotId)
                                .in(ParkingOrder::getStatus, OrderConstant.ORDER_STATUS_RESERVED, OrderConstant.ORDER_STATUS_IN_PROGRESS);
                        List<ParkingOrder> orderList = parkingOrderMapper.selectList(queryOrder);
                        Set<Long> occupiedIds = orderList.stream()
                                .map(ParkingOrder::getSpotId)
                                .collect(Collectors.toSet());

                        //3.获取该停车场所有的车位id并设置状态
                        List<ParkingSpot> spotList = baseMapper.selectList(
                                new QueryWrapper<ParkingSpot>().eq("lot_id", lotId));
                        Map<String, String> status = new HashMap<>();
                        spotList.forEach(spot ->
                                status.put(spot.getId().toString(),
                                        occupiedIds.contains(spot.getId())
                                                ? RedisConstant.Parking.SPOT_STATUS_OCCUPIED
                                                : RedisConstant.Parking.SPOT_STATUS_FREE));

                        //4.先重建临时车位状态表，防止其他用户在重建结束前访问到了不完整车位状态表，结束后将临时表改名成正式表
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
                if (i == tryTime - 1) {
                    return;
                }
            }
        }
    }


    /**
     * 更新车位信息。
     * 拷贝更新参数到实体后更新数据库，事务提交后在 afterCommit 中
     * 清除 Spring Cache 中的该停车场车位列表缓存，避免读到脏数据。
     * 缓存清除失败不影响事务提交，下一次查询会从数据库重新加载。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateParkingSpot(Long lotId, Long id, SpotUpdateDTO spotUpdateDTO) {
        log.info("更新车位: lotId={}, id={}", lotId, id);
        try {
            ParkingSpot parkingSpot = new ParkingSpot();
            BeanUtils.copyProperties(spotUpdateDTO, parkingSpot);
            parkingSpot.setId(id);
            parkingSpot.setLotId(lotId);
            baseMapper.update(parkingSpot,
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

    /**
     * 批量新增车位。
     * 先校验停车场是否存在，组装车位列表后执行 saveBatch 批量插入，
     * 捕获 DuplicateKeyException 抛业务异常提示编号重复。
     * 插入成功后用 setSql 原子递增停车场的 totalSpots 和 availableSpots。
     * 事务提交后在 afterCommit 中清理 parking:spot:status、
     * parking:lot:info 和 Spring Cache 车位列表三项缓存。
     * 业务异常直接抛出，系统异常包装后抛出。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result batchCreateSpots(SpotInsertDTO spotInsertDTO) {
        Long lotId = spotInsertDTO.getLotId();
        int insertCount = spotInsertDTO.getSpotNumbers().size();

        log.info("批量新增车位开始: lotId={}, count={}", lotId, insertCount);

        if (insertCount == 0) {
            return Result.ok();
        }

        try {
            // 1. 校验停车场是否存在
            ParkingLot lot = parkingLotMapper.selectById(lotId);
            if (lot == null) {
                log.warn("停车场不存在，新增车位失败: {}", lotId);
                throw new BusinessException(ResultConstant.BAD_REQUEST, "停车场不存在，新增车位失败");
            }

            Integer seq=lot.getTotalSpots();

            // 2. 组装车位列表
            List<ParkingSpot> spots = spotInsertDTO.getSpotNumbers().stream().map(number -> {
                ParkingSpot spot = new ParkingSpot();
                spot.setLotId(lotId);
                spot.setSpotNumber(number);
                spot.setType(spotInsertDTO.getType());
                return spot;
            }).collect(Collectors.toList());

            for (ParkingSpot spot : spots) {
                spot.setSeq(seq);
                seq++;
            }

            try {
                saveBatch(spots);
            } catch (DuplicateKeyException e) {
                log.warn("批量新增车位编号重复: lotId={}", lotId);
                throw new BusinessException(ResultConstant.BAD_REQUEST, "车位编号重复");
            }

            parkingLotMapper.update(null, new LambdaUpdateWrapper<ParkingLot>()
                    .eq(ParkingLot::getId, lotId)
                    .setSql("total_spots = total_spots + " + insertCount)
                    .setSql("available_spots = available_spots + " + insertCount));

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                stringRedisTemplate.delete(RedisConstant.Parking.PARKING_SPOT_STATUS + lotId);
                                stringRedisTemplate.delete(RedisConstant.Parking.PARKING_LOT_INFO + lotId);
                                Cache spotListCache = cacheManager.getCache(CacheConstant.PARKING_SPOT_LIST);
                                if (spotListCache != null) {
                                    spotListCache.evict(lotId);
                                }
                                stringRedisTemplate.delete(RedisConstant.Parking.DASHBOARD_SPOT_COUNT);
                                log.info("批量新增车位成功，缓存清理完毕: lotId={}", lotId);
                            } catch (Exception e) {
                                log.error("批量新增车位后清理缓存异常: lotId={}", lotId, e);
                            }
                        }
                    });

            return Result.ok();

        } catch (BusinessException e) {
            //自己抛出的业务异常不需要重新包装，直接抛出
            throw e;
        } catch (Exception e) {
            //抛出的系统异常，包装成业务异常然后抛出
            log.error("批量新增车位出现系统异常: lotId={}", lotId, e);
            throw new BusinessException(ResultConstant.SYSTEM_ERROR, "批量新增车位失败: " + e.getMessage());
        }
    }
}