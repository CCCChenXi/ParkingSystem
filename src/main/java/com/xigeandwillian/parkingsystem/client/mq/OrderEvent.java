package com.xigeandwillian.parkingsystem.client.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class OrderEvent {
    private Long orderId;
    private Long lotId;
    private Long spotId;
    private Long seq;
    private Long userId;
    private String plateNumber;
    private String title;
    private String content;
    private Integer msgType;
    private LocalDateTime createTime;
}
