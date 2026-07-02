package com.xigeandwillian.parkingsystem.admin.controller;

import com.xigeandwillian.parkingsystem.admin.dto.parkingspot.SpotInsertDTO;
import com.xigeandwillian.parkingsystem.admin.dto.parkingspot.SpotUpdateDTO;
import com.xigeandwillian.parkingsystem.admin.service.Service.ParkingSpotService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/parking-spot")
public class ParkingSpotController {

    private final ParkingSpotService parkingSpotService;

    @GetMapping("/{lotId}")
    public Result listSpotsByLotId(@PathVariable Long lotId){
        log.info("查询车位列表: lotId={}", lotId);
        return parkingSpotService.listSpotsByLotId(lotId);
    }

    @DeleteMapping("/{lotId}/{id}")
    public Result deleteParkingSpot(@PathVariable Long lotId,@PathVariable Long id){
        log.info("删除车位: lotId={}, id={}", lotId, id);
        return parkingSpotService.deleteParkingSpot(lotId,id);
    }

    @PutMapping("/{lotId}/{id}")
    public Result updateParkingSpot(@PathVariable Long lotId,@PathVariable Long id,@RequestBody SpotUpdateDTO spotUpdateDTO){
        log.info("更新车位: lotId={}, id={}", lotId, id);
        return parkingSpotService.updateParkingSpot(lotId,id,spotUpdateDTO);
    }

    @PostMapping
    public Result batchCreateSpots(@Validated @RequestBody SpotInsertDTO spotInsertDTO){
        log.info("批量新增车位: lotId={}", spotInsertDTO.getLotId());
        return parkingSpotService.batchCreateSpots(spotInsertDTO);
    }

}
