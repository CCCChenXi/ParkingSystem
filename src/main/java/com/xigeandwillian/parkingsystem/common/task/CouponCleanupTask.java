package com.xigeandwillian.parkingsystem.common.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.entity.Coupon;
import com.xigeandwillian.parkingsystem.common.mapper.CouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class CouponCleanupTask {

    private final CouponMapper couponMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredCouponKeys() {
        log.info("开始清理过期优惠券Redis缓存...");
        try {
            List<Coupon> expiredCoupons = couponMapper.selectList(
                    Wrappers.<Coupon>lambdaQuery()
                            .select(Coupon::getId)
                            .lt(Coupon::getEndTime, LocalDateTime.now()));
            if (expiredCoupons.isEmpty()) {
                log.info("无过期优惠券需要清理");
                return;
            }
            int count = 0;
            for (Coupon coupon : expiredCoupons) {
                Long id = coupon.getId();
                stringRedisTemplate.delete(RedisConstant.Coupon.SECKILL_STOCK + id);
                stringRedisTemplate.delete(RedisConstant.Coupon.BOUGHT_KEY + id);
                stringRedisTemplate.delete(RedisConstant.Coupon.STATIC_KEY + id);
                count++;
            }
            log.info("过期优惠券Redis缓存清理完成, 共清理 {} 个", count);
        } catch (Exception e) {
            log.error("过期优惠券Redis缓存清理失败", e);
        }
    }
}
