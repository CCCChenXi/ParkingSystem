package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.service.ParkingService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;


@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/parking-lots")
public class ParkingController {

    private final ParkingService parkingService;

    /**
     * 获取所有停车场列表
     */
    @GetMapping
    public Result listAll() {
        log.info("获取所有停车场列表");
        return parkingService.listAll();
    }

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
        log.info("获取附近停车场列表: longitude={}, latitude={}, radius={}", longitude, latitude, radius);
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
        log.info("获取停车场详情: id={}", id);
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
        log.info("获取停车场车位信息: id={}", id);
        return parkingService.parkingSpots(id);
//        return Result.ok();
    }

}
