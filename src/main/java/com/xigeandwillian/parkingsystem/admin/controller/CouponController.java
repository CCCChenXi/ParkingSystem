package com.xigeandwillian.parkingsystem.admin.controller;

import com.xigeandwillian.parkingsystem.admin.dto.coupon.CouponSaveDTO;
import com.xigeandwillian.parkingsystem.admin.service.Service.CouponService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/coupons")
public class CouponController {
    private final CouponService couponService;

    @GetMapping
    public Result list() {
        log.info("获取优惠券列表");
        return couponService.list();
    }

    @PostMapping
    public Result create(@Validated @RequestBody CouponSaveDTO dto) {
        log.info("新增优惠券: name={}", dto.getName());
        return couponService.create(dto);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Validated @RequestBody CouponSaveDTO dto) {
        log.info("更新优惠券: id={}", id);
        return couponService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        log.info("删除优惠券: id={}", id);
        return couponService.delete(id);
    }
}
