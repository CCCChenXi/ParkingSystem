package com.xigeandwillian.parkingsystem.common.mapper;

import com.xigeandwillian.parkingsystem.client.vo.order.OrderConsumeVO;
import com.xigeandwillian.parkingsystem.client.vo.order.OrderInfoVO;
import com.xigeandwillian.parkingsystem.client.vo.order.OrderVO;
import com.xigeandwillian.parkingsystem.common.entity.ParkingOrder;
import com.xigeandwillian.parkingsystem.common.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ParkingOrderConverter {

    OrderVO toVO(ParkingOrder parkingOrder);

    List<OrderVO> toVOList(List<ParkingOrder> parkingOrders);

    @Mapping(target = "lotName", ignore = true)
    @Mapping(target = "spotNumber", ignore = true)
    OrderInfoVO toInfoVO(ParkingOrder parkingOrder);

    List<OrderInfoVO> toInfoVOList(List<ParkingOrder> parkingOrders);

    default OrderConsumeVO toConsumeVO(ParkingOrder parkingOrder, Wallet wallet) {
        BigDecimal payable = parkingOrder.getAmount().subtract(
            parkingOrder.getDiscount() != null ? parkingOrder.getDiscount() : BigDecimal.ZERO);
        return OrderConsumeVO.builder()
            .orderId(parkingOrder.getId())
            .amount(parkingOrder.getAmount())
            .discount(parkingOrder.getDiscount())
            .payable(payable)
            .balance(wallet.getBalance())
            .build();
    }
}
