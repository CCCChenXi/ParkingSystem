package com.xigeandwillian.parkingsystem.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xigeandwillian.parkingsystem.admin.dto.parkingspot.SpotInsertDTO;
import com.xigeandwillian.parkingsystem.admin.dto.parkingspot.SpotUpdateDTO;
import com.xigeandwillian.parkingsystem.admin.mapper.ParkingLotMapper;
import com.xigeandwillian.parkingsystem.admin.mapper.ParkingSpotMapper;
import com.xigeandwillian.parkingsystem.admin.service.Service.ParkingSpotService;
import com.xigeandwillian.parkingsystem.admin.vo.parkingspot.SpotListVO;
import com.xigeandwillian.parkingsystem.common.cache.ParkingCache;
import com.xigeandwillian.parkingsystem.common.constant.CacheConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.entity.ParkingSpot;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingSpotServiceImpl implements ParkingSpotService {
    private final ParkingSpotMapper parkingSpotMapper;
    private final ParkingLotMapper parkingLotMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ParkingCache parkingCache;

    @Override
    @CacheEvict(cacheNames = CacheConstant.PARKING_SPOT_LIST, key = "#lotId")
    public Result deleteParkingSpot(Long lotId, Long id) {
        parkingSpotMapper.delete(new QueryWrapper<ParkingSpot>().eq("lot_id", lotId).eq("id", id));
        return Result.ok();
    }

    @Override
    public Result listSpotsByLotId(Long lotId) {
        Map<Object, Object> spotStatus = stringRedisTemplate.opsForHash().entries(RedisConstant.Parking.PARKING_SPOT_STATUS + lotId);
        List<SpotListVO> voList = parkingCache.getSpotsByLotId(lotId).stream().map(item -> {
            SpotListVO vo = new SpotListVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).toList();
        voList.forEach(item-> {
            Object val = spotStatus.get(item.getId().toString());
            item.setStatus(val != null ? Integer.parseInt(val.toString()) : null);
        });
        return Result.ok(voList);
    }

    @Override
    public Result init() {
        List<ParkingLot> lots = parkingLotMapper.selectList(null);
        lots.forEach(lot->{
            List<ParkingSpot> spots = parkingSpotMapper.selectList(new QueryWrapper<ParkingSpot>().eq("lot_id", lot.getId()));
            Map<Object,Object> status = new HashMap<>();
            spots.forEach(spot->{
                status.put(spot.getId().toString(), "0");
            });
            stringRedisTemplate.opsForHash().putAll(RedisConstant.Parking.PARKING_SPOT_STATUS + lot.getId(),status);
        });
        return Result.ok();
    }

    @Override
    @CacheEvict(cacheNames = CacheConstant.PARKING_SPOT_LIST, key = "#lotId")
    public Result updateParkingSpot(Long lotId, Long id, SpotUpdateDTO spotUpdateDTO) {
        ParkingSpot parkingSpot = new ParkingSpot();
        BeanUtils.copyProperties(spotUpdateDTO, parkingSpot);
        parkingSpot.setId(id);
        parkingSpot.setLotId(lotId);
        parkingSpotMapper.update(parkingSpot, new QueryWrapper<ParkingSpot>().eq("lot_id", lotId).eq("id", id));
        return Result.ok();
    }

    @Override
    @CacheEvict(cacheNames = CacheConstant.PARKING_SPOT_LIST, key = "#spotInsertDTO.lotId")
    public Result batchCreateSpots(SpotInsertDTO spotInsertDTO) {
        List<ParkingSpot> spots = spotInsertDTO.getSpotNumbers().stream().map(number -> {
            ParkingSpot spot = new ParkingSpot();
            spot.setLotId(spotInsertDTO.getLotId());
            spot.setSpotNumber(number);
            spot.setType(spotInsertDTO.getType());
            return spot;
        }).toList();
        Map<Object, Object> status = stringRedisTemplate.opsForHash().entries(RedisConstant.Parking.PARKING_SPOT_STATUS + spotInsertDTO.getLotId());

        spots.forEach(item->{
            try{
                parkingSpotMapper.insert(item);
                status.put(item.getId().toString(),"0");
            }
            catch(Exception ignored){
                log.warn("{}插入失败",item.getSpotNumber());
                log.error(ignored.getMessage());
            }
        });
        stringRedisTemplate.opsForHash().putAll(RedisConstant.Parking.PARKING_SPOT_STATUS + spotInsertDTO.getLotId(), status);
        return Result.ok();
    }
}
