package com.xigeandwillian.parkingsystem.client.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterVO {
    String token;
    ProfileVO user;
}
