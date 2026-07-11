package com.xigeandwillian.parkingsystem.client.mq;

import com.xigeandwillian.parkingsystem.common.constant.MQConstant;
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
        rabbitTemplate.convertAndSend(MQConstant.SECKILL_EXCHANGE, MQConstant.SECKILL_ROUTING_KEY, event);
        log.info("秒杀消息已发送: {}", event);
    }
}
