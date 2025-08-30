package com.jjg.game.slots.data;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/30 11:44
 */
public class SpecialAuxiliaryInfo {
    private int id;
    //免费游戏
    //private
    //奖励
    private List<SpecialAuxiliaryAwardInfo> awardInfos;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<SpecialAuxiliaryAwardInfo> getAwardInfos() {
        return awardInfos;
    }

    public void setAwardInfos(List<SpecialAuxiliaryAwardInfo> awardInfos) {
        this.awardInfos = awardInfos;
    }
}
