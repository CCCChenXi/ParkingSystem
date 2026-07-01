package com.xigeandwillian.parkingsystem.admin.service.Service;

import com.xigeandwillian.parkingsystem.admin.dto.parkinglot.LotSaveDTO;
import com.xigeandwillian.parkingsystem.common.result.Result;

public interface ParkingLotService {
    Result listParkingLots(Integer page, Integer size, String keyword, Integer status);

    Result deleteParkingLot(Long id);

    Result updateParkingLot(Long id, LotSaveDTO lotSaveDTO);

    Result createParkingLot(LotSaveDTO lotSaveDTO);

    Result listParkingLotNames();
}
