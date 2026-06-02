package com.jjg.game.slots.game.bountyduel.data;

import com.jjg.game.slots.data.FullAwardLineInfo;

import java.util.Set;

/**
 * 赏金大对决中奖线信息。
 */
public class BountyDuelAwardLineInfo extends FullAwardLineInfo {

    /**
     * 金框图标消除后转 wild 的坐标集合。
     */
    private Set<Integer> replaceWildIndexs;

    public Set<Integer> getReplaceWildIndexs() {
        return replaceWildIndexs;
    }

    public void setReplaceWildIndexs(Set<Integer> replaceWildIndexs) {
        this.replaceWildIndexs = replaceWildIndexs;
    }
}
