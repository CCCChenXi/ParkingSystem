package com.xigeandwillian.parkingsystem.admin.service;

import com.xigeandwillian.parkingsystem.admin.dto.auth.LoginDTO;
import com.xigeandwillian.parkingsystem.admin.dto.auth.ProfileUpdateDTO;
import com.xigeandwillian.parkingsystem.common.result.Result;

public interface AuthService {
    Result login(LoginDTO loginDTO);

    Result logout();

    Result updateProfile(ProfileUpdateDTO dto);
}
