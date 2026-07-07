package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.service.service.CouponService;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public Result list(
            @RequestParam String scope,
            @RequestParam(required = false) Long lastTimestamp,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        log.info("查询我的优惠卷列表: scope={}, lastTimestamp={}, lastId={}, pageSize={}, status={}, keyword={}",
                scope, lastTimestamp, lastId, pageSize, status, keyword);
        if ("mine".equals(scope)) {
            return couponService.myCoupons(lastTimestamp, lastId, pageSize, status, keyword);
        }
        throw new BusinessException(ResultConstant.BAD_REQUEST, "不支持的 scope: " + scope);
    }

    @GetMapping("/available")
    public Result scrollQuery(
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Long lastTimestamp,
            @RequestParam(required = false) Long lastId) {
        log.info("滚动分页查询可领取优惠卷列表: pageSize={}, lastTimestamp={}, lastId={}",
                pageSize, lastTimestamp, lastId);
        return couponService.scrollQuery(pageSize, lastTimestamp, lastId);
    }

    @GetMapping("/available/{id}/detail")
    public Result availableDetail(@PathVariable Long id) {
        log.info("查询可领取优惠券详情: {}", id);
        return couponService.availableDetail(id);
    }

    @GetMapping("/mine/{id}/detail")
    public Result mineDetail(@PathVariable Long id) {
        log.info("查询我的优惠券详情: {}", id);
        return couponService.mineDetail(id);
    }

    @PostMapping("/claim/{id}")
    public Result claim(@PathVariable Long id) {
        log.info("领取优惠券: {}", id);
        return couponService.claim(id);
    }

    @PostMapping("/flash/{id}")
    public Result flash(@PathVariable Long id){
        log.info("抢购秒杀卷：{}",id);
        return couponService.flash(id);
    }
}
