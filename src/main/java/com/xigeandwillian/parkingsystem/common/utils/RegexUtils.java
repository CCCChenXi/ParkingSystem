package com.xigeandwillian.parkingsystem.common.utils;

public class RegexUtils {
    public static boolean isValidPhone(String phone){
        return phone!=null&&phone.matches("^1[3-9]\\d{9}$");
    }
}
