package com.xigeandwillian.parkingsystem.admin.mq;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ParkingSpotCacheRetryEvent implements Serializable {
    private Long lotId;
    private int retryCount;
    private String sourceQueue;
}
