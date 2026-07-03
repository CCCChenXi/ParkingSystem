package com.xigeandwillian.parkingsystem.common.utils;

public class RegexUtils {
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches(PHONE_REGEX);
    }
}
