package com.xigeandwillian.parkingsystem.admin.controller;

import com.xigeandwillian.parkingsystem.admin.dto.parkinglot.LotSaveDTO;
import com.xigeandwillian.parkingsystem.admin.service.ParkingLotService;
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
    private final ParkingLotService parkingLotService;

    @GetMapping()
    public Result listParkingLots(Integer page,Integer size,String keyword,Integer status){
        log.info("获取停车场信息: page={}, size={}, keyword={}, status={}", page, size, keyword, status);
        return parkingLotService.listParkingLots(page,size,keyword,status);
    }

    @DeleteMapping("/{id}")
    public Result deleteParkingLot(@PathVariable Long id){
        log.info("删除停车场: lotId={}", id);
        return parkingLotService.deleteParkingLot(id);
    }

    @PutMapping("/{id}")
    public Result updateParkingLot(@PathVariable Long id,@Validated @RequestBody LotSaveDTO lotSaveDTO){
        log.info("更新停车场: lotId={}", id);
        return parkingLotService.updateParkingLot(id, lotSaveDTO);
    }

    @PostMapping
    public Result createParkingLot(@Validated @RequestBody LotSaveDTO lotSaveDTO){
        log.info("新增停车场: name={}", lotSaveDTO.getName());
        return parkingLotService.createParkingLot(lotSaveDTO);
    }

    @GetMapping("/names")
    public Result listParkingLotNames(){
        log.info("获取停车场名字静态资源");
        return parkingLotService.listParkingLotNames();
    }
}
