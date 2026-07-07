package com.xigeandwillian.parkingsystem.client.mq;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.common.config.RabbitMQConfig;
import com.xigeandwillian.parkingsystem.common.constant.CouponConstant;
import com.xigeandwillian.parkingsystem.common.entity.UserCoupon;
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

    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
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
        log.info("秒杀订单入库成功: {}", event);
    }
}
