package com.jjg.game.slots.game.hotfootball.data;

import com.jjg.game.slots.data.FullAwardLineInfo;

import java.util.Set;

/**
 * @author 11
 * @date 2025/8/1 17:31
 */
public class HotFootballAwardLineInfo extends FullAwardLineInfo {

    //替换成wild的坐标
    private Set<Integer> replaceWildIndexs;

    public Set<Integer> getReplaceWildIndexs() {
        return replaceWildIndexs;
    }

    public void setReplaceWildIndexs(Set<Integer> replaceWildIndexs) {
        this.replaceWildIndexs = replaceWildIndexs;
    }
}
