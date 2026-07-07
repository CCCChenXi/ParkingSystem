package com.xigeandwillian.parkingsystem.client.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xigeandwillian.parkingsystem.client.dto.order.BookDTO;
import com.xigeandwillian.parkingsystem.client.dto.order.SettleDTO;
import com.xigeandwillian.parkingsystem.client.mapper.WalletLogMapper;
import com.xigeandwillian.parkingsystem.client.mq.OrderEvent;
import com.xigeandwillian.parkingsystem.client.mq.SettleEvent;
import com.xigeandwillian.parkingsystem.client.service.service.ParkingOrderService;
import com.xigeandwillian.parkingsystem.client.vo.coupon.CouponDetailVO;
import com.xigeandwillian.parkingsystem.client.vo.order.OrderConsumeVO;
import com.xigeandwillian.parkingsystem.client.vo.order.OrderInfoVO;
import com.xigeandwillian.parkingsystem.client.vo.order.OrderVO;
import com.xigeandwillian.parkingsystem.client.vo.parkingLot.SpotVO;
import com.xigeandwillian.parkingsystem.common.config.RabbitMQConfig;
import com.xigeandwillian.parkingsystem.common.constant.*;
import com.xigeandwillian.parkingsystem.common.entity.*;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.mapper.*;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.utils.OrderIdFormer;
import com.xigeandwillian.parkingsystem.common.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
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
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private final CouponDataProvider couponDataProvider;
    private final ParkingDataProvider parkingDataProvider;
    private final ApplicationEventPublisher applicationEventPublisher;


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
        ParkingLot parkingLot = JSONUtil.toBean(json, ParkingLot.class);
        bookOrder.setHourlyRate(parkingLot.getHourlyRate());
        parkingOrderMapper.insert(bookOrder);
        OrderVO orderVO = new OrderVO();
        BeanUtil.copyProperties(bookOrder, orderVO);
        //3.再设置车位状态（Redis），原子操作防止并发
        Boolean preStatus = stringRedisTemplate.opsForValue().setBit(PARKING_SPOT_STATUS + bookDTO.getLotId(), seq, true);
        if (Boolean.TRUE.equals(preStatus)) {
            parkingOrderMapper.deleteById(bookOrder.getId());
            throw new BusinessException(ResultConstant.BAD_REQUEST, "车位已被被占用");
        }
        //4.MQ发送消息
        OrderEvent event = new OrderEvent(
                bookOrder.getId(), bookOrder.getLotId(), bookOrder.getSpotId(),
                seq, bookOrder.getUserId(), bookOrder.getPlateNumber(), BOOK_TITLE, BOOK_CONTENT, ORDER_STATUS_RESERVED, bookOrder.getCreateTime());
        //发送消息
        rabbitTemplate.convertAndSend(RabbitMQConfig.BOOKING_EXCHANGE,
                RabbitMQConfig.BOOKING_NOTIFY_ROUTING_KEY, event);
        //发送延迟消息
        rabbitTemplate.convertAndSend(RabbitMQConfig.BOOKING_EXCHANGE,
                RabbitMQConfig.BOOKING_DELAY_ROUTING_KEY, event);
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
                List<SpotVO> spotList = JSONUtil.toList(JSONUtil.parseArray(spotListJson), SpotVO.class);
                for (SpotVO spot : spotList) {
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
            OrderInfoVO vo = new OrderInfoVO();
            BeanUtil.copyProperties(o, vo);
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
        ParkingLot parkingLot = JSONUtil.toBean(json, ParkingLot.class);
        order.setHourlyRate(parkingLot.getHourlyRate());
        order.setStartTime(LocalDateTime.now());
        parkingOrderMapper.insert(order);
        OrderVO orderVO = new OrderVO();
        BeanUtil.copyProperties(order, orderVO);
        //3.再设置车位状态（Redis），原子操作防止并发
        Boolean preStatus = stringRedisTemplate.opsForValue().setBit(PARKING_SPOT_STATUS + bookDTO.getLotId(), seq, true);
        if (Boolean.TRUE.equals(preStatus)) {
            parkingOrderMapper.deleteById(order.getId());
            throw new BusinessException(ResultConstant.BAD_REQUEST, "车位已被被占用");
        }
        //4.MQ发送消息
        OrderEvent event = new OrderEvent(
                order.getId(), order.getLotId(), order.getSpotId(),
                seq, order.getUserId(), order.getPlateNumber(), ENTER_SUCCESS_TITLE, ENTER_SUCCESS_CONTENT, ORDER_STATUS_IN_PROGRESS, order.getCreateTime());
        //发送消息
        rabbitTemplate.convertAndSend(RabbitMQConfig.BOOKING_EXCHANGE,
                RabbitMQConfig.BOOKING_NOTIFY_ROUTING_KEY, event);
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
        List<SpotVO> allSpotByLotId = parkingDataProvider.getAllSpotByLotId(lotId);
        Long seq = allSpotByLotId.stream()
                .filter(s -> s.getId().equals(spotId))
                .map(SpotVO::getSeq)
                .findFirst()
                .orElse(null);
        //删除Redis中的车位状态
        updateSpotStatus(lotId, seq);
        return Result.ok();
    }

    /**
     * 结算
     *
     * @param id
     * @param settleDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result settle(Long id, SettleDTO settleDTO) {

        ParkingOrder parkingOrder = parkingOrderMapper.selectById(id);
        if (parkingOrder == null || parkingOrder.getStatus() != ORDER_STATUS_IN_PROGRESS) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "订单不存在或已结算");
        }

        //1获取订单时长
        LocalDateTime startTime = parkingOrder.getStartTime();
        LocalDateTime endTime = LocalDateTime.now();
        //1.1毫秒值
        long durationMilli = endTime.toInstant(ZoneOffset.of("+8")).toEpochMilli() - startTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
        //1.2小时值
        long durationHour = durationMilli / 1000 / 60 / 60;
        //2原价
        BigDecimal bill = parkingOrder.getHourlyRate()
                .multiply(BigDecimal.valueOf(durationHour))//相乘
                .setScale(2, RoundingMode.HALF_UP);//保留两位小数


        //优惠金额
        BigDecimal discount;

        if (settleDTO.getCouponId() != null) {
            //根据id查询优惠卷
            CouponDetailVO couponDetail = couponDataProvider.getDetailById(settleDTO.getCouponId());
            if (couponDetail == null) {
                throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券不存在");
            }
            //比较用卷最低标准
            if (couponDetail.getMinAmount().compareTo(bill) > 0) {
                throw new BusinessException(ResultConstant.BAD_REQUEST, "优惠券不满足条件");
            }
            discount = couponDetail.getDiscountAmount();
        } else {
            discount = BigDecimal.ZERO;
        }

        //应支付金额
        BigDecimal payable = bill.subtract(discount).setScale(2, RoundingMode.HALF_UP);


        //更新订单结算时间
        int orderRows = parkingOrderMapper.update(new ParkingOrder(), new LambdaUpdateWrapper<ParkingOrder>()
                .eq(ParkingOrder::getId, id)
                .eq(ParkingOrder::getStatus, ORDER_STATUS_IN_PROGRESS)//sql行级锁
                .set(ParkingOrder::getEndTime, endTime)
                .set(ParkingOrder::getAmount, bill)//原金额
                .set(ParkingOrder::getCouponId, settleDTO.getCouponId())
                .set(ParkingOrder::getDiscount, discount)
                .set(ParkingOrder::getStatus, ORDER_STATUS_SETTLED));

        if (orderRows == 0) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "订单不存在");
        }


        //1更新钱包余额(数据库2)
        int walletRows = walletMapper.update(new Wallet(), new LambdaUpdateWrapper<Wallet>()
                .eq(Wallet::getUserId, UserHolder.get())
                .ge(Wallet::getBalance, payable)//余额校验
                .setSql("balance = balance - " + payable)
        );

        if (walletRows == 0) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "钱包余额不足");
        }

        //2更新用户优惠券使用状态
        if (settleDTO.getCouponId() != null) {
            userCouponMapper.update(new UserCoupon(),
                    new LambdaUpdateWrapper<UserCoupon>()
                            .eq(UserCoupon::getUserId, UserHolder.get())
                            .eq(UserCoupon::getCouponId, settleDTO.getCouponId())
                            .eq(UserCoupon::getStatus, CouponConstant.USER_COUPON_STATUS_UNUSED)
                            .set(UserCoupon::getStatus, CouponConstant.USER_COUPON_STATUS_USED)
                            .set(UserCoupon::getUseTime, LocalDateTime.now())
            );
        }

        //(数据库3)
        Wallet wallet = walletMapper.selectOne(new QueryWrapper<Wallet>().eq("user_id", UserHolder.get()));


        //3更新钱包日志(数据库4)
        WalletLog walletLog = new WalletLog();
        walletLog.setWalletId(wallet.getId());
        walletLog.setAmount(payable);
        walletLog.setType(WalletConstant.PAY_TYPE);
        walletLog.setRemark(WalletConstant.PAY_REMARK);
        walletLogMapper.insert(walletLog);


        //3释放车位状态
        updateSpotStatus(settleDTO.getLotId(), settleDTO.getSeq());


        //4.事务提交后发送消息
        applicationEventPublisher.publishEvent(new SettleEvent(
                id, settleDTO.getLotId(), settleDTO.getSpotId(), settleDTO.getSeq(),
                parkingOrder.getUserId(), parkingOrder.getPlateNumber(), endTime));


        //创建返回对象
        OrderConsumeVO consumeVO = OrderConsumeVO.builder()
                .orderId(id)
                .amount(bill)
                .discount(discount)
                .payable(payable)
                .balance(wallet.getBalance())
                .build();

        return Result.ok(consumeVO);
    }

    /*释放车位状态*/
    private void updateSpotStatus(Long lotId, Long seq) {
        try {
            stringRedisTemplate.opsForValue().setBit(PARKING_SPOT_STATUS + lotId, seq, false);
        } catch (Exception e) {
            log.error("redis异常");
        }
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
