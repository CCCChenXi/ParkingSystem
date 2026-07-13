package com.xigeandwillian.parkingsystem.client.mq;

import com.xigeandwillian.parkingsystem.common.constant.CaffeineConstant;
import com.xigeandwillian.parkingsystem.common.constant.MQConstant;
import com.xigeandwillian.parkingsystem.common.mq.CacheInvalidateProducer;
import com.xigeandwillian.parkingsystem.common.service.impl.ParkingDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static com.xigeandwillian.parkingsystem.common.constant.RedisConstant.Parking.PARKING_SPOT_STATUS;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpotReleaseRetryConsumer {



    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ParkingDataProvider parkingDataProvider;
    private final CacheInvalidateProducer cacheInvalidateProducer;

    @RabbitListener(queues = MQConstant.SPOT_RELEASE_RETRY_PROC_QUEUE)
    public void handle(SpotReleaseRetryEvent event) {
        log.info("重试释放车位: lotId={}, seq={}, retryCount={}", event.getLotId(), event.getSeq(), event.getRetryCount());
        try {
            stringRedisTemplate.opsForValue().setBit(PARKING_SPOT_STATUS + event.getLotId(), event.getSeq(), false);
            parkingDataProvider.invalidateParkingSpotsCache(CaffeineConstant.PARKING_SPOTS_KEY_PREFIX + event.getLotId());
            cacheInvalidateProducer.sendCacheInvalidate(CaffeineConstant.PARKING_SPOTS_KEY_PREFIX + event.getLotId());
            log.info("重试释放车位成功: lotId={}, seq={}", event.getLotId(), event.getSeq());
        } catch (Exception e) {
            int next = event.getRetryCount() + 1;
            if (next < MQConstant.DEFAULT_RETRY_MAX) {
                log.warn("释放车位失败第{}次重试: lotId={}, seq={}", next, event.getLotId(), event.getSeq(), e);
                rabbitTemplate.convertAndSend(MQConstant.SPOT_RELEASE_RETRY_DELAY_QUEUE,
                        new SpotReleaseRetryEvent(event.getLotId(), event.getSeq(), next));
            } else {
                log.error("释放车位重试用尽，需人工介入: lotId={}, seq={}", event.getLotId(), event.getSeq(), e);
                rabbitTemplate.convertAndSend(MQConstant.SPOT_RELEASE_RETRY_EXCHANGE,
                        MQConstant.ALERT_ROUTING_KEY,
                        new SpotReleaseRetryEvent(event.getLotId(), event.getSeq(), next));
            }
        }
    }
}
