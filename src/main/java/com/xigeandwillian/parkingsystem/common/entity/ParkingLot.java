package com.xigeandwillian.parkingsystem.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("parking_lot")
public class ParkingLot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer totalSpots;
    private Integer availableSpots;
    private BigDecimal hourlyRate;
    private String imageUrl;
    private Integer status;
    private LocalDateTime createTime;
}
