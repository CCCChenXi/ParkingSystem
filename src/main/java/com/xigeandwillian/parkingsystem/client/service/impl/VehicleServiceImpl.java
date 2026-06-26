package com.xigeandwillian.parkingsystem.client.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.client.mapper.VehicleMapper;
import com.xigeandwillian.parkingsystem.client.service.Service.VehicleService;
import com.xigeandwillian.parkingsystem.common.entity.Vehicle;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleMapper vehicleMapper;

    /**
     * 获取用户车辆信息
     *
     * @return
     */
    @Override
    public Result vehiclesInfo() {
        long userId = UserHolder.get();
        List<Vehicle> vehicles = vehicleMapper.selectList(Wrappers.<Vehicle>lambdaQuery()
                .eq(Vehicle::getUserId, userId)
        );
        return Result.ok(vehicles);
    }
}
