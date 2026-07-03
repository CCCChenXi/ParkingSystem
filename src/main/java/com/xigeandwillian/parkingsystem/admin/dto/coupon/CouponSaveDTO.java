package com.xigeandwillian.parkingsystem.admin.dto.coupon;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponSaveDTO {
    @NotBlank(message = "优惠券名称不能为空")
    private String name;

    private String description;

    @NotNull(message = "优惠金额不能为空")
    private BigDecimal discountAmount;

    private BigDecimal minAmount;

    @NotNull(message = "类型不能为空")
    private Integer type;

    @NotNull(message = "库存不能为空")
    private Integer stock;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}
