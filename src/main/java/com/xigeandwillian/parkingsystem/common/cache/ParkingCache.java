package com.xigeandwillian.parkingsystem.common.cache;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xigeandwillian.parkingsystem.admin.mapper.ParkingLotMapper;
import com.xigeandwillian.parkingsystem.admin.mapper.AdminParkingSpotMapper;
import com.xigeandwillian.parkingsystem.admin.vo.parkinglot.LotListVO;
import com.xigeandwillian.parkingsystem.admin.vo.parkingspot.SpotListVO;
import com.xigeandwillian.parkingsystem.common.constant.CacheConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingSpot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingCache {

    private final ParkingLotMapper parkingLotMapper;
    private final AdminParkingSpotMapper adminParkingSpotMapper;
    private final CacheManager cacheManager;
    private final StringRedisTemplate stringRedisTemplate;

    @Cacheable(cacheNames = CacheConstant.PARKING_LOT_LIST)
    public List<LotListVO> getLotList() {
        log.info("查询全部停车场信息");
        return parkingLotMapper.selectList(null).stream().map(lot -> {
            LotListVO vo = new LotListVO();
            BeanUtils.copyProperties(lot, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Cacheable(cacheNames = CacheConstant.PARKING_SPOT_LIST, key = "#lotId")
    public List<SpotListVO> getSpotsByLotId(Long lotId) {
        log.info("查询停车场车位信息: {}", lotId);
        return adminParkingSpotMapper.selectList(new QueryWrapper<ParkingSpot>().eq("lot_id", lotId))
                .stream().map(spot -> {
                    SpotListVO vo = new SpotListVO();
                    BeanUtils.copyProperties(spot, vo);
                    return vo;
                }).collect(Collectors.toList());
    }

    public void clearAll() {
        log.info("清除所有停车场缓存");
        Cache lotListCache = cacheManager.getCache(CacheConstant.PARKING_LOT_LIST);
        if (lotListCache != null) {
            lotListCache.clear();
        }
        Cache nameListCache = cacheManager.getCache(CacheConstant.PARKING_LOT_NAME_LIST);
        if (nameListCache != null) {
            nameListCache.clear();
        }
        Cache spotListCache = cacheManager.getCache(CacheConstant.PARKING_SPOT_LIST);
        if (spotListCache != null) {
            spotListCache.clear();
        }

        Set<String> keys = new HashSet<>();
        try {
            Set<String> spotStatusKeys = stringRedisTemplate.keys(RedisConstant.Parking.PARKING_SPOT_STATUS + "*");
            Set<String> legacySpotStatusKeys = stringRedisTemplate.keys("parking:spot_status:*");
            if (legacySpotStatusKeys != null) {
                keys.addAll(legacySpotStatusKeys);
            }
            if (spotStatusKeys != null) {
                keys.addAll(spotStatusKeys);
            }
            Set<String> availableKeys = stringRedisTemplate.keys(RedisConstant.Parking.PARKING_LOT_AVAILABLE + "*");
            if (availableKeys != null) {
                keys.addAll(availableKeys);
            }
            Set<String> infoKeys = stringRedisTemplate.keys(RedisConstant.Parking.PARKING_LOT_INFO + "*");
            if (infoKeys != null) {
                keys.addAll(infoKeys);
            }
            keys.add(RedisConstant.Parking.PARKING_GEO);
            if (!keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
            log.info("停车场缓存清除完成, 共删除{}个key", keys.size());
        } catch (Exception e) {
            log.error("清除Redis缓存失败", e);
        }
    }
}
