package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.service.Service.ParkingService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

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
    public Result parkingList(@RequestParam BigDecimal longitude,
                              @RequestParam BigDecimal latitude
    ) {
        return parkingService.parkingList(longitude, latitude);
    }

    /**
     * 获取停车场详情
     *
     * @param id 停车场id
     * @return
     */
    @GetMapping("/{id}")
    public Result parkingInfo(@PathVariable Long id) {
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
        return parkingService.parkingSpots(id);
    }

}
