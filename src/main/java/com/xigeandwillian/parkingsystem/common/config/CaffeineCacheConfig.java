package com.xigeandwillian.parkingsystem.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xigeandwillian.parkingsystem.client.vo.coupon.CouponAvailableVO;
import com.xigeandwillian.parkingsystem.client.vo.coupon.CouponDetailVO;
import com.xigeandwillian.parkingsystem.client.vo.parkinglot.ParkingLotVO;
import com.xigeandwillian.parkingsystem.common.vo.parkingspot.SpotListVO;
import com.xigeandwillian.parkingsystem.client.vo.wallet.WalletVO;
import com.xigeandwillian.parkingsystem.common.constant.CaffeineConstant;
import com.xigeandwillian.parkingsystem.common.entity.Vehicle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineCacheConfig {

    @Bean("couponAvailableCache")
    public Cache<String, List<CouponAvailableVO>> couponAvailableCache() {
        return Caffeine.newBuilder()
                .maximumSize(CaffeineConstant.COUPON_MAXIMUM_SIZE)
                .expireAfterWrite(CaffeineConstant.COUPON_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Bean("parkingLotCache")
    public Cache<Long, ParkingLotVO> parkingLotCache() {
        return Caffeine.newBuilder()
                .maximumSize(CaffeineConstant.PARKING_MAXIMUM_SIZE)
                .expireAfterWrite(CaffeineConstant.PARKING_EXPIRE_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    @Bean("vehicleCache")
    public Cache<String, List<Vehicle>> vehicleCache() {
        return Caffeine.newBuilder()
                .maximumSize(CaffeineConstant.VEHICLE_MAXIMUM_SIZE)
                .expireAfterWrite(CaffeineConstant.VEHICLE_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Bean("couponDetailCache")
    public Cache<String, Map<Long, CouponDetailVO>> couponDetailCache() {
        return Caffeine.newBuilder()
                .maximumSize(CaffeineConstant.COUPON_DETAIL_MAXIMUM_SIZE)
                .expireAfterWrite(CaffeineConstant.COUPON_DETAIL_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Bean("couponStaticCache")
    public Cache<String, CouponDetailVO> couponStaticCache() {
        return Caffeine.newBuilder()
                .maximumSize(CaffeineConstant.COUPON_MAXIMUM_SIZE)
                .expireAfterWrite(CaffeineConstant.COUPON_DETAIL_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Bean("parkingSpotCache")
    public Cache<String, List<SpotListVO>> parkingSpotCache() {
        return Caffeine.newBuilder()
                .maximumSize(CaffeineConstant.PARKING_SPOT_MAXIMUM_SIZE)
                .expireAfterWrite(CaffeineConstant.PARKING_SPOT_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Bean("balanceCache")
    public Cache<String, WalletVO> balanceCache() {
        return Caffeine.newBuilder()
                .maximumSize(CaffeineConstant.WALLET_MAXIMUM_SIZE)
                .expireAfterWrite(CaffeineConstant.WALLET_EXPIRE_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    @Bean("parkingLotListCache")
    public Cache<String, List<ParkingLotVO>> parkingLotListCache() {
        return Caffeine.newBuilder()
                .maximumSize(CaffeineConstant.PARKING_LOT_LIST_ALL_MAXIMUM_SIZE)
                .expireAfterWrite(CaffeineConstant.PARKING_LOT_LIST_ALL_EXPIRE_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    @Bean("parkingSpotsCache")
    public Cache<String, List<SpotListVO>> parkingSpotsCache() {
        return Caffeine.newBuilder()
                .maximumSize(CaffeineConstant.PARKING_SPOTS_MAXIMUM_SIZE)
                .expireAfterWrite(CaffeineConstant.PARKING_SPOTS_EXPIRE_SECONDS, TimeUnit.SECONDS)
                .build();
    }

}
