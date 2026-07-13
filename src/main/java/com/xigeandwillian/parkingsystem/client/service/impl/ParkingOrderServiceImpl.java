package com.xigeandwillian.parkingsystem.client.service.impl;


import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xigeandwillian.parkingsystem.client.dto.order.BookDTO;
import com.xigeandwillian.parkingsystem.client.dto.order.SettleDTO;
import com.xigeandwillian.parkingsystem.client.mapper.WalletLogMapper;
import com.xigeandwillian.parkingsystem.client.mq.BookingMessagePublisher;
import com.xigeandwillian.parkingsystem.client.mq.OrderEvent;
import com.xigeandwillian.parkingsystem.common.mq.CacheInvalidateProducer;
import com.xigeandwillian.parkingsystem.client.service.ParkingOrderService;
import com.xigeandwillian.parkingsystem.client.vo.coupon.CouponDetailVO;
import com.xigeandwillian.parkingsystem.common.service.impl.CouponDataProvider;
import com.xigeandwillian.parkingsystem.client.vo.order.OrderConsumeVO;
import com.xigeandwillian.parkingsystem.client.vo.order.OrderInfoVO;
import com.xigeandwillian.parkingsystem.client.vo.order.OrderVO;
import com.xigeandwillian.parkingsystem.common.vo.parkingspot.SpotListVO;
import com.xigeandwillian.parkingsystem.common.constant.*;
import com.xigeandwillian.parkingsystem.common.entity.*;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.mapper.*;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.utils.OrderIdFormer;
import com.xigeandwillian.parkingsystem.common.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static com.xigeandwillian.parkingsystem.common.constant.OrderConstant.*;
import static com.xigeandwillian.parkingsystem.common.constant.RedisConstant.Parking.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingOrderServiceImpl implements ParkingOrderService {


    /*注入*/
    private final UserCouponMapper userCouponMapper;
    private final WalletMapper walletMapper;
    private final WalletLogMapper walletLogMapper;
    private final OrderIdFormer orderIdFormer;
    private final ParkingLotMapper parkingLotMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final ParkingOrderMapper parkingOrderMapper;
    private final ParkingOrderConverter parkingOrderConverter;
    private final StringRedisTemplate stringRedisTemplate;
    private final BookingMessagePublisher bookingMessagePublisher;
    private final CacheInvalidateProducer cacheInvalidateProducer;

    private final CouponDataProvider couponDataProvider;
    private final com.xigeandwillian.parkingsystem.common.service.impl.ParkingDataProvider parkingDataProvider;
    private final com.xigeandwillian.parkingsystem.client.websocket.NotificationPublisher notificationPublisher;


    /**
     * 预约车位
     *
     * @return
     */
    @Override
    public Result bookSpot(BookDTO bookDTO) {
        Long seq = bookDTO.getSeq();
        //1.快速预检（非原子，只是提前过滤明显冲突）
        Boolean spotStatus = stringRedisTemplate.opsForValue().getBit(PARKING_SPOT_STATUS + bookDTO.getLotId(), seq);
        if (Boolean.TRUE.equals(spotStatus)) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "车位已被被占用");
        }
        //2.先生成订单（DB）
        ParkingOrder bookOrder = createOrder(bookDTO, ORDER_STATUS_RESERVED);
        //3.设置订单状态及汇率
        String json = stringRedisTemplate.opsForValue().get(PARKING_LOT_INFO + bookDTO.getLotId());
        ParkingLot parkingLot;
        if (json != null) {
            parkingLot = JSONUtil.toBean(json, ParkingLot.class);
        } else {
            parkingLot = parkingLotMapper.selectById(bookDTO.getLotId());
        }
        if (parkingLot == null) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "停车场不存在");
        }
        bookOrder.setHourlyRate(parkingLot.getHourlyRate());
        parkingOrderMapper.insert(bookOrder);
        OrderVO orderVO = parkingOrderConverter.toVO(bookOrder);
        //3.再设置车位状态（Redis），原子操作防止并发
        Boolean preStatus = stringRedisTemplate.opsForValue().setBit(PARKING_SPOT_STATUS + bookDTO.getLotId(), seq, true);
        if (Boolean.TRUE.equals(preStatus)) {
            parkingOrderMapper.deleteById(bookOrder.getId());
            throw new BusinessException(ResultConstant.BAD_REQUEST, "车位已被被占用");
        }
        parkingDataProvider.invalidateParkingSpotsCache(CaffeineConstant.PARKING_SPOTS_KEY_PREFIX + bookDTO.getLotId());
        cacheInvalidateProducer.sendCacheInvalidate(CaffeineConstant.PARKING_SPOTS_KEY_PREFIX + bookDTO.getLotId());
        //4.MQ发送消息
        OrderEvent event = new OrderEvent(
                bookOrder.getId(), bookOrder.getLotId(), bookOrder.getSpotId(),
                seq, bookOrder.getUserId(), bookOrder.getPlateNumber(), BOOK_TITLE, BOOK_CONTENT, ORDER_STATUS_RESERVED, bookOrder.getCreateTime());
        bookingMessagePublisher.sendBookingNotify(event);
        bookingMessagePublisher.sendBookingDelay(event);
        return Result.ok(orderVO);
    }

    @Override
    public Result orderList(Integer status) {
        //获取用户ID
        Long userId = UserHolder.get();
        //根据id,状态,时间查找排序
        List<ParkingOrder> orders = parkingOrderMapper.selectList(
                new LambdaQueryWrapper<ParkingOrder>()
                        .eq(ParkingOrder::getUserId, userId)
                        .eq(status != null, ParkingOrder::getStatus, status)
                        .orderByDesc(ParkingOrder::getCreateTime));
        //用户不存在订单->返回空集合
        if (orders.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        //获取所有停车场id(set去重)
        Set<Long> lotIds = orders.stream().map(ParkingOrder::getLotId).collect(Collectors.toSet());
        //获取所有车位id(set去重)
        Set<Long> spotIds = orders.stream().map(ParkingOrder::getSpotId).collect(Collectors.toSet());
        //用于存储停车场名称,车位编号
        Map<Long, String> lotNameMap = new HashMap<>();
        Map<Long, String> spotNumberMap = new HashMap<>();

        //批量获取
        for (Long lotId : lotIds) {
            //获取停车场名称
            String lotJson = stringRedisTemplate.opsForValue().get(RedisConstant.Parking.PARKING_LOT_INFO + lotId);
            if (lotJson != null && !lotJson.isEmpty()) {
                ParkingLot lot = JSONUtil.toBean(lotJson, ParkingLot.class);
                //1.存储停车场名称
                lotNameMap.put(lotId, lot.getName());
            } else {
                //未命中的数据库查询
                ParkingLot lot = parkingLotMapper.selectById(lotId);
                if (lot != null) lotNameMap.put(lotId, lot.getName());
            }

            //获取停车场车位信息
            String spotListJson = stringRedisTemplate.opsForValue()
                    .get(RedisConstant.Parking.PARKING_SPOT_LIST + lotId);
            if (spotListJson != null && !spotListJson.isEmpty()) {
                List<SpotListVO> spotList = JSONUtil.toList(JSONUtil.parseArray(spotListJson), SpotListVO.class);
                for (SpotListVO spot : spotList) {
                    //2.存储车位编号
                    spotNumberMap.put(spot.getId(), spot.getSpotNumber());
                }
            }
        }

        //Redis未命中的车位，按id集合一次查库
        Set<Long> missSpotIds = spotIds.stream()
                .filter(id -> !spotNumberMap.containsKey(id))
                .collect(Collectors.toSet());
        if (!missSpotIds.isEmpty()) {
            parkingSpotMapper.selectList(
                            new LambdaQueryWrapper<ParkingSpot>()
                                    .in(ParkingSpot::getId, missSpotIds))
                    .forEach(s -> spotNumberMap.put(s.getId(), s.getSpotNumber()));
        }

        List<OrderInfoVO> voList = orders.stream().map(o -> {
            OrderInfoVO vo = parkingOrderConverter.toInfoVO(o);
            vo.setLotName(lotNameMap.getOrDefault(o.getLotId(), ""));
            vo.setSpotNumber(spotNumberMap.getOrDefault(o.getSpotId(), ""));
            return vo;
        }).toList();

        return Result.ok(voList);
    }

    /**
     * 确认入场
     *
     * @param id
     * @return
     */
    @Override
    public Result checkEnter(Long id) {
        //根据id查询更新订单
        parkingOrderMapper.update(new ParkingOrder(),
                new LambdaUpdateWrapper<ParkingOrder>()
                        .eq(ParkingOrder::getId, id)
                        .set(ParkingOrder::getStatus, OrderConstant.ORDER_STATUS_IN_PROGRESS)
                        .set(ParkingOrder::getStartTime, LocalDateTime.now()));

        return Result.ok();
    }

    /**
     * 直接入场
     *
     * @param bookDTO
     * @return
     */
    @Override
    public Result directEnter(BookDTO bookDTO) {
        Long seq = bookDTO.getSeq();
        //1.快速预检（非原子，只是提前过滤明显冲突）
        Boolean spotStatus = stringRedisTemplate.opsForValue().getBit(PARKING_SPOT_STATUS + bookDTO.getLotId(), seq);
        if (Boolean.TRUE.equals(spotStatus)) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "车位已被被占用");
        }
        //2.先生成订单（DB）
        ParkingOrder order = createOrder(bookDTO, ORDER_STATUS_IN_PROGRESS);
        //3.设置订单状态及汇率
        String json = stringRedisTemplate.opsForValue().get(PARKING_LOT_INFO + bookDTO.getLotId());
        ParkingLot parkingLot;
        if (json != null) {
            parkingLot = JSONUtil.toBean(json, ParkingLot.class);
        } else {
            parkingLot = parkingLotMapper.selectById(bookDTO.getLotId());
        }
        if (parkingLot == null) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "停车场不存在");
        }
        order.setHourlyRate(parkingLot.getHourlyRate());
        order.setStartTime(LocalDateTime.now());
        parkingOrderMapper.insert(order);
        OrderVO orderVO = parkingOrderConverter.toVO(order);
        //3.再设置车位状态（Redis），原子操作防止并发
        Boolean preStatus = stringRedisTemplate.opsForValue().setBit(PARKING_SPOT_STATUS + bookDTO.getLotId(), seq, true);
        if (Boolean.TRUE.equals(preStatus)) {
            parkingOrderMapper.deleteById(order.getId());
            throw new BusinessException(ResultConstant.BAD_REQUEST, "车位已被被占用");
        }
        parkingDataProvider.invalidateParkingSpotsCache(CaffeineConstant.PARKING_SPOTS_KEY_PREFIX + bookDTO.getLotId());
        cacheInvalidateProducer.sendCacheInvalidate(CaffeineConstant.PARKING_SPOTS_KEY_PREFIX + bookDTO.getLotId());
        //4.MQ发送消息
        OrderEvent event = new OrderEvent(
                order.getId(), order.getLotId(), order.getSpotId(),
                seq, order.getUserId(), order.getPlateNumber(), ENTER_SUCCESS_TITLE, ENTER_SUCCESS_CONTENT, ORDER_STATUS_IN_PROGRESS, order.getCreateTime());
        bookingMessagePublisher.sendBookingNotify(event);
        return Result.ok(orderVO);
    }

    /**
     * 取消预约
     *
     * @param id
     * @return
     */
    @Override
    public Result cancelBook(Long id) {
        //更新数据库订单状态
        parkingOrderMapper.update(new ParkingOrder(), new LambdaUpdateWrapper<ParkingOrder>()
                .eq(ParkingOrder::getId, id)
                .set(ParkingOrder::getStatus, OrderConstant.ORDER_STATUS_CANCELLED)
        );
        //获取停车场id,车位seq
        ParkingOrder order = parkingOrderMapper.selectById(id);
        Long lotId = order.getLotId();
        Long spotId = order.getSpotId();
        List<SpotListVO> allSpotByLotId = parkingDataProvider.getAllSpotByLotId(lotId);
        Long seq = allSpotByLotId.stream()
                .filter(s -> s.getId().equals(spotId))
                .map(SpotListVO::getSeq)
                .findFirst()
                .orElse(null);
        //删除Redis中的车位状态
        try {
            stringRedisTemplate.opsForValue().setBit(PARKING_SPOT_STATUS + lotId, seq, false);
        } catch (Exception e) {
            log.error("Redis释放车位异常: lotId={}, seq={}", lotId, seq, e);
        }
        parkingDataProvider.invalidateParkingSpotsCache(CaffeineConstant.PARKING_SPOTS_KEY_PREFIX + lotId);
        cacheInvalidateProducer.sendCacheInvalidate(CaffeineConstant.PARKING_SPOTS_KEY_PREFIX + lotId);
        return Result.ok();
    }

    /**
     * 结算
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result settle(Long id, SettleDTO settleDTO) {

        // ① 查订单
        ParkingOrder order = parkingOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "订单不存在");
        }
        if (order.getStatus() != ORDER_STATUS_IN_PROGRESS) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "订单已结算");
        }

        // ② 计费
        long durationMs = LocalDateTime.now()
                .toInstant(ZoneOffset.of(OrderConstant.ZONE_OFFSET)).toEpochMilli()
                - order.getStartTime().toInstant(ZoneOffset.of(OrderConstant.ZONE_OFFSET)).toEpochMilli();
        long durationHour = (durationMs + OrderConstant.MILLIS_PER_HOUR - 1) / OrderConstant.MILLIS_PER_HOUR;
        BigDecimal bill = order.getHourlyRate()
                .multiply(BigDecimal.valueOf(durationHour))
                .setScale(2, RoundingMode.HALF_UP);

        // ③ 校验优惠券
        BigDecimal discount = BigDecimal.ZERO;
        if (settleDTO.getCouponId() != null) {
            CouponDetailVO coupon = couponDataProvider.getDetailById(settleDTO.getCouponId());
            if (coupon == null) {
                throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券不存在");
            }
            if (bill.compareTo(coupon.getMinAmount()) < 0) {
                throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券不满足条件");
            }
            long usedCount = userCouponMapper.selectCount(
                    new LambdaQueryWrapper<UserCoupon>()
                            .eq(UserCoupon::getUserId, UserHolder.get())
                            .eq(UserCoupon::getCouponId, settleDTO.getCouponId())
                            .eq(UserCoupon::getStatus, CouponConstant.USER_COUPON_STATUS_USED));
            if (usedCount > 0) {
                throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券已使用");
            }
            discount = coupon.getDiscountAmount();
        }

        BigDecimal payable = bill.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (payable.compareTo(BigDecimal.ZERO) < 0) {
            payable = BigDecimal.ZERO;
        }

        // ④ 更新订单（乐观锁）
        int orderRows = parkingOrderMapper.update(null,
                new LambdaUpdateWrapper<ParkingOrder>()
                        .eq(ParkingOrder::getId, id)
                        .eq(ParkingOrder::getStatus, ORDER_STATUS_IN_PROGRESS)
                        .set(ParkingOrder::getStatus, ORDER_STATUS_SETTLED)
                        .set(ParkingOrder::getEndTime, LocalDateTime.now())
                        .set(ParkingOrder::getAmount, bill)
                        .set(ParkingOrder::getCouponId, settleDTO.getCouponId())
                        .set(ParkingOrder::getDiscount, discount));
        if (orderRows == 0) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "订单已结算");
        }

        // ⑤ 更新优惠券（乐观锁）
        if (settleDTO.getCouponId() != null) {
            int couponRows = userCouponMapper.update(null,
                    new LambdaUpdateWrapper<UserCoupon>()
                            .eq(UserCoupon::getUserId, UserHolder.get())
                            .eq(UserCoupon::getCouponId, settleDTO.getCouponId())
                            .eq(UserCoupon::getStatus, CouponConstant.USER_COUPON_STATUS_UNUSED)
                            .set(UserCoupon::getStatus, CouponConstant.USER_COUPON_STATUS_USED)
                            .set(UserCoupon::getUseTime, LocalDateTime.now()));
            if (couponRows == 0) {
                throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券已使用");
            }
        }

        // ⑥ 钱包扣款 + 流水
        int walletRows = walletMapper.update(null,
                new LambdaUpdateWrapper<Wallet>()
                        .eq(Wallet::getUserId, UserHolder.get())
                        .ge(Wallet::getBalance, payable)
                        .setSql("balance = balance - " + payable));
        if (walletRows == 0 && payable.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "钱包余额不足");
        }
        Wallet wallet = walletMapper.selectOne(
                new QueryWrapper<Wallet>().eq("user_id", UserHolder.get()));

        WalletLog walletLog = new WalletLog();
        walletLog.setWalletId(wallet.getId());
        walletLog.setAmount(payable.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : payable.negate());
        walletLog.setType(WalletConstant.PAY_TYPE);
        walletLog.setRemark(WalletConstant.PAY_REMARK);
        walletLogMapper.insert(walletLog);

        // ⑦ afterCommit: 释放车位 + 通知
        Long finalLotId = order.getLotId();
        Long finalSpotId = order.getSpotId();
        BigDecimal finalPayable = payable;
        BigDecimal finalDiscount = discount;
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        List<SpotListVO> spots = parkingDataProvider.getAllSpotByLotId(finalLotId);
                        long seq = 0;
                        for (SpotListVO spot : spots) {
                            if (spot.getId().equals(finalSpotId)) {
                                seq = spot.getSeq();
                                break;
                            }
                        }
                        try {
                            stringRedisTemplate.opsForValue()
                                    .setBit(PARKING_SPOT_STATUS + finalLotId, seq, false);
                        } catch (Exception e) {
                            log.warn("释放车位失败，加入重试队列: lotId={}, seq={}", finalLotId, seq, e);
                            bookingMessagePublisher.sendSpotReleaseRetry(finalLotId, seq);
                        }
                        parkingDataProvider.invalidateParkingSpotsCache(CaffeineConstant.PARKING_SPOTS_KEY_PREFIX + finalLotId);
                        cacheInvalidateProducer.sendCacheInvalidate(CaffeineConstant.PARKING_SPOTS_KEY_PREFIX + finalLotId);
                        notificationPublisher.publish(
                                UserHolder.get(), "结算完成", "停车费用已结算", 2);
                    }
                });

        OrderConsumeVO consumeVO = parkingOrderConverter.toConsumeVO(order, wallet);
        return Result.ok(consumeVO);
    }


    /*生成订单*/
    private ParkingOrder createOrder(BookDTO bookDTO, int status) {
        ParkingOrder parkingOrder = new ParkingOrder();
        parkingOrder.setOrderNo(orderIdFormer.nextId("spotOrder"));
        parkingOrder.setUserId(UserHolder.get());
        parkingOrder.setLotId(bookDTO.getLotId());
        parkingOrder.setSpotId(bookDTO.getSpotId());
        parkingOrder.setPlateNumber(bookDTO.getPlateNumber());
        parkingOrder.setStatus(status);
        return parkingOrder;
    }


}
