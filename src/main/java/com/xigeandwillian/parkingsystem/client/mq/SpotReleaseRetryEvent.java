package com.xigeandwillian.parkingsystem.client.mq;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class SpotReleaseRetryEvent implements Serializable {
    private Long lotId;
    private Long seq;
    private int retryCount;
}
