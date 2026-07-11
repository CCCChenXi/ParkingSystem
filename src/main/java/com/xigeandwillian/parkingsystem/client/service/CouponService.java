package com.xigeandwillian.parkingsystem.client.service;

import com.xigeandwillian.parkingsystem.common.result.Result;

public interface CouponService {
    Result scrollQuery(Integer pageSize, Long lastTimestamp, Long lastId);

    Result claim(Long id);

    Result myCoupons(Long lastTimestamp, Long lastId, Integer pageSize, Integer status, String keyword);

    Result flash(Long id);

    Result availableDetail(Long id);

    Result mineDetail(Long id);
}
