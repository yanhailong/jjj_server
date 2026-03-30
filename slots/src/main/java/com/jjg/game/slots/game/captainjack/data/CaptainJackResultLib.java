package com.jjg.game.slots.game.captainjack.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lm
 * @date 2025/12/2 17:30
 */
public class CaptainJackResultLib extends SlotsResultLib<CaptainJackAwardLineInfo> {
    //消除补齐的信息
    private List<CaptainJackAddIconInfo> addIconInfos;
    //增加的免费次数
    private int addFreeCount;
    //探宝次数
    private int digTimes;
    //探宝倍率
    private List<Integer> digTimesMultiplier;

    public void addDigTimesMultiplier(int digTimesMultiplier) {
        if (this.digTimesMultiplier == null) {
            this.digTimesMultiplier = new ArrayList<>();
        }
        this.digTimesMultiplier.add(digTimesMultiplier);
    }

    public int getDigTimes() {
        return digTimes;
    }

    public void setDigTimes(int digTimes) {
        this.digTimes = digTimes;
    }

    public List<Integer> getDigTimesMultiplier() {
        return digTimesMultiplier;
    }

    public void setDigTimesMultiplier(List<Integer> digTimesMultiplier) {
        this.digTimesMultiplier = digTimesMultiplier;
    }

    public List<CaptainJackAddIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<CaptainJackAddIconInfo> addIconInfos) {
        this.addIconInfos = addIconInfos;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        int times = iconArr.length / 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < times; j++) {
                stringBuilder.append(iconArr[j * 3 + i + 1])
                        .append("  ");
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}
