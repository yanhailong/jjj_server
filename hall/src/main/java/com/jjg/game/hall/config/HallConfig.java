package com.jjg.game.hall.config;

import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/7 10:24
 */
@Component
public class HallConfig {
    private boolean test;
    private int age;
    private String name;

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
