package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.slots.data.AwardLineInfo;
import com.jjg.game.slots.data.BaseLineAwardLineInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 美元快递中奖线信息
 * @author 11
 * @date 2025/7/8 9:41
 */
public class DollarExpressAwardLineInfo extends BaseLineAwardLineInfo {
    //其他图标加成倍数
    protected Map<Integer, Integer> otherIconAwardInfoMap;

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
