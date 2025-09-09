package com.jjg.game.slots.game.dollarexpress.data;


import java.util.ArrayList;
import java.util.List;

/**
 * 美元信息
 * @author 11
 * @date 2025/7/8 18:12
 */
public class DollarInfo implements Cloneable{
    //美元现金奖励倍数
    private int dollarCashTimes;
    //美元图标的倍数
    private List<Integer> dollarTimesList;

    public int getDollarCashTimes() {
        return dollarCashTimes;
    }

    public void setDollarCashTimes(int dollarCashTimes) {
        this.dollarCashTimes = dollarCashTimes;
    }

    public List<Integer> getDollarTimesList() {
        return dollarTimesList;
    }

    public void setDollarTimesList(List<Integer> dollarTimesList) {
        this.dollarTimesList = dollarTimesList;
    }

    public void addDollarTimes(int dollarTimes) {
        if(this.dollarTimesList == null) {
            this.dollarTimesList = new ArrayList<>();
        }
        this.dollarTimesList.add(dollarTimes);
    }

    @Override
    public DollarInfo clone() throws CloneNotSupportedException {
        try {
            DollarInfo cloned = (DollarInfo) super.clone();
            if(this.dollarTimesList != null && !this.dollarTimesList.isEmpty()) {
                cloned.dollarTimesList = new ArrayList<>(this.dollarTimesList);
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
