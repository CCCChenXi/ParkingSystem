package com.xigeandwillian.parkingsystem;

import org.springframework.beans.BeanUtils;

public class TestReflection {

    public static void main(String[] args) {
        User user = new User();

        People people = new People();
        people.setName("xige");
        people.setYear("22");
        BeanUtils.copyProperties(people,user);
    }

}
