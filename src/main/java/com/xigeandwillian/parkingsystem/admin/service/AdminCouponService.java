package com.xigeandwillian.parkingsystem.admin.service;

import com.xigeandwillian.parkingsystem.admin.dto.coupon.CouponSaveDTO;
import com.xigeandwillian.parkingsystem.common.result.Result;

public interface AdminCouponService {
    Result list();

    Result create(CouponSaveDTO dto);

    Result update(Long id, CouponSaveDTO dto);

    Result delete(Long id);
}
