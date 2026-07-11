package com.xigeandwillian.parkingsystem.admin.mq;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ParkingLotCacheInitEvent implements Serializable {
    private Long lotId;
    private Double longitude;
    private Double latitude;
    private int retryCount;
}
