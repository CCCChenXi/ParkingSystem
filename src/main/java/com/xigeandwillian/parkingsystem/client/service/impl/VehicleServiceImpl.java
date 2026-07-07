package com.xigeandwillian.parkingsystem.client.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.xigeandwillian.parkingsystem.client.dto.user.VehicleDTO;
import com.xigeandwillian.parkingsystem.client.service.service.VehicleService;
import com.xigeandwillian.parkingsystem.common.constant.CaffeineConstant;
import com.xigeandwillian.parkingsystem.common.entity.Vehicle;
import com.xigeandwillian.parkingsystem.common.mapper.VehicleMapper;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleMapper vehicleMapper;

    @Resource(name = "vehicleCache")
    private Cache<String, List<Vehicle>> vehicleCache;

    /**
     * 获取当前登录用户的车辆信息
     * 优先从本地缓存读取，未命中时查数据库并回填缓存
     */
    @Override
    public Result vehiclesInfo() {
        long userId = UserHolder.get();
        String cacheKey = CaffeineConstant.VEHICLE_KEY + userId;
        List<Vehicle> vehicles = vehicleCache.get(cacheKey, k -> {
            log.info("车辆缓存未命中，查询数据库 userId: {}", userId);
            return vehicleMapper.selectList(Wrappers.<Vehicle>lambdaQuery()
                    .eq(Vehicle::getUserId, userId)
            );
        });
        return Result.ok(vehicles);
    }

    /**
     * 添加车辆
     *
     * @param vehicleDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addVehicle(VehicleDTO vehicleDTO) {
        Vehicle vehicle = new Vehicle();
        BeanUtil.copyProperties(vehicleDTO, vehicle);
        vehicle.setUserId(UserHolder.get());
        vehicleMapper.insert(vehicle);
        vehicleCache.invalidate(CaffeineConstant.VEHICLE_KEY + vehicle.getUserId());
        log.info("新增车辆成功，已清除用户 {} 的车辆缓存", vehicle.getUserId());
        return Result.ok();
    }

    /**
     * 删除车辆
     *
     * @param id
     * @return
     */
    @Override
    public Result deleteVehicle(Long id) {
        vehicleMapper.deleteById(id);
        vehicleCache.invalidate(CaffeineConstant.VEHICLE_KEY + UserHolder.get());
        return Result.ok();
    }

    /**
     * 更新车辆
     *
     * @param vehicleDTO
     * @param id
     * @return
     */
    @Override
    public Result updateVehicle(VehicleDTO vehicleDTO, Long id) {
        Vehicle vehicle = new Vehicle();
        BeanUtil.copyProperties(vehicleDTO, vehicle);
        vehicle.setId(id);
        vehicleMapper.updateById(vehicle);
        vehicleCache.invalidate(CaffeineConstant.VEHICLE_KEY + UserHolder.get());
        return Result.ok();
    }


    public void setVehicleCache(Cache<String, List<Vehicle>> vehicleCache) {
        this.vehicleCache = vehicleCache;
    }
}
