package com.xigeandwillian.parkingsystem.client.vo.parkinglot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotVO {
    private Long id;
    private String name;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer totalSpots;
    private Integer availableSpots;
    private String imageUrl;
    private Integer status;
    private String distance;
}
