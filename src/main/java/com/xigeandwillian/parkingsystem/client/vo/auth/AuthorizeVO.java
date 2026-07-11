package com.xigeandwillian.parkingsystem.client.vo.auth;

import com.xigeandwillian.parkingsystem.client.vo.user.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthorizeVO {
    private String token;
    private UserVO userVO;
}
