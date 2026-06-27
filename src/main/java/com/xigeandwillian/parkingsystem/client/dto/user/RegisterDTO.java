package com.xigeandwillian.parkingsystem.client.dto.user;

import com.xigeandwillian.parkingsystem.common.utils.RegexUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = RegexUtils.PHONE_REGEX, message = "手机号格式错误")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;
}