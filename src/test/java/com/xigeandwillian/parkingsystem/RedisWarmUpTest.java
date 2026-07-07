package com.xigeandwillian.parkingsystem;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.entity.Coupon;
import com.xigeandwillian.parkingsystem.common.entity.UserCoupon;
import com.xigeandwillian.parkingsystem.common.mapper.CouponMapper;
import com.xigeandwillian.parkingsystem.common.mapper.UserCouponMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
class RedisWarmUpTest {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Test
    void warmAllSeckillStock() {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<Coupon>().eq(Coupon::getType, 1);
        java.util.List<Coupon> coupons = couponMapper.selectList(wrapper);

        for (Coupon c : coupons) {
            stringRedisTemplate.opsForValue()
                    .set(RedisConstant.Coupon.SECKILL_STOCK + c.getId(), String.valueOf(c.getRemainStock()));
            stringRedisTemplate.delete(RedisConstant.Coupon.BOUGHT_KEY + c.getId());
            System.out.printf("预热: id=%s, stock=%s%n", c.getId(), c.getRemainStock());
        }

        System.out.printf("共预热 %s 张秒杀券%n", coupons.size());

        amqpAdmin.purgeQueue("seckill.order.queue");
        System.out.println("秒杀队列已清空");

        warmClaimStatus();
    }

    private void warmClaimStatus() {
        List<UserCoupon> all = userCouponMapper.selectList(null);
        Map<Long, List<Long>> couponUserMap = all.stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId,
                        Collectors.mapping(UserCoupon::getUserId, Collectors.toList())));

        couponUserMap.forEach((couponId, userIds) -> {
            String key = RedisConstant.Coupon.BOUGHT_KEY + couponId;
            userIds.forEach(uid -> stringRedisTemplate.opsForSet().add(key, String.valueOf(uid)));
            System.out.printf("预热购买状态: couponId=%s, 已领取人数=%s%n", couponId, userIds.size());
        });

        System.out.printf("共预热 %s 张优惠券的购买状态%n", couponUserMap.size());
    }
}
