package com.xigeandwillian.parkingsystem.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private BigDecimal discountAmount;
    private BigDecimal minAmount;
    private Integer type;
    private Integer stock;
    private Integer remainStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}
