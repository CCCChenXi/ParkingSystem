package com.xigeandwillian.parkingsystem.client.mq;

import com.xigeandwillian.parkingsystem.common.constant.MQConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Async("mqExecutor")
    public void sendBookingNotify(OrderEvent event) {
        try {
            rabbitTemplate.convertAndSend(MQConstant.BOOKING_EXCHANGE,
                    MQConstant.BOOKING_NOTIFY_ROUTING_KEY, event);
            log.info("异步发送预约通知消息: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("异步发送预约通知消息失败: orderId={}", event.getOrderId(), e);
        }
    }

    @Async("mqExecutor")
    public void sendBookingDelay(OrderEvent event) {
        try {
            rabbitTemplate.convertAndSend(MQConstant.BOOKING_EXCHANGE,
                    MQConstant.BOOKING_DELAY_ROUTING_KEY, event);
            log.info("异步发送预约延迟消息: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("异步发送预约延迟消息失败: orderId={}", event.getOrderId(), e);
        }
    }

    @Async("mqExecutor")
    public void sendSpotReleaseRetry(Long lotId, Long seq) {
        try {
            rabbitTemplate.convertAndSend(
                    MQConstant.SPOT_RELEASE_RETRY_DELAY_QUEUE,
                    new SpotReleaseRetryEvent(lotId, seq, 0));
            log.info("异步发送释放车位重试消息: lotId={}, seq={}", lotId, seq);
        } catch (Exception e) {
            log.error("异步发送释放车位重试消息失败: lotId={}, seq={}", lotId, seq, e);
        }
    }
}
