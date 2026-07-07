package com.xigeandwillian.parkingsystem.client.vo.coupon;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CouponScrollVO<T> {
    private List<T> list;
    private Long nextTimestamp;
    private Long nextId;
    private boolean hasMore;
}
