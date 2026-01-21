package com.jjg.game.slots.game.thor.data;

import com.jjg.game.slots.data.AwardLineInfo;
import com.jjg.game.slots.data.BaseLineAwardLineInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2025/12/1 18:01
 */
public class ThorAwardLineInfo extends BaseLineAwardLineInfo {

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
