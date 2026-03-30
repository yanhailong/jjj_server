package com.jjg.game.slots.game.angrybirds.data;

import com.jjg.game.slots.data.SlotsResultLib;
import com.jjg.game.slots.game.angrybirds.pb.bean.AngryBirdsReplaceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lm
 * @date 2025/12/2 17:30
 */
public class AngryBirdsResultLib extends SlotsResultLib<AngryBirdsAwardLineInfo> {
    //免费基础倍率
    private int freeMultiplier;

    //替换图标
    private List<AngryBirdsReplaceInfo> replaceInfoList;

    public int getFreeMultiplier() {
        return freeMultiplier;
    }

    public void setFreeMultiplier(int freeMultiplier) {
        this.freeMultiplier = freeMultiplier;
    }

    public List<AngryBirdsReplaceInfo> getReplaceInfoList() {
        return replaceInfoList;
    }

    public void setReplaceInfoList(List<AngryBirdsReplaceInfo> replaceInfoList) {
        this.replaceInfoList = replaceInfoList;
    }

    public void addAngryBirdsReplaceInfo(AngryBirdsReplaceInfo replaceInfo) {
        if (replaceInfoList == null) {
            replaceInfoList = new ArrayList<>();
        }
        replaceInfoList.add(replaceInfo);
    }
}
