package com.xigeandwillian.parkingsystem.common.mapper;

import com.xigeandwillian.parkingsystem.admin.dto.coupon.CouponSaveDTO;
import com.xigeandwillian.parkingsystem.admin.vo.coupon.CouponListVO;
import com.xigeandwillian.parkingsystem.client.vo.coupon.CouponAvailableVO;
import com.xigeandwillian.parkingsystem.client.vo.coupon.CouponDetailVO;
import com.xigeandwillian.parkingsystem.common.entity.Coupon;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CouponConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "remainStock", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    Coupon toEntity(CouponSaveDTO dto);

    CouponListVO toListVO(Coupon coupon);

    List<CouponListVO> toListVOList(List<Coupon> coupons);

    CouponAvailableVO toAvailableVO(Coupon coupon);

    CouponDetailVO toDetailVO(Coupon coupon);

    @AfterMapping
    default void afterToAvailableVO(Coupon coupon, @MappingTarget CouponAvailableVO vo) {
        vo.setClaim(null);
    }

    @AfterMapping
    default void afterToDetailVO(Coupon coupon, @MappingTarget CouponDetailVO vo) {
        vo.setClaim(null);
        vo.setStatus(null);
    }
}
