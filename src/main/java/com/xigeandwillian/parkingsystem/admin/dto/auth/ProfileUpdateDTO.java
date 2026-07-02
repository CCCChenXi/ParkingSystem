package com.xigeandwillian.parkingsystem.admin.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
