package com.xigeandwillian.parkingsystem.client.mq;

import com.xigeandwillian.parkingsystem.client.service.impl.CouponDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidateConsumer {

    private final CouponDataProvider couponDataProvider;

    @RabbitListener(queues = "#{cacheInvalidateQueue.name}")
    public void handle(CacheInvalidateEvent event) {
        log.info("收到缓存失效消息: cacheKey={}", event.getCacheKey());
        couponDataProvider.invalidateLocalDetail();
    }
}
