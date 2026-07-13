package com.xigeandwillian.parkingsystem.client.service.impl;


import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingLotConverter;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingLotMapper;
import com.xigeandwillian.parkingsystem.client.service.ParkingService;
import com.xigeandwillian.parkingsystem.client.vo.parkinglot.ParkingLotCache;
import com.xigeandwillian.parkingsystem.client.vo.parkinglot.ParkingLotVO;
import com.xigeandwillian.parkingsystem.common.vo.parkingspot.SpotListVO;
import com.xigeandwillian.parkingsystem.common.result.CacheResult;
import com.xigeandwillian.parkingsystem.common.constant.CaffeineConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.service.impl.ParkingDataProvider;
import com.xigeandwillian.parkingsystem.common.utils.DistanceUtil;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;
import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.xigeandwillian.parkingsystem.common.constant.DistanceConstant.KILOMETER;
import static com.xigeandwillian.parkingsystem.common.constant.RedisConstant.Parking.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParkingServiceImpl implements ParkingService {

    private final ParkingLotConverter parkingLotConverter;
    private final ParkingLotMapper parkingLotMapper;
    private final ParkingDataProvider parkingDataProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Resource(name = "parkingLotCache")
    private Cache<Long, ParkingLotVO> localCache;
    @Resource(name = "parkingLotListCache")
    private Cache<String, List<ParkingLotVO>> parkingLotListCache;
    /*Caffeine空对象缓存*/
    private static final ParkingLotVO NULL_MARKER = new ParkingLotVO();




    /**
     * ！！！已弃用！！！改为前端获取全部停车场列表，然后前端处理距离
     * 获取附近停车场列表
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return
     */
    @Override
    public Result parkingList(BigDecimal longitude, BigDecimal latitude, long radius) {
        log.info("查询附近停车场: longitude={}, latitude={}, radius={}", longitude, latitude, radius);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = null;
        // 1.Geo 搜索附近停车场
        try {
            results = stringRedisTemplate
                    .opsForGeo()
                    .search(
                            PARKING_GEO,
                            GeoReference.fromCoordinate(new Point(longitude.doubleValue(), latitude.doubleValue())),
                            GeoShape.byRadius(new Distance(radius, Metrics.METERS)),
                            RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                                    .includeCoordinates()
                                    .includeDistance()
                                    .sortAscending()
                                    .limit(PARKING_RETURN_NUMBER)
                    );
            log.info("GEO搜索到{}个停车场", results.getContent().size());
        } catch (Exception e) {
            log.error("获取停车场列表失败", e);
            // 降级：经纬度 1°≈111km，计算 bounding-box 查 DB
            // 50km ≈ 0.45°
            double lat = latitude.doubleValue();
            double lon = longitude.doubleValue();
            double latDiff = (radius / 1000.0) / 111.0;
            double lonDiff = (radius / 1000.0) / (111.0 * Math.cos(Math.toRadians(lat)));
            QueryWrapper<ParkingLot> wrapper = new QueryWrapper<ParkingLot>()
                    .between("latitude", lat - latDiff, lat + latDiff)
                    .between("longitude", lon - lonDiff, lon + lonDiff)
                    .last("LIMIT " + PARKING_DEFAULT_NUMBER);
            List<ParkingLot> parkingLots = parkingLotMapper.selectList(wrapper);
            return Result.ok(parkingLots);
        }

        List<Long> ids = results.getContent().stream()
                .map(geoResult -> Long.valueOf(geoResult.getContent().getName()))
                .toList();

        if (ids.isEmpty()) {
            log.warn("附近无停车场: longitude={}, latitude={}", longitude, latitude);
            throw new BusinessException(ResultConstant.BAD_REQUEST, "附近无停车场");
        }

        List<ParkingLot> parkingLots = new ArrayList<>();
        List<ParkingLotVO> parkingLotVOs;

        // 2.批量查缓存：Hash（静态）+ string（可用车位）
        //    全部未命中 → DB 查全部并回填
        //    部分未命中 → 只查缺失的 ids 并回填
        //    全部命中   → 跳过
        try {
            List<String> keys = ids.stream()
                    .map(id -> PARKING_LOT_INFO + id)
                    .toList();

            List<String> jsonList = stringRedisTemplate.opsForValue().multiGet(keys);
            List<Object> AvaList = stringRedisTemplate.opsForHash().multiGet(PARKING_AVAILABLE_SPOTS, new ArrayList<>(ids));

            long missCount = jsonList.stream().filter(Objects::isNull).count();

            if (missCount == ids.size()) {
                log.info("缓存全部未命中，查DB回填: ids={}", ids);
                List<ParkingLot> lotList = parkingLotMapper.selectByIds(ids);

                // DB 返回顺序不一定与 ids 一致，按 id 建立映射
                log.info("DB查到{}条停车场记录", lotList.size());
                Map<Long, ParkingLot> lotMap = new HashMap<>();
                for (ParkingLot lot : lotList) {
                    lotMap.put(lot.getId(), lot);
                }

                Map<String, String> cacheMap = new HashMap<>();
                Map<String, String> spotsMap = new HashMap<>();

                for (int i = 0; i < ids.size(); i++) {
                    Long id = ids.get(i);
                    ParkingLot lot = lotMap.get(id);
                    if (lot == null) continue;

                    ParkingLotCache cache = parkingLotConverter.toCache(lot);
                    String json = JSONUtil.toJsonStr(cache);

                    jsonList.set(i, json);
                    AvaList.set(i, lot.getAvailableSpots());

                    cacheMap.put(PARKING_LOT_INFO + id, json);
                    spotsMap.put(String.valueOf(id), String.valueOf(lot.getAvailableSpots()));
                }

                stringRedisTemplate.opsForValue().multiSet(cacheMap);
                stringRedisTemplate.opsForHash().putAll(PARKING_AVAILABLE_SPOTS, spotsMap);

            } else if (missCount > 0) {
                log.info("缓存部分未命中，缺失{}条", missCount);
                List<Integer> missIndices = new ArrayList<>();
                for (int i = 0; i < jsonList.size(); i++) {
                    if (jsonList.get(i) == null) {
                        missIndices.add(i);
                    }
                }
                List<Long> missIds = missIndices.stream().map(ids::get).toList();
                log.info("缺失的停车场ID: {}", missIds);

                List<ParkingLot> missLots = parkingLotMapper.selectByIds(missIds);

                Map<Long, ParkingLot> missLotMap = new HashMap<>();
                for (ParkingLot lot : missLots) {
                    missLotMap.put(lot.getId(), lot);
                }

                Map<String, String> cacheMap = new HashMap<>();
                Map<String, String> spotsMap = new HashMap<>();

                for (int index : missIndices) {
                    Long id = ids.get(index);
                    ParkingLot lot = missLotMap.get(id);
                    if (lot == null) continue;

                    ParkingLotCache cache = parkingLotConverter.toCache(lot);
                    String json = JSONUtil.toJsonStr(cache);

                    jsonList.set(index, json);
                    AvaList.set(index, lot.getAvailableSpots());

                    cacheMap.put(PARKING_LOT_INFO + id, json);
                    spotsMap.put(String.valueOf(id), String.valueOf(lot.getAvailableSpots()));
                }

                stringRedisTemplate.opsForValue().multiSet(cacheMap);
                stringRedisTemplate.opsForHash().putAll(PARKING_AVAILABLE_SPOTS, spotsMap);
            }

            if (missCount == 0) {
                log.info("缓存全部命中: ids={}", ids);
            }

            // 3.合并可用车位 → 转为 VO
            for (int i = 0; i < jsonList.size(); i++) {
                ParkingLot parkingLot = JSONUtil.toBean((jsonList.get(i)), ParkingLot.class);
                parkingLot.setAvailableSpots((Integer) AvaList.get(i));
                parkingLots.add(parkingLot);
            }
            parkingLotVOs = parkingLotConverter.toVOList(parkingLots);

        } catch (Exception e) {
            log.warn("从redis获取停车场详细信息失败，回查数据库");
            // DB 降级：查询结果无序，需计算距离并重新排序
            List<ParkingLot> ParkingLots = parkingLotMapper.selectByIds(ids);
            parkingLotVOs = parkingLotConverter.toVOList(ParkingLots);
            parkingLotVOs.forEach(vo -> {
                double dis = DistanceUtil.haversine(
                        latitude.doubleValue(), longitude.doubleValue(),
                        vo.getLatitude().doubleValue(), vo.getLongitude().doubleValue());
                vo.setDistance(String.format("%.2f km", dis));
            });
            parkingLotVOs.sort(Comparator.comparingDouble(vo ->
                    Double.parseDouble(vo.getDistance().replace(KILOMETER, ""))));
        }

        log.info("返回附近停车场{}条", parkingLotVOs.size());

        // 4.覆盖 Geo 返回的距离到 VO
        for (int i = 0; i < parkingLotVOs.size() && i < results.getContent().size(); i++) {
            ParkingLotVO parkingLotVO = parkingLotVOs.get(i);
            Distance distance = results.getContent().get(i).getDistance();
            double dis = distance.getValue() / 1000.0;
            parkingLotVO.setDistance(String.format("%.2f km", dis));
        }
        return Result.ok(parkingLotVOs);

    }


    /**
     * 获取停车场信息(本地缓存防高并发)
     *
     * @param id
     * @return
     */
    @Override
    public Result parkingInfo(Long id) {
        log.info("获取停车场信息:{}", id);

        // 0.Caffeine 本地缓存兜底
        ParkingLotVO cached = localCache.getIfPresent(id);
        if (cached != null) {
            if (cached == NULL_MARKER) {
                throw new BusinessException(ResultConstant.BAD_REQUEST, "停车场不存在");
            }
            return Result.ok(cached);
        }

        // 1.Redis → Cache → VO
        try {
            String json = stringRedisTemplate.opsForValue().get(PARKING_LOT_INFO + id);
            if (json != null && !json.isEmpty()) {
                ParkingLotCache cache = JSONUtil.toBean(json, ParkingLotCache.class);
                Object obj = stringRedisTemplate.opsForHash().get(PARKING_AVAILABLE_SPOTS, String.valueOf(id));
                Integer avaSpots = obj != null ? Integer.valueOf(obj.toString()) : null;
                ParkingLotVO vo = parkingLotConverter.cacheToVO(cache);
                vo.setAvailableSpots(avaSpots);
                localCache.put(id, vo);
                return Result.ok(vo);
            }
        } catch (Exception e) {
            log.error("获取停车场信息失败", e);
        }

        // 2.DB 降级（缓存穿透 / Redis 不可用）
        ParkingLot parkingLot = parkingLotMapper.selectById(id);
        if (parkingLot == null) {
            localCache.put(id, NULL_MARKER);
            throw new BusinessException(ResultConstant.BAD_REQUEST, "停车场不存在");
        }
        ParkingLotVO vo = parkingLotConverter.toVO(parkingLot);
        vo.setAvailableSpots(parkingLot.getAvailableSpots());
        localCache.put(id, vo);
        return Result.ok(vo);
    }

    @Override
    public Result parkingSpots(Long id) {
        log.info("获取停车场车位信息: lotId={}", id);
        List<SpotListVO> spots = parkingDataProvider.getParkingSpots(id);
        if (spots.isEmpty()) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "该停车场暂无车位");
        }
        return Result.ok(spots);
    }

    @Override
    public Result listAll() {
        log.info("获取所有停车场列表");

        List<ParkingLotVO> local = parkingLotListCache.getIfPresent(CaffeineConstant.PARKING_LOT_LIST_ALL_KEY);
        if (local != null) {
            log.info("停车场全量列表命中本地缓存");
            return Result.ok(local);
        }

        CacheResult<List<ParkingLotVO>> redisResult = getLotListFromRedis();
        if (redisResult.isHit()) {
            log.info("停车场全量列表命中Redis缓存");
            parkingLotListCache.put(CaffeineConstant.PARKING_LOT_LIST_ALL_KEY, redisResult.getData());
            return Result.ok(redisResult.getData());
        }

        log.info("停车场全量列表缓存未命中，查询数据库");
        List<ParkingLotVO> dbList = getLotListFromDb();
        parkingLotListCache.put(CaffeineConstant.PARKING_LOT_LIST_ALL_KEY, dbList);
        if (!redisResult.isError()) {
            saveLotListToRedis(dbList);
        }

        return Result.ok(dbList);
    }

    private CacheResult<List<ParkingLotVO>> getLotListFromRedis() {
        try {
            String json = stringRedisTemplate.opsForValue()
                    .get(PARKING_LOT_LIST_ALL);
            if (json == null) return CacheResult.miss();
            List<ParkingLotVO> list = JSONUtil.toList(JSONUtil.parseArray(json), ParkingLotVO.class);
            return CacheResult.hit(list);
        } catch (Exception e) {
            log.warn("停车场全量列表Redis查询失败", e);
            return CacheResult.error();
        }
    }

    private void saveLotListToRedis(List<ParkingLotVO> voList) {
        try {
            stringRedisTemplate.opsForValue().set(
                    PARKING_LOT_LIST_ALL,
                    JSONUtil.toJsonStr(voList));
            log.info("停车场全量列表已写入Redis，共{}条", voList.size());
        } catch (Exception e) {
            log.warn("停车场全量列表Redis写入失败", e);
        }
    }

    private List<ParkingLotVO> getLotListFromDb() {
        List<ParkingLot> lots = parkingLotMapper.selectList(null);
        List<ParkingLotVO> voList = lots.stream().map(lot -> {
            ParkingLotVO vo = parkingLotConverter.toVO(lot);
            return vo;
        }).toList();
        log.info("从数据库查询到{}个停车场", voList.size());
        return voList;
    }
}
