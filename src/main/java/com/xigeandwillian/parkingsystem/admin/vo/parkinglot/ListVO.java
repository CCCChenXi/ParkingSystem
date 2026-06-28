package com.xigeandwillian.parkingsystem.admin.vo.parkinglot;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ListVO {
    private Long id;
    private String name;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer totalSpots;
    private Integer availableSpots;
    private Integer status;
}