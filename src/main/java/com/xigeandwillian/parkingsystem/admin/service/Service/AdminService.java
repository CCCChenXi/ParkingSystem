package com.xigeandwillian.parkingsystem.admin.service.Service;

import com.xigeandwillian.parkingsystem.admin.dto.admin.LoginDTO;
import com.xigeandwillian.parkingsystem.common.result.Result;

public interface AdminService {
    Result login(LoginDTO loginDTO);
}
