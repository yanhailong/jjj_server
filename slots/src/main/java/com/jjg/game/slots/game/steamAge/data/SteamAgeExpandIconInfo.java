package com.jjg.game.slots.game.steamAge.data;


import java.util.List;

/**
 * @author lihaocao
 * @date 2025/9/8 16:26
 */
public class SteamAgeExpandIconInfo {
    //添加的图案， 坐标id
    private List<Integer> addIconList;
    //奖励
    private List<SteamAgeAwardLineInfo> awardLineInfoList;

    public List<Integer> getAddIconList() {
        return addIconList;
    }

    public void setAddIconList(List<Integer> addIconList) {
        this.addIconList = addIconList;
    }

    public List<SteamAgeAwardLineInfo> getAwardLineInfoList() {
        return awardLineInfoList;
    }

    public void setAwardLineInfoList(List<SteamAgeAwardLineInfo> awardLineInfoList) {
        this.awardLineInfoList = awardLineInfoList;
    }
}
