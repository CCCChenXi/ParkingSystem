package com.xigeandwillian.parkingsystem.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vehicle")
public class Vehicle {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String plateNumber;
    private String brand;
    private String color;
    private LocalDateTime createTime;
}
