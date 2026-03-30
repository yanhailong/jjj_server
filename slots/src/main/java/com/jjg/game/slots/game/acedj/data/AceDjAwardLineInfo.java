package com.jjg.game.slots.game.acedj.data;

import com.jjg.game.slots.data.AwardLineInfo;

import java.util.Map;
import java.util.Set;

/**
 * @author lihaocao
 * @date 2025/12/2 17:31
 */
public class AceDjAwardLineInfo extends AwardLineInfo {

    //相同元素的坐标id
    private Set<Integer> sameIconSet;
    //元素id
    private int sameIcon;
    //替换成wild的坐标
    private Set<Integer> replaceWildIndexs;
    //基础倍数
    private int baseTimes;

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

    public Set<Integer> getReplaceWildIndexs() {
        return replaceWildIndexs;
    }

    public void setReplaceWildIndexs(Set<Integer> replaceWildIndexs) {
        this.replaceWildIndexs = replaceWildIndexs;
    }

    public int getBaseTimes() {
        return baseTimes;
    }

    public void setBaseTimes(int baseTimes) {
        this.baseTimes = baseTimes;
    }
}
