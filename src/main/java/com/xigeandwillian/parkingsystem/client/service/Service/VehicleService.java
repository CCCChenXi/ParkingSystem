package com.xigeandwillian.parkingsystem.client.service.Service;

import com.xigeandwillian.parkingsystem.client.dto.user.VehicleDTO;
import com.xigeandwillian.parkingsystem.common.result.Result;

public interface VehicleService {

    Result vehiclesInfo();

    Result addVehicle(VehicleDTO vehicleDTO);

    Result deleteVehicle(Long id);
}
