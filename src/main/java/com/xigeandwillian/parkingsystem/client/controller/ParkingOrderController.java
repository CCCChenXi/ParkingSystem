package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.dto.order.BookDTO;
import com.xigeandwillian.parkingsystem.client.dto.order.SettleDTO;
import com.xigeandwillian.parkingsystem.client.service.ParkingOrderService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class ParkingOrderController {

    private final ParkingOrderService parkingOrderService;

    /**
     * 预约车位
     *
     * @return
     */
    @PostMapping
    public Result bookSpot(@RequestBody @Validated BookDTO bookDTO) {
        log.info("预约车位: lotId={}, spotId={}, seq={}", bookDTO.getLotId(), bookDTO.getSpotId(), bookDTO.getSeq());
        return parkingOrderService.bookSpot(bookDTO);
    }

    /**
     * 预约后确认进入停车场
     *
     * @return
     */
    @PutMapping("/{id}/enter")
    public Result checkEnter(@PathVariable Long id) {
        log.info("确认入场: orderId={}", id);
        return parkingOrderService.checkEnter(id);
    }

    /**
     * 获取订单列表
     *
     * @return
     */
    @GetMapping
    public Result orderList(
            @RequestParam Integer status,
            @RequestParam(required = false) Long lastTimestamp,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        log.info("获取订单列表: status={}, lastTimestamp={}, lastId={}, pageSize={}",
                status, lastTimestamp, lastId, pageSize);
        return parkingOrderService.orderList(status, lastTimestamp, lastId, pageSize);
    }


    /**
     * 直接入场
     *
     * @return
     */
    @PostMapping("/direct")
    public Result directEnter(@RequestBody @Validated BookDTO bookDTO) {
        log.info("直接入场: lotId={}, spotId={}, seq={}", bookDTO.getLotId(), bookDTO.getSpotId(), bookDTO.getSeq());
        return parkingOrderService.directEnter(bookDTO);
    }

    /**
     * 取消预约
     *
     * @return
     */
    @PutMapping("/{id}/cancel")
    public Result cancelBook(@PathVariable Long id) {
        log.info("取消预约: orderId={}", id);
        return parkingOrderService.cancelBook(id);
    }

    /**
     * 结算
     *
     * @return
     */
    @PutMapping("/{id}/settle")
    public Result settle(@PathVariable Long id, @RequestBody SettleDTO settleDTO) {
        log.info("结算订单: orderId={}, couponId={}", id, settleDTO.getCouponId());
        return parkingOrderService.settle(id, settleDTO);
    }


}
