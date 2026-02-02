package com.jjg.game.slots.game.elephantgod.data;

import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class ElephantGodResultLib extends SlotsResultLib<ElephantGodAwardLineInfo> {
    //基础乘倍数
    private int basicMultiplier;
    //当前结算后wild数量
    private int wildCount;
    //增加的免费游戏次数
    private int addFreeCount;

    public int getBasicMultiplier() {
        return basicMultiplier;
    }

    public void setBasicMultiplier(int basicMultiplier) {
        this.basicMultiplier = basicMultiplier;
    }

    public int getWildCount() {
        return wildCount;
    }

    public void setWildCount(int wildCount) {
        this.wildCount = wildCount;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }
}
