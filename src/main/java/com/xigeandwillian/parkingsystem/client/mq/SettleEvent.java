package com.xigeandwillian.parkingsystem.client.mq;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SettleEvent {
    private final Long orderId;
    private final Long lotId;
    private final Long spotId;
    private final Long seq;
    private final Long userId;
    private final String plateNumber;
    private final LocalDateTime endTime;

    public SettleEvent(Long orderId, Long lotId, Long spotId, Long seq,
                       Long userId, String plateNumber, LocalDateTime endTime) {
        this.orderId = orderId;
        this.lotId = lotId;
        this.spotId = spotId;
        this.seq = seq;
        this.userId = userId;
        this.plateNumber = plateNumber;
        this.endTime = endTime;
    }
}
