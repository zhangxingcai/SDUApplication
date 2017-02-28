package com.example.sduapplication.item;

import java.util.Map;

/**
 * Created by lenovo on 2017/2/28.
 */
public class Item {
    private String class1,class2;
    private int time;
    private Map<String, String> cookies = null;

    public String getClass1() {
        return class1;
    }

    public void setClass1(String class1) {
        this.class1 = class1;
    }

    public String getClass2() {
        return class2;
    }

    public void setClass2(String class2) {
        this.class2 = class2;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public Item(String class1, String class2, int time, Map<String, String> cookies) {
        this.class1 = class1;
        this.class2 = class2;

        this.time = time;
        this.cookies = cookies;
    }

    public Item(int time) {

        this.time = time;
    }
}
