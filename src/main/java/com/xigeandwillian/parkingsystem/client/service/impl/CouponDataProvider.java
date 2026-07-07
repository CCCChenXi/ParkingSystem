package com.xigeandwillian.parkingsystem.client.service.impl;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.xigeandwillian.parkingsystem.client.mq.CacheInvalidateProducer;
import com.xigeandwillian.parkingsystem.client.vo.coupon.CouponAvailableVO;
import com.xigeandwillian.parkingsystem.client.vo.coupon.CouponDetailVO;
import com.xigeandwillian.parkingsystem.common.cache.CacheResult;
import com.xigeandwillian.parkingsystem.common.constant.CaffeineConstant;
import com.xigeandwillian.parkingsystem.common.constant.CouponConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.entity.Coupon;
import com.xigeandwillian.parkingsystem.common.entity.UserCoupon;
import com.xigeandwillian.parkingsystem.common.mapper.CouponMapper;
import com.xigeandwillian.parkingsystem.common.mapper.UserCouponMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class CouponDataProvider {

    private final CouponMapper couponMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserCouponMapper userCouponMapper;
    private final CacheInvalidateProducer cacheInvalidateProducer;
    private final RedissonClient redissonClient;

    @Resource(name = "couponAvailableCache")
    private Cache<String, List<CouponAvailableVO>> localCache;

    @Resource(name = "couponDetailCache")
    private Cache<String, Map<Long, CouponDetailVO>> detailLocalCache;

    @Resource(name = "couponStaticCache")
    private Cache<String, CouponDetailVO> staticLocalCache;

    private static final String LOCAL_KEY = CaffeineConstant.COUPON_AVAILABLE_KEY;
    private static final String REDIS_KEY = RedisConstant.Coupon.AVAILABLE_KEY;

    private static final String DETAIL_MAP_KEY = RedisConstant.Coupon.DETAIL_KEY;

    private static final String AVAILABLE_LOCK_KEY = "coupon:rebuild:lock:available";
    private static final String DETAIL_LOCK_KEY = "coupon:rebuild:lock:detail";

    @PostConstruct
    public void initSecKillStock() {
        try {
            List<Coupon> seckillCoupons = couponMapper.selectList(
                    Wrappers.<Coupon>lambdaQuery()
                            .eq(Coupon::getType, CouponConstant.COUPON_TYPE_SECKILL)
                            .le(Coupon::getStartTime, LocalDateTime.now())
                            .ge(Coupon::getEndTime, LocalDateTime.now()));
            for (Coupon c : seckillCoupons) {
                String key = RedisConstant.Coupon.SECKILL_STOCK + c.getId();
                stringRedisTemplate.opsForValue().setIfAbsent(
                        key, c.getRemainStock().toString(),
                        30, TimeUnit.DAYS);
            }
            log.info("秒杀库存初始化完成, 共 {} 条", seckillCoupons.size());
        } catch (Exception e) {
            log.error("秒杀库存初始化失败", e);
        }
    }

    public void initSecKillStock(Long couponId, Integer stock) {
        try {
            stringRedisTemplate.opsForValue().setIfAbsent(
                    RedisConstant.Coupon.SECKILL_STOCK + couponId,
                    stock.toString(),
                    30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("秒杀库存Redis初始化失败: couponId={}", couponId, e);
        }
    }

    public void resetSecKillStock(Long couponId, Integer stock) {
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisConstant.Coupon.SECKILL_STOCK + couponId,
                    stock.toString(),
                    30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("秒杀库存Redis更新失败: couponId={}", couponId, e);
        }
    }

    public List<CouponAvailableVO> getClaimableCoupons() {
        List<CouponAvailableVO> local = getFromLocalCache();
        if (local != null) {
            return local;
        }

        CacheResult<List<CouponAvailableVO>> redisResult = getFromRedis();
        if (redisResult.isHit()) {
            List<CouponAvailableVO> data = redisResult.getData();
            rebuildLocalCache(data);
            return data;
        }

        RLock lock = redissonClient.getLock(AVAILABLE_LOCK_KEY);
        try {
            if (lock.tryLock(1, 3, TimeUnit.SECONDS)) {
                try {
                    List<CouponAvailableVO> doubleCheck = getFromLocalCache();
                    if (doubleCheck != null) return doubleCheck;

                    List<CouponAvailableVO> dbData = loadFromDatabase();
                    rebuildLocalCache(dbData);
                    if (!redisResult.isError()) {
                        rebuildRedisCache(dbData);
                    }
                    return dbData;
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<CouponAvailableVO> dbData = loadFromDatabase();
        rebuildLocalCache(dbData);
        return dbData;
    }

    public CouponDetailVO getDetailById(Long id) {
        Map<Long, CouponDetailVO> map = detailLocalCache.getIfPresent(DETAIL_MAP_KEY);
        if (map != null) {
            return map.get(id);
        }

        CacheResult<Map<Long, CouponDetailVO>> redisResult = getDetailFromRedis();
        if (redisResult.isHit()) {
            map = redisResult.getData();
            detailLocalCache.put(DETAIL_MAP_KEY, map);
            return map.get(id);
        }

        RLock lock = redissonClient.getLock(DETAIL_LOCK_KEY);
        try {
            if (lock.tryLock(1, 3, TimeUnit.SECONDS)) {
                try {
                    Map<Long, CouponDetailVO> doubleCheck = detailLocalCache.getIfPresent(DETAIL_MAP_KEY);
                    if (doubleCheck != null) return doubleCheck.get(id);

                    map = buildDetailMap();
                    detailLocalCache.put(DETAIL_MAP_KEY, map);
                    if (!redisResult.isError()) {
                        rebuildDetailRedisCache(map);
                    }
                    return map.get(id);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        map = buildDetailMap();
        detailLocalCache.put(DETAIL_MAP_KEY, map);
        return map.get(id);
    }

    public void fillRemainStock(CouponDetailVO vo) {
        if (vo.getType() == CouponConstant.COUPON_TYPE_NORMAL) {
            Coupon coupon = couponMapper.selectById(vo.getId());
            if (coupon != null) {
                vo.setRemainStock(coupon.getRemainStock());
            }
        } else {
            try {
                String stock = stringRedisTemplate.opsForValue().get(RedisConstant.Coupon.SECKILL_STOCK + vo.getId());
                if (stock != null) {
                    vo.setRemainStock(Integer.parseInt(stock));
                } else {
                    Coupon coupon = couponMapper.selectById(vo.getId());
                    if (coupon != null) {
                        vo.setRemainStock(coupon.getRemainStock());
                    }
                }
            } catch (Exception e) {
                log.warn("秒杀库存查询失败: id={}", vo.getId(), e);
            }
        }
    }

    public void invalidateDetail() {
        try {
            stringRedisTemplate.delete(DETAIL_MAP_KEY);
        } catch (Exception e) {
            log.warn("优惠券详情Redis缓存删除失败", e);
        }

        cacheInvalidateProducer.sendCacheInvalidate(DETAIL_MAP_KEY);
        detailLocalCache.invalidate(DETAIL_MAP_KEY);
    }

    public void invalidateLocalDetail() {
        detailLocalCache.invalidate(DETAIL_MAP_KEY);
    }

    private CacheResult<Map<Long, CouponDetailVO>> getDetailFromRedis() {
        try {
            String json = stringRedisTemplate.opsForValue().get(DETAIL_MAP_KEY);
            if (json == null) {
                return CacheResult.miss();
            }
            Map<Long, CouponDetailVO> map = JSONUtil.toBean(json, new TypeReference<Map<Long, CouponDetailVO>>() {
            }.getType(), false);
            return CacheResult.hit(map);
        } catch (Exception e) {
            log.warn("优惠券详情Redis缓存查询失败", e);
            return CacheResult.error();
        }
    }

    private void rebuildDetailRedisCache(Map<Long, CouponDetailVO> map) {
        try {
            stringRedisTemplate.opsForValue().set(
                    DETAIL_MAP_KEY,
                    JSONUtil.toJsonStr(map),
                    RedisConstant.Coupon.DETAIL_TTL_SECOND,
                    TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("优惠券详情Redis缓存写入失败", e);
        }
    }

    private Map<Long, CouponDetailVO> buildDetailMap() {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(Coupon::getStartTime, LocalDateTime.now())
                .ge(Coupon::getEndTime, LocalDateTime.now())
                .gt(Coupon::getRemainStock, 0);
        List<Coupon> list = couponMapper.selectList(wrapper);
        Map<Long, CouponDetailVO> map = new HashMap<>(list.size());
        for (Coupon coupon : list) {
            map.put(coupon.getId(), toDetailVO(coupon));
        }
        return map;
    }

    private List<CouponAvailableVO> getFromLocalCache() {
        return localCache.getIfPresent(LOCAL_KEY);
    }

    private CacheResult<List<CouponAvailableVO>> getFromRedis() {
        try {
            String json = stringRedisTemplate.opsForValue().get(REDIS_KEY);
            if (json == null) {
                return CacheResult.miss();
            }
            List<CouponAvailableVO> list = JSONUtil.toList(JSONUtil.parseArray(json), CouponAvailableVO.class);
            return CacheResult.hit(list);
        } catch (Exception e) {
            log.warn("获取可领取优惠券Redis缓存失败", e);
            return CacheResult.error();
        }
    }

    private List<CouponAvailableVO> loadFromDatabase() {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(Coupon::getStartTime, LocalDateTime.now())
                .ge(Coupon::getEndTime, LocalDateTime.now())
                .gt(Coupon::getRemainStock, 0)
                .orderByDesc(Coupon::getStartTime)
                .orderByDesc(Coupon::getId);

        List<Coupon> list = couponMapper.selectList(wrapper);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    private CouponAvailableVO toVO(Coupon coupon) {
        CouponAvailableVO vo = new CouponAvailableVO();
        vo.setId(coupon.getId());
        vo.setName(coupon.getName());
        vo.setDescription(coupon.getDescription());
        vo.setDiscountAmount(coupon.getDiscountAmount());
        vo.setMinAmount(coupon.getMinAmount());
        vo.setType(coupon.getType());
        vo.setStartTime(coupon.getStartTime());
        vo.setEndTime(coupon.getEndTime());
        return vo;
    }

    private CouponDetailVO toDetailVO(Coupon coupon) {
        CouponDetailVO vo = new CouponDetailVO();
        vo.setId(coupon.getId());
        vo.setName(coupon.getName());
        vo.setDescription(coupon.getDescription());
        vo.setDiscountAmount(coupon.getDiscountAmount());
        vo.setMinAmount(coupon.getMinAmount());
        vo.setType(coupon.getType());
        vo.setStock(coupon.getStock());
        vo.setRemainStock(coupon.getRemainStock());
        vo.setStartTime(coupon.getStartTime());
        vo.setEndTime(coupon.getEndTime());
        return vo;
    }

    private void rebuildLocalCache(List<CouponAvailableVO> data) {
        localCache.put(LOCAL_KEY, data);
    }

    private void rebuildRedisCache(List<CouponAvailableVO> data) {
        try {
            String json = JSONUtil.toJsonStr(data);
            stringRedisTemplate.opsForValue()
                    .set(REDIS_KEY, json, RedisConstant.Coupon.AVAILABLE_TTL_SECOND, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("重建可领取优惠券Redis缓存失败", e);
        }
    }

    public CacheResult<Boolean> checkClaimCouponFromRedis(Long id, Long userId) {
        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(RedisConstant.Coupon.BOUGHT_KEY + id, String.valueOf(userId)))) {
                return CacheResult.hit(true);
            }
            return CacheResult.miss();
        } catch (Exception e) {
            log.error("获取优惠卷购买状态失败", e);
            return CacheResult.error();
        }
    }

    public CacheResult<Boolean> checkClaimCoupon(Long id, Long userId) {
        CacheResult<Boolean> redisResult = checkClaimCouponFromRedis(id, userId);
        if (redisResult.isHit()) {
            return redisResult;
        }
        return checkClaimCouponFromDb(id, userId);
    }

    private CacheResult<Boolean> checkClaimCouponFromDb(Long id, Long userId) {
        try {
            Long count = userCouponMapper.selectCount(
                    Wrappers.<UserCoupon>lambdaQuery()
                            .eq(UserCoupon::getUserId, userId)
                            .eq(UserCoupon::getCouponId, id));
            if (count > 0) {
                stringRedisTemplate.opsForSet().add(
                        RedisConstant.Coupon.BOUGHT_KEY + id, String.valueOf(userId));
                return CacheResult.hit(true);
            }
            return CacheResult.miss();
        } catch (Exception e) {
            log.error("数据库查询优惠券领取状态失败: couponId={}, userId={}", id, userId, e);
            return CacheResult.error();
        }
    }

    public CouponDetailVO getCouponStatic(Long id) {
        Map<Long, CouponDetailVO> map = detailLocalCache.getIfPresent(DETAIL_MAP_KEY);
        if (map != null && map.containsKey(id)) {
            return map.get(id);
        }

        String cacheKey = RedisConstant.Coupon.STATIC_KEY + id;
        CouponDetailVO cached = staticLocalCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        CacheResult<CouponDetailVO> redisResult = getStaticFromRedis(id);
        if (redisResult.isHit()) {
            CouponDetailVO vo = redisResult.getData();
            if (vo != null) {
                staticLocalCache.put(cacheKey, vo);
            }
            return vo;
        }

        if (!redisResult.isError()) {
            Coupon coupon = couponMapper.selectById(id);
            if (coupon == null) {
                saveStaticNullToRedis(id);
                return null;
            }
            CouponDetailVO vo = toDetailVO(coupon);
            staticLocalCache.put(cacheKey, vo);
            saveStaticToRedis(id, vo);
            return vo;
        }

        Coupon dbCoupon = couponMapper.selectById(id);
        if (dbCoupon == null) return null;
        CouponDetailVO dbVo = toDetailVO(dbCoupon);
        staticLocalCache.put(cacheKey, dbVo);
        return dbVo;
    }

    private CacheResult<CouponDetailVO> getStaticFromRedis(Long id) {
        try {
            String json = stringRedisTemplate.opsForValue()
                    .get(RedisConstant.Coupon.STATIC_KEY + id);
            if (json == null) return CacheResult.miss();
            if (json.isEmpty()) return CacheResult.hit(null);
            CouponDetailVO vo = JSONUtil.toBean(json, CouponDetailVO.class);
            return CacheResult.hit(vo);
        } catch (Exception e) {
            log.warn("优惠券静态信息Redis查询失败: id={}", id, e);
            return CacheResult.error();
        }
    }

    private void saveStaticToRedis(Long id, CouponDetailVO vo) {
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisConstant.Coupon.STATIC_KEY + id,
                    JSONUtil.toJsonStr(vo),
                    RedisConstant.Coupon.STATIC_TTL_SECOND, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("优惠券静态信息Redis写入失败: id={}", id, e);
        }
    }

    private void saveStaticNullToRedis(Long id) {
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisConstant.Coupon.STATIC_KEY + id,
                    "",
                    RedisConstant.Coupon.STATIC_NULL_TTL_MINUTE, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("优惠券静态信息Redis写入空值失败: id={}", id, e);
        }
    }

    public List<Object> getCouponClaimStatus(Long userId,List<Long> ids){
        CacheResult<List<Object>> result = getClaimStatusFromRedis(userId, ids);
        if(result.isHit()){
            return result.getData();
        }
        return getClaimStatusFromDataBase(userId, ids);
    }

    private CacheResult<List<Object>> getClaimStatusFromRedis(Long userId, List<Long> ids) {
        try {
            List<Object> result= stringRedisTemplate.executePipelined((RedisCallback<?>) conn -> {
                var stringSerializer = stringRedisTemplate.getStringSerializer();
                ids.forEach(id -> {
                    byte[] key = stringSerializer.serialize(RedisConstant.Coupon.BOUGHT_KEY + id);
                    byte[] value = stringSerializer.serialize(userId.toString());
                    conn.sIsMember(key, value);
                });
                return null;
            });
            return CacheResult.hit(result);
        } catch (Exception e) {
            log.error("获取优惠卷购买状态失败", e);
            return CacheResult.error();
        }
    }

    private List<Object> getClaimStatusFromDataBase(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> claimedIds = userCouponMapper.selectList(
                        Wrappers.<UserCoupon>lambdaQuery()
                                .select(UserCoupon::getCouponId)
                                .eq(UserCoupon::getUserId, userId)
                                .in(UserCoupon::getCouponId, ids))
                .stream().map(UserCoupon::getCouponId).toList();
        return ids.stream()
                .map(id -> (Object) claimedIds.contains(id))
                .collect(Collectors.toList());
    }
}
