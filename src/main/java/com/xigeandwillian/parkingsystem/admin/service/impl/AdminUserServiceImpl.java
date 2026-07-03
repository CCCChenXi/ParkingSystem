package com.xigeandwillian.parkingsystem.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xigeandwillian.parkingsystem.admin.service.Service.AdminUserService;
import com.xigeandwillian.parkingsystem.admin.vo.user.AdminUserListVO;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingOrderMapper;
import com.xigeandwillian.parkingsystem.common.mapper.UserMapper;
import com.xigeandwillian.parkingsystem.common.mapper.VehicleMapper;
import com.xigeandwillian.parkingsystem.common.mapper.WalletMapper;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingOrder;
import com.xigeandwillian.parkingsystem.common.entity.User;
import com.xigeandwillian.parkingsystem.common.entity.Vehicle;
import com.xigeandwillian.parkingsystem.common.entity.Wallet;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.result.PageResult;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final VehicleMapper vehicleMapper;
    private final ParkingOrderMapper parkingOrderMapper;
    private final WalletMapper walletMapper;

    /**
     * 执行流程
     * 1.对用户表进行分页查询
     * 2.查询订单数
     * 3.查询车辆数
     * 4.查询钱包
     *
     * @param keyword 根据关键字对名字和手机号模糊查询
     * @return 当前页用户列表
     */
    @Override
    public Result page(Integer page, Integer size, String keyword) {

        if (keyword != null && keyword.length() > RedisConstant.Parking.KEYWORD_MAX_LENGTH) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "关键字过长");
        }

        Page<User> p = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getPhone, keyword));
        }
        Page<User> userPage = userMapper.selectPage(p, wrapper);

        List<Long> userIds = userPage.getRecords().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        Map<Long, BigDecimal> walletMap;
        if (userIds.isEmpty()) {
            walletMap = Map.of();
        } else {
            LambdaQueryWrapper<Wallet> walletWrapper = new LambdaQueryWrapper<>();
            walletWrapper.in(Wallet::getUserId, userIds);
            walletMap = walletMapper.selectList(walletWrapper).stream()
                    .collect(Collectors.toMap(Wallet::getUserId, Wallet::getBalance));
        }

        List<AdminUserListVO> voList = userPage.getRecords().stream().map(user -> {
            AdminUserListVO vo = new AdminUserListVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setPhone(user.getPhone());
            vo.setCreateTime(user.getCreateTime());

            BigDecimal balance = walletMap.get(user.getId());
            vo.setBalance(balance != null ? balance : BigDecimal.ZERO);

            LambdaQueryWrapper<Vehicle> vehicleWrapper = new LambdaQueryWrapper<>();
            vehicleWrapper.eq(Vehicle::getUserId, user.getId());
            long vehicleCount = vehicleMapper.selectCount(vehicleWrapper);
            vo.setVehicles((int) vehicleCount);

            LambdaQueryWrapper<ParkingOrder> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(ParkingOrder::getUserId, user.getId());
            long orderCount = parkingOrderMapper.selectCount(orderWrapper);
            vo.setOrderCount((int) orderCount);

            return vo;
        }).collect(Collectors.toList());

        PageResult<AdminUserListVO> pageResult = new PageResult<>();
        pageResult.setTotal(userPage.getTotal());
        pageResult.setDataList(voList);
        return Result.ok(pageResult);
    }

    @Override
    public Result detail(Long id) {
        if (id == null) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "用户ID不能为空");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            log.warn("用户不存在: id={}", id);
            throw new BusinessException(ResultConstant.BAD_REQUEST, "用户不存在");
        }

        AdminUserListVO vo = new AdminUserListVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setCreateTime(user.getCreateTime());

        LambdaQueryWrapper<Vehicle> vehicleWrapper = new LambdaQueryWrapper<>();
        vehicleWrapper.eq(Vehicle::getUserId, user.getId());
        long vehicleCount = vehicleMapper.selectCount(vehicleWrapper);
        vo.setVehicles((int) vehicleCount);

        LambdaQueryWrapper<ParkingOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(ParkingOrder::getUserId, user.getId());
        long orderCount = parkingOrderMapper.selectCount(orderWrapper);
        vo.setOrderCount((int) orderCount);

        LambdaQueryWrapper<Wallet> walletWrapper = new LambdaQueryWrapper<>();
        walletWrapper.eq(Wallet::getUserId, user.getId());
        Wallet wallet = walletMapper.selectOne(walletWrapper);
        vo.setBalance(wallet != null ? wallet.getBalance() : BigDecimal.ZERO);

        return Result.ok(vo);
    }
}
