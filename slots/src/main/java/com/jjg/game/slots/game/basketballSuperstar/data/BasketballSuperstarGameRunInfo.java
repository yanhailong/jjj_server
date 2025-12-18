package com.jjg.game.slots.game.basketballSuperstar.data;

import com.jjg.game.slots.data.GameRunInfo;

import java.util.Set;

/**
 * @author lihaocao
 * @date 2025/12/2 17:55
 */
public class BasketballSuperstarGameRunInfo extends GameRunInfo<BasketballSuperstarPlayerGameData> {
    public long mini;
    public long minor;
    public long major;
    public long grand;
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

    public long getMini() {
        return mini;
    }

    public void setMini(long mini) {
        this.mini = mini;
    }

    public long getMinor() {
        return minor;
    }

    public void setMinor(long minor) {
        this.minor = minor;
    }

    public long getMajor() {
        return major;
    }

    public void setMajor(long major) {
        this.major = major;
    }

    public long getGrand() {
        return grand;
    }

    public void setGrand(long grand) {
        this.grand = grand;
    }

    public BasketballSuperstarGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }
}
