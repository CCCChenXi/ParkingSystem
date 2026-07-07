package com.xigeandwillian.parkingsystem.client.mq;

import com.xigeandwillian.parkingsystem.common.config.RabbitMQConfig;
import com.xigeandwillian.parkingsystem.common.constant.OrderConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettleEventListener {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSettle(SettleEvent event) {
        try {
            OrderEvent orderEvent = OrderEvent.builder()
                    .orderId(event.getOrderId())
                    .lotId(event.getLotId())
                    .spotId(event.getSpotId())
                    .seq(event.getSeq())
                    .userId(event.getUserId())
                    .plateNumber(event.getPlateNumber())
                    .title(OrderConstant.SETTLE_TITLE)
                    .content(OrderConstant.SETTLE_CONTENT)
                    .msgType(OrderConstant.ORDER_STATUS_SETTLED)
                    .createTime(event.getEndTime())
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConfig.BOOKING_EXCHANGE,
                    RabbitMQConfig.BOOKING_NOTIFY_ROUTING_KEY, orderEvent);
        } catch (Exception e) {
            log.error("发送结算通知失败: orderId={}", event.getOrderId(), e);
        }
    }
}
