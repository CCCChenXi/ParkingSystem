package com.xigeandwillian.parkingsystem.client.dto.user;

import com.xigeandwillian.parkingsystem.common.utils.RegexUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CodeDTO {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = RegexUtils.PHONE_REGEX, message = "手机号格式错误")
    private String phone;
}
