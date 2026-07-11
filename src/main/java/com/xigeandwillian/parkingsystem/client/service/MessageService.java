package com.xigeandwillian.parkingsystem.client.service;

import com.xigeandwillian.parkingsystem.common.result.Result;

public interface MessageService {
    Result readOne(Long id);
    Result readAll();
}
