package com.xigeandwillian.parkingsystem.client.vo.coupon;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponAvailableVO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal discountAmount;
    private BigDecimal minAmount;
    private Integer type;
    private Boolean claim;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
