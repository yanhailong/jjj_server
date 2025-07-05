package com.jjg.game.slots.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 中奖线信息
 * @author 11
 * @date 2025/6/17 14:25
 */
public class AwardLineInfo {
    //线的id
    private int id;
    //图标id
    private int iconId;
    //这条线的基础倍数
    private int baseTimes;
    //这条线上相同的个数
    private int sameCount;
    private Map<Integer, List<SpecialAwardInfo>> specialAwardInfoMap;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public int getBaseTimes() {
        return baseTimes;
    }

    public void setBaseTimes(int baseTimes) {
        this.baseTimes = baseTimes;
    }

    public int getSameCount() {
        return sameCount;
    }

    public void setSameCount(int sameCount) {
        this.sameCount = sameCount;
    }

    public Map<Integer, List<SpecialAwardInfo>> getSpecialAwardInfoMap() {
        return specialAwardInfoMap;
    }

    public void setSpecialAwardInfoMap(Map<Integer, List<SpecialAwardInfo>> specialAwardInfoMap) {
        this.specialAwardInfoMap = specialAwardInfoMap;
    }

    public void addSpecialAwardInfo(int iconId, SpecialAwardInfo specialAwardInfo) {
        if(this.specialAwardInfoMap == null){
            this.specialAwardInfoMap = new HashMap<>();
        }

        this.specialAwardInfoMap.computeIfAbsent(iconId, k -> new ArrayList<>()).add(specialAwardInfo);
    }
}
