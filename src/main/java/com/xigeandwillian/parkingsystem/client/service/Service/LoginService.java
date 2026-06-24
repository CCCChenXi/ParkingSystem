package com.xigeandwillian.parkingsystem.client.service.Service;

import com.xigeandwillian.parkingsystem.client.dto.userLoginDTO;
import com.xigeandwillian.parkingsystem.common.result.Result;
public interface LoginService {
    /**
     * 登录
     * @param userLoginDTO
     * @return
     */
    Result login(userLoginDTO userLoginDTO);
}
