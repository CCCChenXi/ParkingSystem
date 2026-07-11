package com.xigeandwillian.parkingsystem.admin.service.impl;

import com.xigeandwillian.parkingsystem.admin.dto.coupon.CouponSaveDTO;
import com.xigeandwillian.parkingsystem.common.service.impl.CouponDataProvider;
import com.xigeandwillian.parkingsystem.common.mapper.CouponConverter;
import com.xigeandwillian.parkingsystem.common.mapper.CouponMapper;
import com.xigeandwillian.parkingsystem.admin.service.AdminCouponService;
import com.xigeandwillian.parkingsystem.admin.vo.coupon.CouponListVO;
import com.xigeandwillian.parkingsystem.common.constant.CouponConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.Coupon;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminCouponServiceImpl implements AdminCouponService {

    private final CouponConverter couponConverter;
    private final CouponMapper couponMapper;
    private final CouponDataProvider dataProvider;

    @Override
    public Result list() {
        List<Coupon> list = couponMapper.selectList(null);
        List<CouponListVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());
        return Result.ok(voList);
    }

    @Override
    public Result create(CouponSaveDTO dto) {
        validateParams(dto);

        Coupon coupon = couponConverter.toEntity(dto);

        couponMapper.insert(coupon);

        if (dto.getType() == CouponConstant.COUPON_TYPE_SECKILL) {
            dataProvider.initSecKillStock(coupon.getId(), dto.getStock());
        }

        dataProvider.invalidateAvailable();
        log.info("新增优惠券成功: name={}, id={}", dto.getName(), coupon.getId());
        return Result.ok();
    }

    @Override
    public Result update(Long id, CouponSaveDTO dto) {
        Coupon existing = couponMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券不存在");
        }

        validateParams(dto);

        Coupon coupon = couponConverter.toEntity(dto);
        coupon.setId(id);

        couponMapper.updateById(coupon);

        if (dto.getType() == CouponConstant.COUPON_TYPE_SECKILL) {
            if (existing.getType() == CouponConstant.COUPON_TYPE_SECKILL && !dto.getStock().equals(existing.getStock())) {
                dataProvider.resetSecKillStock(id, dto.getStock());
            } else if (existing.getType() != CouponConstant.COUPON_TYPE_SECKILL) {
                dataProvider.initSecKillStock(id, dto.getStock());
            }
        }

        invalidateDetailCache();
        dataProvider.invalidateAvailable();
        log.info("更新优惠券成功: id={}", id);
        return Result.ok();
    }

    @Override
    public Result delete(Long id) {
        Coupon existing = couponMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券不存在");
        }

        couponMapper.deleteById(id);

        invalidateDetailCache();
        dataProvider.invalidateAvailable();
        log.info("删除优惠券成功: id={}", id);
        return Result.ok();
    }

    private void validateParams(CouponSaveDTO dto) {
        if (dto.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠金额必须大于0");
        }
        if (dto.getStock() <= 0) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "库存必须大于0");
        }
        int type = dto.getType();
        if (type != CouponConstant.COUPON_TYPE_NORMAL && type != CouponConstant.COUPON_TYPE_SECKILL) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券类型只能是 0 或 1");
        }
    }

    private void invalidateDetailCache() {
        dataProvider.invalidateDetail();
    }

    private CouponListVO toVO(Coupon coupon) {
        return couponConverter.toListVO(coupon);
    }
}
