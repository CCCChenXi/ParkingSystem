package com.xigeandwillian.parkingsystem.common.mapper;

import com.xigeandwillian.parkingsystem.client.dto.user.RegisterDTO;
import com.xigeandwillian.parkingsystem.client.vo.coupon.UserCouponMyVO;
import com.xigeandwillian.parkingsystem.client.vo.user.UserVO;
import com.xigeandwillian.parkingsystem.common.entity.Coupon;
import com.xigeandwillian.parkingsystem.common.entity.User;
import com.xigeandwillian.parkingsystem.common.entity.UserCoupon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConverter {

    UserVO toVO(User user);

    List<UserVO> toVOList(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "phone", source = "registerDTO.phone")
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    User toEntity(RegisterDTO registerDTO);

    @Mapping(target = "id", source = "coupon.id")
    @Mapping(target = "name", source = "coupon.name")
    @Mapping(target = "description", source = "coupon.description")
    @Mapping(target = "discountAmount", source = "coupon.discountAmount")
    @Mapping(target = "minAmount", source = "coupon.minAmount")
    @Mapping(target = "type", source = "coupon.type")
    @Mapping(target = "status", source = "userCoupon.status")
    @Mapping(target = "createTime", source = "userCoupon.createTime")
    @Mapping(target = "startTime", source = "coupon.startTime")
    @Mapping(target = "endTime", source = "coupon.endTime")
    UserCouponMyVO toMyVO(Coupon coupon, UserCoupon userCoupon);

}
