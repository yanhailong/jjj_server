package com.jjg.game.slots.game.frozenThrone.data;

import com.jjg.game.slots.data.AwardLineInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author lihaocao
 * @date 2025/12/2 17:31
 */
public class FrozenThroneAwardLineInfo extends AwardLineInfo {
    //中奖线的倍数
    private int baseTimes;
    //相同元素的坐标id
    private Set<Integer> sameIconSet;
    //元素id
    private int sameIcon;

    /**
     * 线id
     */
    private int lineId;


    public int getSameCount() {
        return sameIconSet == null ? 0 : sameIconSet.size();
    }


    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }


    public int getBaseTimes() {
        return baseTimes;
    }

    public void setBaseTimes(int baseTimes) {
        this.baseTimes = baseTimes;
    }

    public Set<Integer> getSameIconSet() {
        return sameIconSet;
    }

    public void setSameIconSet(Set<Integer> sameIconSet) {
        this.sameIconSet = sameIconSet;
    }

    public int getSameIcon() {
        return sameIcon;
    }

    public void setSameIcon(int sameIcon) {
        this.sameIcon = sameIcon;
    }

    public void addSameIconIndexSet(int icon) {

        if (sameIconSet == null) {
            sameIconSet = new HashSet<>();
        }
        sameIconSet.add(icon);
    }
}
