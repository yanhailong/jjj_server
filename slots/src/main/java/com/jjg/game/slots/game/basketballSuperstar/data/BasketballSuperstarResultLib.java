package com.jjg.game.slots.game.basketballSuperstar.data;

import com.jjg.game.slots.data.SlotsResultLib;
import com.jjg.game.slots.game.christmasBashNight.data.ChristmasBashNightAddIconInfo;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author lihaocao
 * @date 2025/12/2 17:30
 */
@Document
public class BasketballSuperstarResultLib extends SlotsResultLib<BasketballSuperstarAwardLineInfo> {
    //本次触发的jackpotId
    private int jackpotId;
    //增加的免费次数
    private int addFreeCount;
    //免费转 转了多少局
    private int freeCount;
    //根据权重选取 变成wild 图标 免费转结束，才取消
    private int stickyIcon;
    //免费转  图标变成wild  变化的图案， key -> 图标id
    private Set<Integer> changeStickyIconSet;
    //免费转  图标变成wild  变化的图案， key -> 图标id
    private Set<Integer> addStickyIconSet;

    public int getFreeCount() {
        return freeCount;
    }

    public void setFreeCount(int freeCount) {
        this.freeCount = freeCount;
    }

    public Set<Integer> getAddStickyIconSet() {
        return addStickyIconSet;
    }

    public void setAddStickyIconSet(Set<Integer> addStickyIconSet) {
        this.addStickyIconSet = addStickyIconSet;
    }

    public Set<Integer> getChangeStickyIconSet() {
        return changeStickyIconSet;
    }

    public void setChangeStickyIconSet(Set<Integer> changeStickyIconSet) {
        this.changeStickyIconSet = changeStickyIconSet;
    }

    public int getStickyIcon() {
        return stickyIcon;
    }

    public void setStickyIcon(int stickyIcon) {
        this.stickyIcon = stickyIcon;
    }

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }
}
