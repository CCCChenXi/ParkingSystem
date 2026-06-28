package com.xigeandwillian.parkingsystem.admin.controller;

import com.xigeandwillian.parkingsystem.admin.dto.parkinglot.SaveDTO;
import com.xigeandwillian.parkingsystem.admin.service.Service.ParkingLotService;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/parking-lots")
@RequiredArgsConstructor
@Slf4j
public class ParkingLotController {
    public final ParkingLotService parkingLotService;

    @GetMapping()
    public Result list(){
        log.info("获取停车场信息");
        return parkingLotService.list();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id){
        log.info("删除停车场信息");
        return parkingLotService.delete(id);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id,@Validated @RequestBody SaveDTO saveDTO){
        log.info("修改停车场信息");
        return parkingLotService.update(id,saveDTO);
    }

    @PostMapping
    public Result insert(@Validated @RequestBody SaveDTO saveDTO){
        log.info("新增停车场信息");
        return parkingLotService.insert(saveDTO);
    }
}
