package com.xigeandwillian.parkingsystem.client.mq;

import com.xigeandwillian.parkingsystem.common.mq.CacheInvalidateEvent;
import com.xigeandwillian.parkingsystem.common.service.impl.CouponDataProvider;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.service.impl.ParkingDataProvider;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant.Coupon;
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
        if (event.getCacheKey() != null) {
            if (event.getCacheKey().equals(Coupon.AVAILABLE_KEY)) {
                couponDataProvider.invalidateLocalAvailable();
            } else if (event.getCacheKey().startsWith(RedisConstant.Parking.PARKING_SPOT_LIST)) {
                parkingDataProvider.invalidateLocalSpotList(event.getCacheKey());
            } else {
                couponDataProvider.invalidateLocalDetail();
            }
        }
    }
}
