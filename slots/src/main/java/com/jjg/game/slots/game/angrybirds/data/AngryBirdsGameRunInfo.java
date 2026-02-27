package com.jjg.game.slots.game.angrybirds.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.angrybirds.pb.bean.AngryBirdsReplaceInfo;
import com.jjg.game.slots.game.angrybirds.pb.bean.AngryBirdsWinIconInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:55
 */
public class AngryBirdsGameRunInfo extends GameRunInfo<AngryBirdsPlayerGameData> {

    //中奖线信息
    private List<AngryBirdsWinIconInfo> awardLineInfos;
    //替换元素信息
    private List<AngryBirdsReplaceInfo> replaceInfo;
    //免费倍率
    private int freeMultiplier;

    public AngryBirdsGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public int getFreeMultiplier() {
        return freeMultiplier;
    }

    public void setFreeMultiplier(int freeMultiplier) {
        this.freeMultiplier = freeMultiplier;
    }

    public List<AngryBirdsWinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<AngryBirdsWinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }

    public List<AngryBirdsReplaceInfo> getReplaceInfo() {
        return replaceInfo;
    }

    public void setReplaceInfo(List<AngryBirdsReplaceInfo> replaceInfo) {
        this.replaceInfo = replaceInfo;
    }
}
