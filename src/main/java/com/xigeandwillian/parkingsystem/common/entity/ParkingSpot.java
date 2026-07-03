package com.xigeandwillian.parkingsystem.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("parking_spot")
public class ParkingSpot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lotId;
    private Integer seq;
    private String spotNumber;
    private Integer type;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
