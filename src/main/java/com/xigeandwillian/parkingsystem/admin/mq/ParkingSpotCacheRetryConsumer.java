package com.xigeandwillian.parkingsystem.admin.mq;

import com.xigeandwillian.parkingsystem.common.mq.CacheInvalidateEvent;
import com.xigeandwillian.parkingsystem.common.constant.MQConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingSpotCacheRetryConsumer {



    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = MQConstant.PARKING_SPOT_CACHE_RETRY_PROC_QUEUE)
    public void handle(ParkingSpotCacheRetryEvent event) {
        Long lotId = event.getLotId();
        int retryCount = event.getRetryCount();
        log.info("重试清理Redis缓存: lotId={}, retryCount={}", lotId, retryCount);

        try {
            stringRedisTemplate.delete(RedisConstant.Parking.PARKING_SPOT_LIST + lotId);
            stringRedisTemplate.delete(RedisConstant.Parking.PARKING_SPOT_STATUS + lotId);
            stringRedisTemplate.delete(RedisConstant.Parking.PARKING_LOT_INFO + lotId);
            log.info("Redis缓存清理成功: lotId={}", lotId);
            rabbitTemplate.convertAndSend(MQConstant.CACHE_INVALIDATE_EXCHANGE, null,
                    new CacheInvalidateEvent(RedisConstant.Parking.PARKING_SPOT_LIST + lotId));
        } catch (Exception e) {
            int nextRetry = retryCount + 1;
            if (nextRetry < MQConstant.DEFAULT_RETRY_MAX) {
                log.warn("Redis缓存清理失败，第{}次重试: lotId={}", nextRetry, lotId, e);
                rabbitTemplate.convertAndSend(event.getSourceQueue(),
                        new ParkingSpotCacheRetryEvent(lotId, nextRetry, event.getSourceQueue()));
            } else {
                log.error("Redis缓存清理重试用尽，需人工介入: lotId={}", lotId, e);
                rabbitTemplate.convertAndSend(MQConstant.PARKING_SPOT_CACHE_RETRY_EXCHANGE, MQConstant.ALERT_ROUTING_KEY,
                        new ParkingSpotCacheRetryEvent(lotId, nextRetry, event.getSourceQueue()));
            }
        }
    }
}
