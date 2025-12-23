package com.jjg.game.slots.game.basketballSuperstar.data;

import com.jjg.game.slots.data.GameRunInfo;

import java.util.Set;

/**
 * @author lihaocao
 * @date 2025/12/2 17:55
 */
public class BasketballSuperstarGameRunInfo extends GameRunInfo<BasketballSuperstarPlayerGameData> {
    //根据权重选取 变成wild 图标 免费转结束，才取消
    private int stickyIcon;
    //免费转  图标变成wild  变化的图案， key -> 图标id
    private Set<Integer> changeStickyIconSet;

    public int getStickyIcon() {
        return stickyIcon;
    }

    public void setStickyIcon(int stickyIcon) {
        this.stickyIcon = stickyIcon;
    }

    public Set<Integer> getChangeStickyIconSet() {
        return changeStickyIconSet;
    }

    public void setChangeStickyIconSet(Set<Integer> changeStickyIconSet) {
        this.changeStickyIconSet = changeStickyIconSet;
    }

    public BasketballSuperstarGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }
}
