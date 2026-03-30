package com.jjg.game.slots.data;


import java.util.Set;

/**
 * 中奖线信息
 * @author 11
 * @date 2025/6/17 14:25
 */
public class FullAwardLineInfo extends AwardLineInfo{
    //中奖线的倍数
    private int baseTimes;
    //相同元素的坐标id
    private Set<Integer> sameIconSet;
    //元素id
    private int sameIcon;

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

}
