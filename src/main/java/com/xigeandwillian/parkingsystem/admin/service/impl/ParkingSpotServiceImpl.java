package com.xigeandwillian.parkingsystem.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigeandwillian.parkingsystem.admin.dto.parkingspot.SpotInsertDTO;
import com.xigeandwillian.parkingsystem.admin.dto.parkingspot.SpotUpdateDTO;
import com.xigeandwillian.parkingsystem.admin.mq.ParkingSpotCacheRetryEvent;
import com.xigeandwillian.parkingsystem.admin.service.ParkingSpotService;
import com.xigeandwillian.parkingsystem.common.vo.parkingspot.SpotListVO;
import com.xigeandwillian.parkingsystem.common.mq.CacheInvalidateEvent;
import com.xigeandwillian.parkingsystem.common.constant.CaffeineConstant;
import com.xigeandwillian.parkingsystem.common.result.CacheResult;
import com.xigeandwillian.parkingsystem.common.constant.MQConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.entity.ParkingSpot;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingLotMapper;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingSpotConverter;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingSpotMapper;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.service.impl.ParkingDataProvider;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingSpotServiceImpl extends ServiceImpl<ParkingSpotMapper, ParkingSpot> implements ParkingSpotService {
    private final ParkingSpotConverter parkingSpotConverter;
    private final ParkingLotMapper parkingLotMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ParkingDataProvider parkingDataProvider;
    private final RabbitTemplate rabbitTemplate;

    @Resource(name = "parkingSpotsCache")
    private Cache<String, List<SpotListVO>> parkingSpotsCache;

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

    @Override
    public Result listSpotsByLotId(Long lotId) {
        log.info("查询车位列表: lotId={}", lotId);
        List<SpotListVO> spots = parkingDataProvider.getAllSpotByLotId(lotId);
        if (spots.isEmpty()) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "该停车场暂无车位");
        }
        CacheResult<List<Integer>> statusResult = parkingDataProvider.getSpotStatusList(lotId);
        if (statusResult.isHit()) {
            List<Integer> statusList = statusResult.getData();
            for (int i = 0; i < spots.size(); i++) {
                spots.get(i).setStatus(statusList.get(i));
            }
        }
        return Result.ok(spots);
    }


    /**
     * 更新车位信息。
     * 拷贝更新参数到实体后更新数据库，事务提交后在 afterCommit 中
     * 清除 Redis 车位列表缓存，成功后广播清所有实例 JVM 缓存；
     * 如果 Redis 异常，发送到重试队列（保证最终成功）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateParkingSpot(Long lotId, Long id, SpotUpdateDTO spotUpdateDTO) {
        log.info("更新车位: lotId={}, id={}", lotId, id);
        try {
            ParkingSpot parkingSpot = parkingSpotConverter.toEntity(spotUpdateDTO);
            parkingSpot.setId(id);
            parkingSpot.setLotId(lotId);
            baseMapper.update(parkingSpot,
                    new QueryWrapper<ParkingSpot>().eq("lot_id", lotId).eq("id", id));

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            clearSpotCacheAndBroadcast(lotId,
                                    () -> stringRedisTemplate.delete(RedisConstant.Parking.PARKING_SPOT_LIST + lotId),
                                    MQConstant.PARKING_SPOT_CACHE_UPDATE_SOURCE_QUEUE);
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

            long seq = lot.getTotalSpots();

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
                            clearSpotCacheAndBroadcast(lotId, () -> {
                                stringRedisTemplate.delete(RedisConstant.Parking.PARKING_SPOT_LIST + lotId);
                                stringRedisTemplate.delete(RedisConstant.Parking.PARKING_SPOT_STATUS + lotId);
                                stringRedisTemplate.delete(RedisConstant.Parking.PARKING_LOT_INFO + lotId);
                                stringRedisTemplate.delete(RedisConstant.Parking.DASHBOARD_SPOT_COUNT);
                                stringRedisTemplate.delete(RedisConstant.Parking.PARKING_LOT_LIST_ALL);
                            }, MQConstant.PARKING_SPOT_CACHE_CREATE_SOURCE_QUEUE);
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

    private void clearSpotCacheAndBroadcast(Long lotId, Runnable redisOps, String sourceQueueName) {
        parkingSpotsCache.invalidate(CaffeineConstant.PARKING_SPOTS_KEY_PREFIX + lotId);
        try {
            redisOps.run();
            log.info("Redis车位缓存清理成功: lotId={}", lotId);
            rabbitTemplate.convertAndSend(MQConstant.CACHE_INVALIDATE_EXCHANGE, null,
                    new CacheInvalidateEvent(RedisConstant.Parking.PARKING_SPOT_LIST + lotId));
            rabbitTemplate.convertAndSend(MQConstant.CACHE_INVALIDATE_EXCHANGE, null,
                    new CacheInvalidateEvent(CaffeineConstant.PARKING_SPOTS_KEY_PREFIX + lotId));
        } catch (Exception e) {
            log.warn("Redis车位缓存清理失败，发送到重试队列: lotId={}", lotId, e);
            try {
                rabbitTemplate.convertAndSend(sourceQueueName,
                        new ParkingSpotCacheRetryEvent(lotId, 0, sourceQueueName));
            } catch (Exception ex) {
                log.error("发送重试队列消息失败: lotId={}", lotId, ex);
            }
        }
    }
}