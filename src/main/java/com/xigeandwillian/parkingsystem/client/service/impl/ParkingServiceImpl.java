package com.xigeandwillian.parkingsystem.client.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingLotMapper;
import com.xigeandwillian.parkingsystem.client.service.service.ParkingService;
import com.xigeandwillian.parkingsystem.client.vo.parkingLot.ParkingLotCache;
import com.xigeandwillian.parkingsystem.client.vo.parkingLot.ParkingLotVO;
import com.xigeandwillian.parkingsystem.client.vo.parkingLot.SpotVO;
import com.xigeandwillian.parkingsystem.common.cache.CacheResult;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.result.Result;
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

import static com.xigeandwillian.parkingsystem.common.constant.DistanceConstant.KILOMETER;
import static com.xigeandwillian.parkingsystem.common.constant.RedisConstant.Parking.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParkingServiceImpl implements ParkingService {

    private final ParkingLotMapper parkingLotMapper;
    private final ParkingDataProvider parkingDataProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Resource(name = "parkingLotCache")
    private Cache<Long, ParkingLotVO> localCache;
    /*Caffeine空对象缓存*/
    private static final ParkingLotVO NULL_MARKER = new ParkingLotVO();

    /**
     * 获取附近停车场列表
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return
     */
    @Override
    public Result parkingList(BigDecimal longitude, BigDecimal latitude, long radius) {

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
                    .map(id -> PARKING_INFO + id)
                    .toList();

            List<String> jsonList = stringRedisTemplate.opsForValue().multiGet(keys);
            List<Object> AvaList = stringRedisTemplate.opsForHash().multiGet(PARKING_AVAILABLE_SPOTS, new ArrayList<>(ids));

            long missCount = jsonList.stream().filter(Objects::isNull).count();

            if (missCount == ids.size()) {
                List<ParkingLot> lotList = parkingLotMapper.selectByIds(ids);

                // DB 返回顺序不一定与 ids 一致，按 id 建立映射
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

                    ParkingLotCache cache = BeanUtil.copyProperties(lot, ParkingLotCache.class);
                    String json = JSONUtil.toJsonStr(cache);

                    jsonList.set(i, json);
                    AvaList.set(i, lot.getAvailableSpots());

                    cacheMap.put(PARKING_INFO + id, json);
                    spotsMap.put(String.valueOf(id), String.valueOf(lot.getAvailableSpots()));
                }

                stringRedisTemplate.opsForValue().multiSet(cacheMap);
                stringRedisTemplate.opsForHash().putAll(PARKING_AVAILABLE_SPOTS, spotsMap);

            } else if (missCount > 0) {
                List<Integer> missIndices = new ArrayList<>();
                for (int i = 0; i < jsonList.size(); i++) {
                    if (jsonList.get(i) == null) {
                        missIndices.add(i);
                    }
                }
                List<Long> missIds = missIndices.stream().map(ids::get).toList();

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

                    ParkingLotCache cache = BeanUtil.copyProperties(lot, ParkingLotCache.class);
                    String json = JSONUtil.toJsonStr(cache);

                    jsonList.set(index, json);
                    AvaList.set(index, lot.getAvailableSpots());

                    cacheMap.put(PARKING_INFO + id, json);
                    spotsMap.put(String.valueOf(id), String.valueOf(lot.getAvailableSpots()));
                }

                stringRedisTemplate.opsForValue().multiSet(cacheMap);
                stringRedisTemplate.opsForHash().putAll(PARKING_AVAILABLE_SPOTS, spotsMap);
            }

            // 3.合并可用车位 → 转为 VO
            for (int i = 0; i < jsonList.size(); i++) {
                ParkingLot parkingLot = JSONUtil.toBean((jsonList.get(i)), ParkingLot.class);
                parkingLot.setAvailableSpots((Integer) AvaList.get(i));
                parkingLots.add(parkingLot);
            }
            parkingLotVOs = BeanUtil.copyToList(parkingLots, ParkingLotVO.class);

        } catch (Exception e) {
            log.info("从redis获取停车场详细信息失败，回查数据库");
            // DB 降级：查询结果无序，需计算距离并重新排序
            List<ParkingLot> ParkingLots = parkingLotMapper.selectByIds(ids);
            parkingLotVOs = BeanUtil.copyToList(ParkingLots, ParkingLotVO.class);
            parkingLotVOs.forEach(vo -> {
                double dis = DistanceUtil.haversine(
                        latitude.doubleValue(), longitude.doubleValue(),
                        vo.getLatitude().doubleValue(), vo.getLongitude().doubleValue());
                vo.setDistance(String.format("%.2f km", dis));
            });
            parkingLotVOs.sort(Comparator.comparingDouble(vo ->
                    Double.parseDouble(vo.getDistance().replace(KILOMETER, ""))));
        }

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

        // 0.Caffeine 本地缓存兜底（防恶意请求穿透到 DB）
        ParkingLotVO cached = localCache.getIfPresent(id);
        if (cached != null) {
            if (cached == NULL_MARKER) {
                throw new BusinessException(ResultConstant.BAD_REQUEST, "停车场不存在");
            }
            return Result.ok(cached);
        }

        // 1.Redis → Cache → VO
        try {
            String json = stringRedisTemplate.opsForValue().get(PARKING_INFO + id);
            if (json != null && !json.isEmpty()) {
                ParkingLotCache cache = JSONUtil.toBean(json, ParkingLotCache.class);
                Object obj = stringRedisTemplate.opsForHash().get(PARKING_AVAILABLE_SPOTS, String.valueOf(id));
                Integer avaSpots = obj != null ? Integer.valueOf(obj.toString()) : null;
                ParkingLotVO vo = BeanUtil.copyProperties(cache, ParkingLotVO.class);
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
        ParkingLotVO vo = BeanUtil.copyProperties(parkingLot, ParkingLotVO.class);
        vo.setAvailableSpots(parkingLot.getAvailableSpots());
        localCache.put(id, vo);
        return Result.ok(vo);
    }

    @Override
    public Result parkingSpots(Long id) {
        log.info("获取停车场车位信息: lotId={}", id);
        List<SpotVO> spots = parkingDataProvider.getAllSpotByLotId(id);
        if (spots.isEmpty()) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "该停车场暂无车位");
        }
        CacheResult<List<Integer>> statusResult = parkingDataProvider.getSpotStatusList(id);
        if (statusResult.isHit()) {
            List<Integer> statusList = statusResult.getData();
            for (int i = 0; i < Math.min(statusList.size(), spots.size()); i++) {
                spots.get(i).setStatus(statusList.get(i));
            }
        }
        return Result.ok(spots);
    }
}
