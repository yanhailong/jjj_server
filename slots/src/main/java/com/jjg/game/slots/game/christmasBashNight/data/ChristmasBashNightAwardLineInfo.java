package com.jjg.game.slots.game.christmasBashNight.data;

import com.jjg.game.slots.data.FullAwardLineInfo;

import java.util.Set;

/**
 * @author lihaocao
 * @date 2025/12/2 17:31
 */
public class ChristmasBashNightAwardLineInfo extends FullAwardLineInfo {

    //替换成wild的坐标
    private Set<Integer> replaceWildIndexs;

    public Set<Integer> getReplaceWildIndexs() {
        return replaceWildIndexs;
    }

    public void setReplaceWildIndexs(Set<Integer> replaceWildIndexs) {
        this.replaceWildIndexs = replaceWildIndexs;
    }
}
