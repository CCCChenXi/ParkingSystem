package com.xigeandwillian.parkingsystem.client.mq;

import com.xigeandwillian.parkingsystem.client.websocket.NotificationPublisher;
import com.xigeandwillian.parkingsystem.common.constant.MQConstant;
import com.xigeandwillian.parkingsystem.common.constant.OrderConstant;
import com.xigeandwillian.parkingsystem.common.entity.Message;
import com.xigeandwillian.parkingsystem.common.entity.ParkingOrder;
import com.xigeandwillian.parkingsystem.common.mapper.MessageMapper;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static com.xigeandwillian.parkingsystem.common.constant.OrderConstant.ORDER_STATUS_CANCELLED;
import static com.xigeandwillian.parkingsystem.common.constant.OrderConstant.ORDER_STATUS_RESERVED;
import static com.xigeandwillian.parkingsystem.common.constant.RedisConstant.Parking.PARKING_SPOT_STATUS;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageConsumer {

    private final MessageMapper messageMapper;
    private final ParkingOrderMapper parkingOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final NotificationPublisher notificationPublisher;

    @RabbitListener(queues = MQConstant.BOOKING_NOTIFY_QUEUE)
    public void handleNotify(OrderEvent event) {
        log.info("发送消息成功!");
        Message message = createMessage(event, event.getTitle(), event.getContent(), event.getMsgType());
        messageMapper.insert(message);
        notificationPublisher.publish(event.getUserId(), event.getTitle(), event.getContent(), event.getMsgType());
    }

    @RabbitListener(queues = MQConstant.BOOKING_EXPIRE_QUEUE)
    public void handleExpire(OrderEvent event) {
        //1.查询订单
        ParkingOrder order = parkingOrderMapper.selectById(event.getOrderId());
        //订单不存在或者状态已变更
        if (order == null || order.getStatus() != ORDER_STATUS_RESERVED) {
            log.info("订单 {} 状态已变更，忽略过期处理", event.getOrderId());
            return;
        }
        //2.取消订单
        order.setStatus(ORDER_STATUS_CANCELLED);
        //3.更新数据库
        parkingOrderMapper.updateById(order);
        //4.bitmap释放车位
        stringRedisTemplate.opsForValue().setBit(PARKING_SPOT_STATUS + event.getLotId(), event.getSeq(), false);
        log.info("预约已过期: orderId={}", event.getOrderId());
        Message message = createMessage(event, "预约已取消", "由于您未在指定时间内入场，您预约的订单已取消", OrderConstant.ORDER_STATUS_CANCELLED);
        messageMapper.insert(message);
        notificationPublisher.publish(event.getUserId(), "预约已取消", "由于您未在指定时间内入场，您预约的订单已取消", OrderConstant.ORDER_STATUS_CANCELLED);
    }


    private Message createMessage(OrderEvent event, String title, String content, Integer type) {
        Message message = new Message();
        message.setUserId(event.getUserId());
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setIsRead(0);
        return message;
    }
}
