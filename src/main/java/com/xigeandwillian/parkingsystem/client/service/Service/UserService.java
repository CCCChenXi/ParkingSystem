package com.xigeandwillian.parkingsystem.client.service.Service;

import com.xigeandwillian.parkingsystem.client.dto.user.RegisterDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.userLoginDTO;
import com.xigeandwillian.parkingsystem.common.result.Result;
public interface UserService {
    Result login(userLoginDTO userLoginDTO);

    Result register(RegisterDTO registerDTO);

    Result sendCode(String phone);
}
