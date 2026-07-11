package com.xigeandwillian.parkingsystem.client.mq;

import com.xigeandwillian.parkingsystem.client.service.impl.CouponDataProvider;
import com.xigeandwillian.parkingsystem.common.service.impl.ParkingDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidateConsumer {

    private final CouponDataProvider couponDataProvider;
    private final ParkingDataProvider parkingDataProvider;

    @RabbitListener(queues = "#{cacheInvalidateQueue.name}")
    public void handle(CacheInvalidateEvent event) {
        log.info("收到缓存失效消息: cacheKey={}", event.getCacheKey());
        if (event.getCacheKey() != null && event.getCacheKey().startsWith("parking:spot:list:")) {
            parkingDataProvider.invalidateLocalSpotList(event.getCacheKey());
        } else {
            couponDataProvider.invalidateLocalDetail();
        }
    }
}
