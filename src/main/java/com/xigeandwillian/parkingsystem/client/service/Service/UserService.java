package com.xigeandwillian.parkingsystem.client.service.Service;

import com.xigeandwillian.parkingsystem.client.dto.user.ProfileEditDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.RegisterDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.LoginDTO;
import com.xigeandwillian.parkingsystem.common.result.Result;

public interface UserService {
    Result login(LoginDTO LoginDTO);

    Result register(RegisterDTO registerDTO);

    Result sendCode(String phone);

    Result userProfile();

    Result editProfile(ProfileEditDTO profileEditDTO);

    Result vehiclesInfo();
}
