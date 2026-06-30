package com.xigeandwillian.parkingsystem.common.cache;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xigeandwillian.parkingsystem.admin.mapper.ParkingLotMapper;
import com.xigeandwillian.parkingsystem.admin.mapper.ParkingSpotMapper;
import com.xigeandwillian.parkingsystem.common.constant.CacheConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.entity.ParkingSpot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingCache {

    private final ParkingLotMapper parkingLotMapper;
    private final ParkingSpotMapper parkingSpotMapper;

    @Cacheable(cacheNames = CacheConstant.PARKING_LOT_LIST)
    public List<ParkingLot> getLotList() {
        log.info("查询全部停车场信息");
        return parkingLotMapper.selectList(null);
    }

    @Cacheable(cacheNames = CacheConstant.PARKING_SPOT_LIST, key = "#lotId")
    public List<ParkingSpot> getSpotsByLotId(Long lotId) {
        log.info("查询停车场车位信息: {}", lotId);
        return parkingSpotMapper.selectList(new QueryWrapper<ParkingSpot>().eq("lot_id", lotId));
    }
}
