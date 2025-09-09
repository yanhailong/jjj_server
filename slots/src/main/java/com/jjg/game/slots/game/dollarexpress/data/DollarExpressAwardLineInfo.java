package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.slots.data.AwardLineInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 美元快递中奖线信息
 * @author 11
 * @date 2025/7/8 9:41
 */
public class DollarExpressAwardLineInfo extends AwardLineInfo {
    //线的id
    protected int id;
    //这条线的基础倍数
    protected int baseTimes;
    //图标id
    protected int iconId;
    //这条线上相同的个数
    protected int sameCount;
    //其他图标加成倍数
    protected Map<Integer, Integer> otherIconAwardInfoMap;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBaseTimes() {
        return baseTimes;
    }

    public void setBaseTimes(int baseTimes) {
        this.baseTimes = baseTimes;
    }

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public int getSameCount() {
        return sameCount;
    }

    public void setSameCount(int sameCount) {
        this.sameCount = sameCount;
    }

    public Map<Integer, Integer> getOtherIconAwardInfoMap() {
        return otherIconAwardInfoMap;
    }

    public void setOtherIconAwardInfoMap(Map<Integer, Integer> otherIconAwardInfoMap) {
        this.otherIconAwardInfoMap = otherIconAwardInfoMap;
    }

    public void addSpecialAwardInfo(int iconId, int times) {
        if(this.otherIconAwardInfoMap == null){
            this.otherIconAwardInfoMap = new HashMap<>();
        }

        this.otherIconAwardInfoMap.put(iconId,times);
    }
}
