package com.jjg.game.slots.game.lianHuanDuoBao.data;

import java.util.List;
import java.util.Map;

/**
 * 消除后补齐的图标信息
 *
 * @author lm
 * @date 2026/6/2
 */
public class LianHuanDuoBaoAddIconInfo {
    //添加的图案， 坐标 -> 图标id
    private Map<Integer, Integer> addIconMap;
    //本轮 cascade 检测出来的中奖线
    private List<LianHuanDuoBaoAwardLineInfo> awardLineInfoList;

    public Map<Integer, Integer> getAddIconMap() {
        return addIconMap;
    }

    public void setAddIconMap(Map<Integer, Integer> addIconMap) {
        this.addIconMap = addIconMap;
    }

    public List<LianHuanDuoBaoAwardLineInfo> getAwardLineInfoList() {
        return awardLineInfoList;
    }

    public void setAwardLineInfoList(List<LianHuanDuoBaoAwardLineInfo> awardLineInfoList) {
        this.awardLineInfoList = awardLineInfoList;
    }
}
