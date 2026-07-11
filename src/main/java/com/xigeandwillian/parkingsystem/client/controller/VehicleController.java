package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.dto.user.VehicleDTO;
import com.xigeandwillian.parkingsystem.client.service.VehicleService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    /**
     * 获取用户车辆信息
     *
     * @return
     * @author willian
     */
    @GetMapping
    public Result vehiclesInfo() {
        log.info("获取用户车辆信息");
        return vehicleService.vehiclesInfo();
    }

    /**
     * 添加车辆
     *
     * @return
     * @author willian
     */
    @PostMapping
    public Result addVehicle(@Validated @RequestBody VehicleDTO vehicleDTO) {
        log.info("现在执行添加车辆: plateNumber={}", vehicleDTO.getPlateNumber());
        return vehicleService.addVehicle(vehicleDTO);
    }

    /**
     * 更新车辆
     *
     * @return
     * @author willian
     */
    @PutMapping("/{id}")
    public Result updateVehicle(@Validated @RequestBody VehicleDTO vehicleDTO, @PathVariable Long id) {
        log.info("现在执行更新车辆: id={}", id);
        return vehicleService.updateVehicle(vehicleDTO, id);
    }

    /**
     * 删除车辆
     *
     * @return
     * @author willian
     */
    @DeleteMapping("/{id}")
    public Result deleteVehicle(@PathVariable Long id) {
        log.info("现在执行删除车辆: id={}", id);
        return vehicleService.deleteVehicle(id);
    }

}

