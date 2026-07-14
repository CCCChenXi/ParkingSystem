package com.xigeandwillian.parkingsystem.client.service;

import com.xigeandwillian.parkingsystem.client.dto.order.BookDTO;
import com.xigeandwillian.parkingsystem.client.dto.order.SettleDTO;
import com.xigeandwillian.parkingsystem.common.result.Result;

public interface ParkingOrderService {

    Result bookSpot(BookDTO bookDTO);

    Result orderList(Integer status, Long lastTimestamp, Long lastId, Integer pageSize);

    Result checkEnter(Long id);

    Result directEnter(BookDTO bookDTO);

    Result cancelBook(Long id);

    Result settle(Long id, SettleDTO settleDTO);
}
