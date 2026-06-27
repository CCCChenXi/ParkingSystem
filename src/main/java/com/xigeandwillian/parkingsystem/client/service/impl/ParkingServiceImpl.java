package com.xigeandwillian.parkingsystem.client.service.impl;

import cn.hutool.json.JSONUtil;
import com.xigeandwillian.parkingsystem.client.mapper.ParkingMapper;
import com.xigeandwillian.parkingsystem.client.service.Service.ParkingService;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.entity.ParkingSpot;
import com.xigeandwillian.parkingsystem.common.result.Result;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static com.baomidou.mybatisplus.extension.ddl.DdlScriptErrorHandler.PrintlnLogErrorHandler.log;
import static com.xigeandwillian.parkingsystem.common.constant.RedisConstant.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParkingServiceImpl implements ParkingService {

    private final ParkingMapper parkingMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 获取附近停车场列表
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return
     */
    @Override
    public Result parkingList(BigDecimal longitude, BigDecimal latitude) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = null;
        try {
            //从缓存获取坐标范围内的停车场列表
            results = stringRedisTemplate
                    .opsForGeo()
                    .search(
                            PARKING_GEO,
                            //中心点
                            GeoReference.fromCoordinate(new Point(longitude.doubleValue(), latitude.doubleValue())),
                            //搜索形状
                            GeoShape.byRadius(new Distance(PARKING_DEFAULT_RADIUS, Metrics.KILOMETERS)),
                            //返回参数
                            RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                                    .includeCoordinates()//返回坐标
                                    .includeDistance()//返回距离
                                    .sortAscending()//排序
                                    .limit(PARKING_RETURN_NUMBER)//限制数量
                    );
        } catch (Exception e) {
            log.error("redis获取停车场列表失败!");
            throw new RuntimeException("附近无停车场");
        }


        //获取Geo元素中的id
        List<Long> ids = results.getContent().stream()
                .map(geoResult -> Long.valueOf(geoResult.getContent().getName()))
                .toList();

        List<ParkingLot> parkingLots = parkingMapper.selectByIds(ids);
        return Result.ok(parkingLots);

    }


    /**
     * 获取停车场信息
     *
     * @param id
     * @return
     */
    @Override
    public Result parkingInfo(Long id) {
        log.info("获取停车场信息:{}", id);
        String s = null;
        try {
            s = stringRedisTemplate.opsForValue().get(PARKING_INFO + id);
        } catch (Exception e) {
            log.error("redis服务器异常!");
        }
        if (s != null && s.isEmpty()) {
            throw new RuntimeException("停车场不存在");
        }
        ParkingLot parkingLot = parkingMapper.selectById(id);

        return Result.ok(parkingLot);
    }

    /**
     * 获取停车场车位信息
     *
     * @param id
     * @return
     */
    @Override
    public Result parkingSpots(Long id) {
        //List<ParkingSpot> parkingSpots = parkingMapper.selectSpotsByLotId(id);
        return Result.ok();
    }


}
