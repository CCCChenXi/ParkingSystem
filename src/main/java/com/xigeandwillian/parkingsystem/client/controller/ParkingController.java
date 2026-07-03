package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.service.service.ParkingService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/parking-lots")
public class ParkingController {

    private final ParkingService parkingService;

    /**
     * 获取附近停车场列表
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return
     */
    @GetMapping("/nearby")
    public Result parkingList(@RequestParam @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
                              @RequestParam @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
                              @RequestParam long radius
    ) {
        log.info("获取附近停车场列表");
        return parkingService.parkingList(longitude, latitude, radius);
    }

    /**
     * 获取停车场详情
     *
     * @param id 停车场id
     * @return
     */
    @GetMapping("/{id}")
    public Result parkingInfo(@PathVariable Long id) {
        log.info("获取停车场详情");
        return parkingService.parkingInfo(id);
    }

    /**
     * 获取停车场车位信息
     *
     * @param id 停车场id
     * @return
     */
    @GetMapping("/{id}/spots")
    public Result parkingSpots(@PathVariable Long id) {
        log.info("获取停车场车位信息");
        return parkingService.parkingSpots(id);
    }

}
