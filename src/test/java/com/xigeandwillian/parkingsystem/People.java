package com.xigeandwillian.parkingsystem;

public class People {

    private String name;
    private String year;

    public String getName() {
        System.out.println("正在获取name");
        return name;
    }

    private String getYear() {
        System.out.println("正在获取year");
        return year;
    }

    public String getXige(){
        System.out.println("God xige");
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setYear(String year) {
        this.year = year;
    }

}
