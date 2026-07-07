package com.xigeandwillian.parkingsystem.client.mq;

import com.xigeandwillian.parkingsystem.common.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidateProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendCacheInvalidate(String cacheKey) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.CACHE_INVALIDATE_EXCHANGE, null, new CacheInvalidateEvent(cacheKey));
        log.info("缓存失效消息已发送: cacheKey={}", cacheKey);
    }
}
