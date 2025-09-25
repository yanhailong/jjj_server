package com.jjg.game.core.config.bean;

import com.jjg.game.core.config.AbstractExcelConfig;
import com.jjg.game.core.config.ExcelConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 积分大奖的签到配置
 */
@ExcelConfig(name = "PointsAwardSignin")
public class PointsAwardSignIn extends AbstractExcelConfig {

    /**
     * 签到天数,根据当月的天数，实时计算出读取多个天的配置即可
     */
    private int day;

    /**
     * 奖励
     */
    private List<Integer> getItem = new ArrayList<>();

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public List<Integer> getGetItem() {
        return getItem;
    }

    public void setGetItem(List<Integer> getItem) {
        this.getItem = getItem;
    }
}
