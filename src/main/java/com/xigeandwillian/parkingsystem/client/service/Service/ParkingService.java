package com.xigeandwillian.parkingsystem.client.service.Service;

import com.xigeandwillian.parkingsystem.common.result.Result;

import java.math.BigDecimal;

public interface ParkingService {
    Result parkingList(BigDecimal longitude, BigDecimal latitude);

    Result parkingInfo(Long id);

    Result parkingSpots(Long id);
}
