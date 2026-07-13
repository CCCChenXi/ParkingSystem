package com.xigeandwillian.parkingsystem.common.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.xigeandwillian.parkingsystem.common.vo.parkingspot.SpotListVO;
import com.xigeandwillian.parkingsystem.common.result.CacheResult;
import com.xigeandwillian.parkingsystem.common.constant.CaffeineConstant;
import com.xigeandwillian.parkingsystem.common.constant.OrderConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.client.vo.parkinglot.ParkingLotCache;
import com.xigeandwillian.parkingsystem.client.vo.parkinglot.ParkingLotVO;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.entity.ParkingOrder;
import com.xigeandwillian.parkingsystem.common.entity.ParkingSpot;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingLotMapper;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingOrderMapper;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingSpotMapper;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingLotConverter;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingSpotConverter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
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
    private final ParkingLotMapper parkingLotMapper;
    private final ParkingLotConverter parkingLotConverter;
    private final ParkingSpotConverter parkingSpotConverter;
    private final StringRedisTemplate stringRedisTemplate;

    @Resource(name = "parkingSpotCache")
    private Cache<String, List<SpotListVO>> localCache;

    @Resource(name = "parkingSpotsCache")
    private Cache<String, List<SpotListVO>> parkingSpotsCache;

    public List<SpotListVO> getAllSpotByLotId(Long lotId) {
        String cacheKey = RedisConstant.Parking.PARKING_SPOT_LIST + lotId;

        List<SpotListVO> local = localCache.getIfPresent(cacheKey);
        if (local != null) return local;

        CacheResult<List<SpotListVO>> redisResult = getSpotListFromRedis(cacheKey);
        if (redisResult.isHit()) {
            localCache.put(cacheKey, redisResult.getData());
            return redisResult.getData();
        }

        List<ParkingSpot> spots = parkingSpotMapper.selectList(
                Wrappers.<ParkingSpot>lambdaQuery()
                        .eq(ParkingSpot::getLotId, lotId)
                        .orderByAsc(ParkingSpot::getSeq));
        List<SpotListVO> voList = spots.stream()
                .map(parkingSpotConverter::toListVO)
                .toList();

        localCache.put(cacheKey, voList);

        if (!redisResult.isError()) {
            saveSpotListToRedis(cacheKey, voList);
        }

        return voList;
    }

    public void invalidateLocalSpotList(String cacheKey) {
        localCache.invalidate(cacheKey);
        log.info("本地车位列表缓存已清除: cacheKey={}", cacheKey);
    }

    public List<SpotListVO> getParkingSpots(Long lotId) {
        String cacheKey = CaffeineConstant.PARKING_SPOTS_KEY_PREFIX + lotId;
        return parkingSpotsCache.get(cacheKey, key -> {
            List<SpotListVO> spots = getAllSpotByLotId(lotId);
            if (spots.isEmpty()) return spots;
            List<Integer> statusList = getSpotStatusList(lotId).getData();
            for (int i = 0; i < Math.min(statusList.size(), spots.size()); i++) {
                spots.get(i).setStatus(statusList.get(i));
            }
            return spots;
        });
    }

    public void invalidateParkingSpotsCache(String cacheKey) {
        parkingSpotsCache.invalidate(cacheKey);
        log.info("本地parkingSpots缓存已清除: cacheKey={}", cacheKey);
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
            byte[] bitmap = stringRedisTemplate.execute((RedisCallback<byte[]>) conn ->
                    conn.stringCommands().get(
                            (RedisConstant.Parking.PARKING_SPOT_STATUS + lotId).getBytes()));
            if (bitmap == null) return CacheResult.miss();
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
            stringRedisTemplate.execute((RedisCallback<Boolean>) conn -> {
                conn.stringCommands().set(
                        (RedisConstant.Parking.PARKING_SPOT_STATUS + lotId).getBytes(),
                        bitmap);
                return true;
            });
        } catch (Exception e) {
            log.warn("车位状态Bitmap写入失败: lotId={}", lotId, e);
        }
    }

    private CacheResult<List<SpotListVO>> getSpotListFromRedis(String cacheKey) {
        try {
            String json = stringRedisTemplate.opsForValue().get(cacheKey);
            if (json == null) return CacheResult.miss();
            List<SpotListVO> list = JSONUtil.toList(JSONUtil.parseArray(json), SpotListVO.class);
            return CacheResult.hit(list);
        } catch (Exception e) {
            log.warn("车位列表Redis查询失败: cacheKey={}", cacheKey, e);
            return CacheResult.error();
        }
    }

    private void saveSpotListToRedis(String cacheKey, List<SpotListVO> voList) {
        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    JSONUtil.toJsonStr(voList));
        } catch (Exception e) {
            log.warn("车位列表Redis写入失败: cacheKey={}", cacheKey, e);
        }
    }

    @PostConstruct
    public void initCache() {
        initParkingLotInfo();
        initParkingLotListAll();
        initParkingSpotList();
        initSpotStatus();
    }

    private void initParkingLotInfo() {
        try {
            List<ParkingLot> lots = parkingLotMapper.selectList(null);
            for (ParkingLot lot : lots) {
                ParkingLotCache cache = parkingLotConverter.toCache(lot);
                String key = RedisConstant.Parking.PARKING_LOT_INFO + lot.getId();
                stringRedisTemplate.opsForValue().set(
                        key, JSONUtil.toJsonStr(cache));
            }
            log.info("停车场信息缓存初始化完成, 共 {} 条", lots.size());
        } catch (Exception e) {
            log.error("停车场信息缓存初始化失败", e);
        }
    }

    private void initParkingLotListAll() {
        try {
            List<ParkingLot> lots = parkingLotMapper.selectList(null);
            List<ParkingLotVO> voList = parkingLotConverter.toVOList(lots);
            stringRedisTemplate.opsForValue().set(
                    RedisConstant.Parking.PARKING_LOT_LIST_ALL,
                    JSONUtil.toJsonStr(voList));
            log.info("停车场全量列表缓存初始化完成, 共 {} 条", voList.size());
        } catch (Exception e) {
            log.error("停车场全量列表缓存初始化失败", e);
        }
    }

    private void initParkingSpotList() {
        try {
            List<ParkingLot> lots = parkingLotMapper.selectList(null);
            for (ParkingLot lot : lots) {
                Long lotId = lot.getId();
                String key = RedisConstant.Parking.PARKING_SPOT_LIST + lotId;

                List<ParkingSpot> spots = parkingSpotMapper.selectList(
                        Wrappers.<ParkingSpot>lambdaQuery()
                                .eq(ParkingSpot::getLotId, lotId)
                                .orderByAsc(ParkingSpot::getSeq));
                if (spots.isEmpty()) continue;

                List<SpotListVO> voList = parkingSpotConverter.toListVOList(spots);

                stringRedisTemplate.opsForValue().set(
                        key, JSONUtil.toJsonStr(voList));
            }
            log.info("车位信息静态列表缓存初始化完成, 共 {} 个停车场", lots.size());
        } catch (Exception e) {
            log.error("车位信息静态列表缓存初始化失败", e);
        }
    }

    private void initSpotStatus() {
        try {
            List<ParkingLot> lots = parkingLotMapper.selectList(null);
            for (ParkingLot lot : lots) {
                Long lotId = lot.getId();
                String key = RedisConstant.Parking.PARKING_SPOT_STATUS + lotId;

                stringRedisTemplate.delete(key);

                List<ParkingSpot> spots = parkingSpotMapper.selectList(
                        Wrappers.<ParkingSpot>lambdaQuery()
                                .eq(ParkingSpot::getLotId, lotId)
                                .orderByAsc(ParkingSpot::getSeq));
                if (spots.isEmpty()) continue;

                Set<Long> occupied = parkingOrderMapper.selectList(
                                Wrappers.<ParkingOrder>lambdaQuery()
                                        .select(ParkingOrder::getSpotId)
                                        .eq(ParkingOrder::getLotId, lotId)
                                        .in(ParkingOrder::getStatus,
                                                OrderConstant.ORDER_STATUS_RESERVED,
                                                OrderConstant.ORDER_STATUS_IN_PROGRESS))
                        .stream().map(ParkingOrder::getSpotId)
                        .collect(Collectors.toSet());

                int size = spots.size();
                byte[] bitmap = new byte[(size + 7) / 8];
                for (int i = 0; i < size; i++) {
                    if (occupied.contains(spots.get(i).getId())) {
                        bitmap[i / 8] |= (byte) (1 << (7 - i % 8));
                    }
                }

                stringRedisTemplate.execute((RedisCallback<Boolean>) conn -> {
                    conn.stringCommands().set(key.getBytes(), bitmap);
                    return true;
                });
            }
            log.info("车位状态缓存初始化完成, 共 {} 个停车场", lots.size());
        } catch (Exception e) {
            log.error("车位状态缓存初始化失败", e);
        }
    }
}
