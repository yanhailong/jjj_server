package com.jjg.game.slots.game.captainjack.data;

import com.jjg.game.slots.game.christmasBashNight.data.ChristmasBashNightAwardLineInfo;

import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/9/8 16:26
 */
public class CaptainJackAddIconInfo {
    //添加的图案， 坐标 -> 图标id
    private Map<Integer, Integer> addIconMap;
    //奖励
    private List<CaptainJackAwardLineInfo> awardLineInfoList;

    public Map<Integer, Integer> getAddIconMap() {
        return addIconMap;
    }

    public void setAddIconMap(Map<Integer, Integer> addIconMap) {
        this.addIconMap = addIconMap;
    }

    public List<CaptainJackAwardLineInfo> getAwardLineInfoList() {
        return awardLineInfoList;
    }

    public void setAwardLineInfoList(List<CaptainJackAwardLineInfo> awardLineInfoList) {
        this.awardLineInfoList = awardLineInfoList;
    }
}
