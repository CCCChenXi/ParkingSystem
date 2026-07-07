package com.xigeandwillian.parkingsystem.client.mq;

import com.xigeandwillian.parkingsystem.common.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(SeckillOrderEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.SECKILL_EXCHANGE, RabbitMQConfig.SECKILL_ROUTING_KEY, event);
        log.info("秒杀消息已发送: {}", event);
    }
}
