package com.xigeandwillian.parkingsystem.admin.service.impl;

import com.xigeandwillian.parkingsystem.admin.dto.parkinglot.SaveDTO;
import com.xigeandwillian.parkingsystem.admin.mapper.ParkingLotMapper;
import com.xigeandwillian.parkingsystem.admin.service.Service.ParkingLotService;
import com.xigeandwillian.parkingsystem.admin.vo.parkinglot.ListVO;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xige
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingLotServiceImpl implements ParkingLotService {

    private final ParkingLotMapper parkingLotMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result insert(SaveDTO saveDTO) {
        ParkingLot parkingLot = new ParkingLot();
        BeanUtils.copyProperties(saveDTO, parkingLot);
        parkingLotMapper.insert(parkingLot);
        stringRedisTemplate.opsForGeo().add(RedisConstant.Vehicle.PARKING_GEO,new Point(parkingLot.getLongitude().doubleValue(),parkingLot.getLatitude().doubleValue()),parkingLot.getId().toString());
        return Result.ok();
    }

    @Override
    public Result update(Long id, SaveDTO saveDTO) {
        ParkingLot parkingLot = new ParkingLot();
        BeanUtils.copyProperties(saveDTO, parkingLot);
        parkingLot.setId(id);
        parkingLotMapper.updateById(parkingLot);
        stringRedisTemplate.opsForGeo().add(RedisConstant.Vehicle.PARKING_GEO,new Point(parkingLot.getLongitude().doubleValue(),parkingLot.getLatitude().doubleValue()),id.toString());
        return Result.ok();
    }

    @Override
    public Result delete(Long id) {
        parkingLotMapper.deleteById(id);
        return Result.ok();
    }

    @Override
    public Result list() {
        List<ParkingLot> list = parkingLotMapper.selectList(null);
        List<ListVO> voList = list.stream().map(lot->{
                ListVO vo = new ListVO();
                BeanUtils.copyProperties(lot,vo);
                return vo;
        }).collect(Collectors.toList());
        return Result.ok(voList);
    }
}
