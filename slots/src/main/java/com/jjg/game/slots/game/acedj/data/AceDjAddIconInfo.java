package com.jjg.game.slots.game.acedj.data;

import java.util.List;
import java.util.Map;

/**
 * @author lihaocao
 * @date 2025/9/8 16:26
 */
public class AceDjAddIconInfo {
    //添加的图案， 坐标 -> 图标id
    private Map<Integer, Integer> addIconMap;
    //奖励
    private List<AceDjAwardLineInfo> awardLineInfoList;
    //中奖线的倍数
    private Map<Integer,Integer> winTimes;
    //中奖后 变更的 倍数
    private List<Integer> wildTimes;

    public List<Integer> getWildTimes() {
        return wildTimes;
    }

    public void setWildTimes(List<Integer> wildTimes) {
        this.wildTimes = wildTimes;
    }

    public Map<Integer, Integer> getWinTimes() {
        return winTimes;
    }

    public void setWinTimes(Map<Integer, Integer> winTimes) {
        this.winTimes = winTimes;
    }

    public Map<Integer, Integer> getAddIconMap() {
        return addIconMap;
    }

    public void setAddIconMap(Map<Integer, Integer> addIconMap) {
        this.addIconMap = addIconMap;
    }

    public List<AceDjAwardLineInfo> getAwardLineInfoList() {
        return awardLineInfoList;
    }

    public void setAwardLineInfoList(List<AceDjAwardLineInfo> awardLineInfoList) {
        this.awardLineInfoList = awardLineInfoList;
    }
}
