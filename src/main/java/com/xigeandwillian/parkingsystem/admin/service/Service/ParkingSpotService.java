package com.xigeandwillian.parkingsystem.admin.service.Service;

import com.xigeandwillian.parkingsystem.admin.dto.parkingspot.SpotInsertDTO;
import com.xigeandwillian.parkingsystem.admin.dto.parkingspot.SpotUpdateDTO;
import com.xigeandwillian.parkingsystem.common.result.Result;

public interface ParkingSpotService {
    Result listSpotsByLotId(Long lotId);

    Result deleteParkingSpot(Long lotId, Long id);

    Result batchCreateSpots(SpotInsertDTO spotInsertDTO);

    Result updateParkingSpot(Long lotId, Long id, SpotUpdateDTO spotUpdateDTO);
}
