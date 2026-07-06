package com.jjg.game.slots.game.superGolf.data;

import com.jjg.game.slots.data.FullAwardLineInfo;

import java.util.Set;

public class SuperGolfAwardLineInfo extends FullAwardLineInfo {
    //本中奖线里参与中奖的"带框符号"坐标集合。cascade 时这些位置会被转换为神秘符号（id=114），
    //而不是被消除。
    private Set<Integer> boxedToMysteryIndexes;

    public Set<Integer> getBoxedToMysteryIndexes() {
        return boxedToMysteryIndexes;
    }

    public void setBoxedToMysteryIndexes(Set<Integer> boxedToMysteryIndexes) {
        this.boxedToMysteryIndexes = boxedToMysteryIndexes;
    }
}
