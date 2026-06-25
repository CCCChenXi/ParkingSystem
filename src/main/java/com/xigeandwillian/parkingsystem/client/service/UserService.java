package com.xigeandwillian.parkingsystem.client.service;


import com.xigeandwillian.parkingsystem.client.dto.user.RegisterDTO;
import com.xigeandwillian.parkingsystem.common.result.Result;

public interface UserService {

    Result register(RegisterDTO registerDTO);
}
