package com.xigeandwillian.parkingsystem.admin.mq;

import com.xigeandwillian.parkingsystem.common.constant.MQConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingLotCacheInitConsumer {



    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = MQConstant.PARKING_LOT_CACHE_INIT_PROC_QUEUE)
    public void handle(ParkingLotCacheInitEvent event) {
        Long lotId = event.getLotId();
        int retryCount = event.getRetryCount();
        log.info("重试初始化停车场GEO缓存: lotId={}, retryCount={}", lotId, retryCount);

        try {
            stringRedisTemplate.opsForGeo().add(RedisConstant.Parking.PARKING_GEO,
                    new Point(event.getLongitude(), event.getLatitude()), lotId.toString());
            stringRedisTemplate.delete(RedisConstant.Parking.DASHBOARD_LOT_COUNT);
            log.info("停车场GEO缓存初始化成功: lotId={}", lotId);
        } catch (Exception e) {
            int nextRetry = retryCount + 1;
            if (nextRetry < MQConstant.DEFAULT_RETRY_MAX) {
                log.warn("停车场GEO缓存初始化失败，第{}次重试: lotId={}", nextRetry, lotId, e);
                rabbitTemplate.convertAndSend(MQConstant.PARKING_LOT_CACHE_INIT_DELAY_QUEUE,
                        new ParkingLotCacheInitEvent(lotId, event.getLongitude(), event.getLatitude(), nextRetry));
            } else {
                log.error("停车场GEO缓存初始化重试用尽，需人工介入: lotId={}", lotId, e);
                rabbitTemplate.convertAndSend(MQConstant.PARKING_LOT_CACHE_INIT_EXCHANGE, MQConstant.ALERT_ROUTING_KEY,
                        new ParkingLotCacheInitEvent(lotId, event.getLongitude(), event.getLatitude(), nextRetry));
            }
        }
    }
}
