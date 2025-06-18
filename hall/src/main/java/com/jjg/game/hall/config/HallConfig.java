package com.jjg.game.hall.config;

import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/7 10:24
 */
@Component
public class HallConfig {
    private boolean test;

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }
}
