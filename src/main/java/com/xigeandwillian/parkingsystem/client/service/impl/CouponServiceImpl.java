package com.xigeandwillian.parkingsystem.client.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.client.service.service.CouponService;
import com.xigeandwillian.parkingsystem.client.vo.coupon.CouponAvailableVO;
import com.xigeandwillian.parkingsystem.client.vo.coupon.CouponDetailVO;
import com.xigeandwillian.parkingsystem.client.vo.coupon.CouponScrollVO;
import com.xigeandwillian.parkingsystem.client.mq.SeckillMessageProducer;
import com.xigeandwillian.parkingsystem.client.mq.SeckillOrderEvent;
import com.xigeandwillian.parkingsystem.client.vo.coupon.UserCouponMyVO;
import com.xigeandwillian.parkingsystem.common.constant.CouponConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.Coupon;
import com.xigeandwillian.parkingsystem.common.entity.UserCoupon;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.mapper.CouponMapper;
import com.xigeandwillian.parkingsystem.common.mapper.UserCouponMapper;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponDataProvider couponDataProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillMessageProducer seckillMessageProducer;

    private static final DefaultRedisScript<Long> SECKILL_LUA_SCRIPT;

    static {
        SECKILL_LUA_SCRIPT = new DefaultRedisScript<>();
        SECKILL_LUA_SCRIPT.setLocation(new ClassPathResource("lua/seckillCoupon.lua"));
        SECKILL_LUA_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result availableDetail(Long id) {
        log.info("查询可领取优惠券详情: {}", id);

        CouponDetailVO vo = couponDataProvider.getDetailById(id);
        if (vo == null) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券不存在");
        }

        couponDataProvider.fillRemainStock(vo);

        Long userId = UserHolder.get();
        List<Object> statuses = couponDataProvider.getCouponClaimStatus(userId, List.of(id));
        vo.setClaim(Boolean.TRUE.equals(statuses.get(0)));

        return Result.ok(vo);
    }

    @Override
    public Result mineDetail(Long id) {
        log.info("查询我的优惠券详情: {}", id);

        Long userId = UserHolder.get();

        UserCoupon uc = userCouponMapper.selectOne(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, id));
        if (uc == null) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "未领取该优惠券");
        }

        CouponDetailVO vo = couponDataProvider.getCouponStatic(id);
        if (vo == null) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券不存在");
        }

        vo.setStock(null);
        vo.setRemainStock(null);
        vo.setStatus(uc.getStatus());

        return Result.ok(vo);
    }

    @Override
    public Result scrollQuery(Integer pageSize, Long lastTimestamp, Long lastId) {
        int ps = (pageSize != null && pageSize > 0) ? pageSize : 10;

        List<CouponAvailableVO> all = couponDataProvider.getClaimableCoupons();

        int start = (lastTimestamp == null || lastId == null)
                ? 0
                : binarySearchCursor(all, lastTimestamp, lastId);

        int end = Math.min(start + ps, all.size());
        List<CouponAvailableVO> pageData = all.subList(start, end);
        boolean hasMore = end < all.size();

        if (!pageData.isEmpty()) {
            Long userId = UserHolder.get();
            List<Long> ids = pageData.stream().map(CouponAvailableVO::getId).collect(Collectors.toList());
            List<Object> claimStatuses = couponDataProvider.getCouponClaimStatus(userId, ids);
            for (int i = 0; i < pageData.size(); i++) {
                pageData.get(i).setClaim((Boolean) claimStatuses.get(i));
            }
        }

        Long nextTimestamp = null;
        Long nextId = null;
        if (hasMore) {
            CouponAvailableVO last = pageData.get(pageData.size() - 1);
            nextTimestamp = last.getStartTime()
                    .toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
            nextId = last.getId();
        }

        return Result.ok(new CouponScrollVO<>(pageData, nextTimestamp, nextId, hasMore));
    }

    @Override
    public Result flash(Long id) {
        log.info("秒杀优惠券: id={}", id);

        CouponDetailVO vo = couponDataProvider.getDetailById(id);
        if (vo == null) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券不存在");
        }
        if (vo.getType() != CouponConstant.COUPON_TYPE_SECKILL) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "非秒杀优惠券");
        }
        if (vo.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券已过期");
        }

        Long userId = UserHolder.get();

        Long execute = stringRedisTemplate.execute(SECKILL_LUA_SCRIPT,
                Arrays.asList(RedisConstant.Coupon.SECKILL_STOCK + id, RedisConstant.Coupon.BOUGHT_KEY + id),
                String.valueOf(userId));

        if (execute == CouponConstant.SECKILL_RESULT_ALREADY_CLAIMED) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "你已经抢购过该优惠券了~");
        }
        if (execute == CouponConstant.SECKILL_RESULT_SOLD_OUT) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券已经被抢完了，请下次再来~");
        }

        seckillMessageProducer.send(new SeckillOrderEvent(userId, id));
        return Result.ok("成功抢购优惠券");
    }

    @Override
    public Result myCoupons(Long lastTimestamp, Long lastId, Integer pageSize, Integer status, String keyword) {
        int ps = (pageSize != null && pageSize > 0) ? pageSize : 10;

        Long userId = UserHolder.get();

        List<UserCoupon> userCoupons = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(status != null, UserCoupon::getStatus, status)
                .orderByDesc(UserCoupon::getCreateTime, UserCoupon::getId));

        if (userCoupons.isEmpty()) {
            return Result.ok(new CouponScrollVO<>(List.of(), null, null, false));
        }

        List<Long> couponIds = userCoupons.stream().map(UserCoupon::getCouponId).toList();
        Map<Long, Coupon> couponMap = couponMapper.selectBatchIds(couponIds)
                .stream().collect(Collectors.toMap(Coupon::getId, c -> c));

        List<UserCouponMyVO> all = userCoupons.stream()
                .filter(uc -> {
                    Coupon c = couponMap.get(uc.getCouponId());
                    if (c == null) return false;
                    if (keyword != null && !keyword.isEmpty() && !c.getName().contains(keyword)) {
                        return false;
                    }
                    return true;
                })
                .map(uc -> toMyVO(uc, couponMap.get(uc.getCouponId())))
                .collect(Collectors.toList());

        int start = (lastTimestamp == null || lastId == null)
                ? 0
                : binarySearchCursorMy(all, lastTimestamp, lastId);

        int end = Math.min(start + ps, all.size());
        List<UserCouponMyVO> pageData = all.subList(start, end);
        boolean hasMore = end < all.size();

        Long nextTimestamp = null;
        Long nextId = null;
        if (hasMore) {
            UserCouponMyVO last = pageData.get(pageData.size() - 1);
            nextTimestamp = last.getCreateTime()
                    .toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
            nextId = last.getId();
        }

        return Result.ok(new CouponScrollVO<>(pageData, nextTimestamp, nextId, hasMore));
    }

    private UserCouponMyVO toMyVO(UserCoupon uc, Coupon c) {
        UserCouponMyVO vo = new UserCouponMyVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setDescription(c.getDescription());
        vo.setDiscountAmount(c.getDiscountAmount());
        vo.setMinAmount(c.getMinAmount());
        vo.setType(c.getType());
        vo.setStatus(uc.getStatus());
        vo.setCreateTime(uc.getCreateTime());
        vo.setStartTime(c.getStartTime());
        vo.setEndTime(c.getEndTime());
        return vo;
    }

    private int binarySearchCursorMy(List<UserCouponMyVO> list, long lastTs, long lastId) {
        int left = 0, right = list.size();
        while (left < right) {
            int mid = (left + right) >>> 1;
            UserCouponMyVO vo = list.get(mid);
            long ts = vo.getCreateTime()
                    .toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
            if (ts < lastTs || (ts == lastTs && vo.getId() < lastId)) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        return left;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result claim(Long id) {
        CouponDetailVO vo = couponDataProvider.getDetailById(id);
        if (vo == null || vo.getType() != CouponConstant.COUPON_TYPE_NORMAL) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券不存在");
        }

        long userId = UserHolder.get();

        Long count = userCouponMapper.selectCount(
                Wrappers.<UserCoupon>lambdaQuery()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, id));
        if (count > 0) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "已领取过该优惠券");
        }

        int rows = couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                .setSql("remain_stock = remain_stock - 1")
                .eq(Coupon::getId, id)
                .gt(Coupon::getRemainStock, 0));
        if (rows == 0) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "领取优惠券失败");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(id);
        userCoupon.setStatus(CouponConstant.USER_COUPON_STATUS_UNUSED);
        userCouponMapper.insert(userCoupon);

        try {
            stringRedisTemplate.opsForSet().add(RedisConstant.Coupon.BOUGHT_KEY + id, String.valueOf(userId));
        } catch (Exception e) {
            log.warn("写入优惠券购买状态到Redis失败: couponId={}, userId={}", id, userId, e);
        }

        log.info("领取优惠券成功: userId={}, couponId={}", userId, id);
        return Result.ok();
    }

    private int binarySearchCursor(List<CouponAvailableVO> list, long lastTs, long lastId) {
        int left = 0, right = list.size();
        while (left < right) {
            int mid = (left + right) >>> 1;
            CouponAvailableVO vo = list.get(mid);
            long ts = vo.getStartTime()
                    .toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
            if (ts < lastTs || (ts == lastTs && vo.getId() < lastId)) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        return left;
    }
}
