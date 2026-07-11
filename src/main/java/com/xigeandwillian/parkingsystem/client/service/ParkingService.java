package com.xigeandwillian.parkingsystem.client.service;

import com.xigeandwillian.parkingsystem.common.result.Result;

import java.math.BigDecimal;

public interface ParkingService {
    Result parkingList(BigDecimal longitude, BigDecimal latitude, long radius);

    Result parkingInfo(Long id);

    Result parkingSpots(Long id);

    Result listAll();
}
