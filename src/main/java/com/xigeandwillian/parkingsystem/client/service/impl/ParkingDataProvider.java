package com.xigeandwillian.parkingsystem.client.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.xigeandwillian.parkingsystem.client.vo.parkingLot.SpotVO;
import com.xigeandwillian.parkingsystem.common.cache.CacheResult;
import com.xigeandwillian.parkingsystem.common.constant.OrderConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingOrder;
import com.xigeandwillian.parkingsystem.common.entity.ParkingSpot;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingOrderMapper;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingSpotMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class ParkingDataProvider {

    private final ParkingSpotMapper parkingSpotMapper;
    private final ParkingOrderMapper parkingOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Resource(name = "parkingSpotCache")
    private Cache<String, List<SpotVO>> localCache;

    public List<SpotVO> getAllSpotByLotId(Long lotId) {
        String cacheKey = RedisConstant.Parking.PARKING_SPOT_LIST + lotId;

        List<SpotVO> local = localCache.getIfPresent(cacheKey);
        if (local != null) return local;

        CacheResult<List<SpotVO>> redisResult = getSpotListFromRedis(cacheKey);
        if (redisResult.isHit()) {
            localCache.put(cacheKey, redisResult.getData());
            return redisResult.getData();
        }

        List<ParkingSpot> spots = parkingSpotMapper.selectList(
                Wrappers.<ParkingSpot>lambdaQuery()
                        .eq(ParkingSpot::getLotId, lotId)
                        .orderByAsc(ParkingSpot::getSeq));
        List<SpotVO> voList = spots.stream()
                .map(s -> new SpotVO(s.getId(), s.getSeq(), s.getSpotNumber(), s.getType(), 0))
                .toList();

        localCache.put(cacheKey, voList);

        if (!redisResult.isError()) {
            saveSpotListToRedis(cacheKey, voList);
        }

        return voList;
    }

    public CacheResult<List<Integer>> getSpotStatusList(Long lotId) {
        CacheResult<List<Integer>> redisResult = getSpotStatusFromRedis(lotId);
        if (redisResult.isHit()) return redisResult;

        List<Integer> dbList = getSpotStatusFromDb(lotId);

        if (!redisResult.isError()) {
            saveSpotStatusToRedis(lotId, dbList);
        }

        return CacheResult.hit(dbList);
    }

    private List<Integer> getSpotStatusFromDb(Long lotId) {
        Set<Long> occupiedSpotIds = parkingOrderMapper.selectList(
                        Wrappers.<ParkingOrder>lambdaQuery()
                                .select(ParkingOrder::getSpotId)
                                .eq(ParkingOrder::getLotId, lotId)
                                .in(ParkingOrder::getStatus,
                                        OrderConstant.ORDER_STATUS_RESERVED,
                                        OrderConstant.ORDER_STATUS_IN_PROGRESS))
                .stream()
                .map(ParkingOrder::getSpotId)
                .collect(Collectors.toSet());

        List<ParkingSpot> spots = parkingSpotMapper.selectList(
                Wrappers.<ParkingSpot>lambdaQuery()
                        .eq(ParkingSpot::getLotId, lotId)
                        .orderByAsc(ParkingSpot::getSeq));

        return spots.stream()
                .map(spot -> occupiedSpotIds.contains(spot.getId()) ? 1 : 0)
                .toList();
    }

    private CacheResult<List<Integer>> getSpotStatusFromRedis(Long lotId) {
        try {
            String raw = stringRedisTemplate.opsForValue()
                    .get(RedisConstant.Parking.PARKING_SPOT_STATUS + lotId);
            if (raw == null) return CacheResult.miss();
            byte[] bitmap = raw.getBytes(StandardCharsets.ISO_8859_1);
            List<Integer> list = new ArrayList<>(bitmap.length * 8);
            for (int i = 0; i < bitmap.length * 8; i++) {
                boolean occupied = (bitmap[i / 8] & (1 << (7 - i % 8))) != 0;
                list.add(occupied ? 1 : 0);
            }
            return CacheResult.hit(list);
        } catch (Exception e) {
            log.warn("车位状态Bitmap查询失败: lotId={}", lotId, e);
            return CacheResult.error();
        }
    }

    private void saveSpotStatusToRedis(Long lotId, List<Integer> statusList) {
        try {
            int size = statusList.size();
            byte[] bitmap = new byte[(size + 7) / 8];
            for (int i = 0; i < size; i++) {
                if (statusList.get(i) == 1) {
                    bitmap[i / 8] |= (byte) (1 << (7 - i % 8));
                }
            }
            stringRedisTemplate.opsForValue().set(
                    RedisConstant.Parking.PARKING_SPOT_STATUS + lotId,
                    new String(bitmap, StandardCharsets.ISO_8859_1));
        } catch (Exception e) {
            log.warn("车位状态Bitmap写入失败: lotId={}", lotId, e);
        }
    }

    private CacheResult<List<SpotVO>> getSpotListFromRedis(String cacheKey) {
        try {
            String json = stringRedisTemplate.opsForValue().get(cacheKey);
            if (json == null) return CacheResult.miss();
            List<SpotVO> list = JSONUtil.toList(JSONUtil.parseArray(json), SpotVO.class);
            return CacheResult.hit(list);
        } catch (Exception e) {
            log.warn("车位列表Redis查询失败: cacheKey={}", cacheKey, e);
            return CacheResult.error();
        }
    }

    private void saveSpotListToRedis(String cacheKey, List<SpotVO> voList) {
        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    JSONUtil.toJsonStr(voList),
                    RedisConstant.Parking.PARKING_SPOT_LIST_TTL,
                    TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("车位列表Redis写入失败: cacheKey={}", cacheKey, e);
        }
    }
}
