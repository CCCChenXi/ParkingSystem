package com.xigeandwillian.parkingsystem.client.mq;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.client.websocket.NotificationPublisher;
import com.xigeandwillian.parkingsystem.common.constant.CouponConstant;
import com.xigeandwillian.parkingsystem.common.constant.MQConstant;
import com.xigeandwillian.parkingsystem.common.entity.Message;
import com.xigeandwillian.parkingsystem.common.entity.UserCoupon;
import com.xigeandwillian.parkingsystem.common.mapper.MessageMapper;
import com.xigeandwillian.parkingsystem.common.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillMessageConsumer {

    private final UserCouponMapper userCouponMapper;
    private final MessageMapper messageMapper;
    private final NotificationPublisher notificationPublisher;

    @RabbitListener(queues = MQConstant.SECKILL_QUEUE)
    public void handle(SeckillOrderEvent event) {
        Long count = userCouponMapper.selectCount(
                Wrappers.<UserCoupon>lambdaQuery()
                        .eq(UserCoupon::getUserId, event.getUserId())
                        .eq(UserCoupon::getCouponId, event.getCouponId()));
        if (count > 0) {
            log.info("秒杀订单已存在，跳过: {}", event);
            return;
        }
        UserCoupon uc = new UserCoupon();
        uc.setUserId(event.getUserId());
        uc.setCouponId(event.getCouponId());
        uc.setStatus(CouponConstant.USER_COUPON_STATUS_UNUSED);
        userCouponMapper.insert(uc);

        Message message = new Message();
        message.setUserId(event.getUserId());
        message.setTitle("秒杀成功");
        message.setContent("秒杀优惠券已发放到账户");
        message.setType(CouponConstant.USER_COUPON_STATUS_UNUSED);
        message.setIsRead(0);
        messageMapper.insert(message);

        notificationPublisher.publish(event.getUserId(), "秒杀成功", "秒杀优惠券已发放到账户", CouponConstant.USER_COUPON_STATUS_UNUSED);
        log.info("秒杀订单入库成功: {}", event);
    }
}
