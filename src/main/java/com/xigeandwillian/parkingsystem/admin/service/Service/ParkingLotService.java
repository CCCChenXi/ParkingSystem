package com.xigeandwillian.parkingsystem.admin.service.Service;

import com.xigeandwillian.parkingsystem.admin.dto.parkinglot.SaveDTO;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.result.Result;

public interface ParkingLotService {
    Result list();

    Result delete(Long id);

    Result update(Long id, SaveDTO saveDTO);

    Result insert(SaveDTO saveDTO);
}
