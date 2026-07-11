package com.xigeandwillian.parkingsystem.common.task;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.client.vo.parkinglot.ParkingLotCache;
import com.xigeandwillian.parkingsystem.client.vo.parkinglot.ParkingLotVO;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingLotMapper;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingLotConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class CacheConsistencyTask {

    private final ParkingLotMapper parkingLotMapper;
    private final ParkingLotConverter parkingLotConverter;
    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(fixedRate = 600000)
    public void rebuildAvailableSpots() {
        try {
            List<ParkingLot> lots = parkingLotMapper.selectList(null);
            Map<String, String> spotsMap = lots.stream()
                    .collect(Collectors.toMap(
                            lot -> lot.getId().toString(),
                            lot -> lot.getAvailableSpots().toString()));
            stringRedisTemplate.opsForHash().putAll(
                    RedisConstant.Parking.PARKING_AVAILABLE_SPOTS, spotsMap);
            log.debug("可用车位缓存重建完成, 共 {} 个停车场", spotsMap.size());
        } catch (Exception e) {
            log.warn("可用车位缓存重建失败", e);
        }
    }

    @Scheduled(fixedRate = 600000)
    public void rebuildParkingLotListAll() {
        try {
            String json = stringRedisTemplate.opsForValue()
                    .get(RedisConstant.Parking.PARKING_LOT_LIST_ALL);
            if (json != null) {
                List<ParkingLotVO> existing = JSONUtil.toList(
                        JSONUtil.parseArray(json), ParkingLotVO.class);
                int dbCount = parkingLotMapper.selectCount(Wrappers.emptyWrapper()).intValue();
                if (existing.size() == dbCount) {
                    return;
                }
                log.info("parking:lot:list:all 数量不一致({} vs {}), 触发重建",
                        existing.size(), dbCount);
            }
            List<ParkingLot> lots = parkingLotMapper.selectList(null);
            List<ParkingLotVO> voList = parkingLotConverter.toVOList(lots);
            stringRedisTemplate.opsForValue().set(
                    RedisConstant.Parking.PARKING_LOT_LIST_ALL,
                    JSONUtil.toJsonStr(voList));
            log.debug("停车场全量列表缓存重建完成, 共 {} 条", voList.size());
        } catch (Exception e) {
            log.warn("停车场全量列表缓存重建失败", e);
        }
    }

    @Scheduled(fixedRate = 600000)
    public void rebuildParkingLotInfo() {
        try {
            List<ParkingLot> lots = parkingLotMapper.selectList(null);
            int rebuilt = 0;
            for (ParkingLot lot : lots) {
                String key = RedisConstant.Parking.PARKING_LOT_INFO + lot.getId();
                Boolean exists = stringRedisTemplate.hasKey(key);
                if (Boolean.FALSE.equals(exists)) {
                    ParkingLotCache cache = parkingLotConverter.toCache(lot);
                    stringRedisTemplate.opsForValue().set(
                            key, JSONUtil.toJsonStr(cache));
                    rebuilt++;
                }
            }
            if (rebuilt > 0) {
                log.info("停车场信息缓存重建完成, 重建 {} 条", rebuilt);
            }
        } catch (Exception e) {
            log.warn("停车场信息缓存重建失败", e);
        }
    }
}
