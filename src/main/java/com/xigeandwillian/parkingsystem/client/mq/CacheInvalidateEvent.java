package com.xigeandwillian.parkingsystem.client.mq;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class CacheInvalidateEvent implements Serializable {
    private String cacheKey;
}
