package com.xigeandwillian.parkingsystem.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("parking_order")
public class ParkingOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderNo;
    private Long userId;
    private Long lotId;
    private Long spotId;
    private String plateNumber;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal hourlyRate;
    private BigDecimal amount;
    private Long couponId;
    private BigDecimal discount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
