package com.jjg.game.slots.data;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/8/30 11:44
 */
public class SpecialAuxiliaryInfo {
    private int cfgId;
    //免费游戏
    private List<JSONObject> freeGames;
    //奖励
    private List<SpecialAuxiliaryAwardInfo> awardInfos;

    public int getCfgId() {
        return cfgId;
    }

    public void setCfgId(int cfgId) {
        this.cfgId = cfgId;
    }

    public List<JSONObject> getFreeGames() {
        return freeGames;
    }

    public void setFreeGames(List<JSONObject> freeGames) {
        this.freeGames = freeGames;
    }

    public List<SpecialAuxiliaryAwardInfo> getAwardInfos() {
        return awardInfos;
    }

    public void setAwardInfos(List<SpecialAuxiliaryAwardInfo> awardInfos) {
        this.awardInfos = awardInfos;
    }

    public void addAwardInfo(SpecialAuxiliaryAwardInfo awardInfo) {
        if (this.awardInfos == null) {
            this.awardInfos = new ArrayList<>();
        }
        this.awardInfos.add(awardInfo);
    }

    public void addFreeGame(JSONObject game) {
        if (this.freeGames == null) {
            this.freeGames = new ArrayList<>();
        }
        this.freeGames.add(game);
    }
}
