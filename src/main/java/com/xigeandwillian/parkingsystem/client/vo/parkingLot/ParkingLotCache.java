package com.xigeandwillian.parkingsystem.client.vo.parkingLot;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;


@Data
@AllArgsConstructor
public class ParkingLotCache {
    private Long id;
    private String name;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer totalSpots;
    private String imageUrl;
    private Integer status;
}
