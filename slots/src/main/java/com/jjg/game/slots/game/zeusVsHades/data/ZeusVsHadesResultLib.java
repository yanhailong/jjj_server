package com.jjg.game.slots.game.zeusVsHades.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.Map;
import java.util.Set;

/**
 * @author lihaocao
 * @date 2025/12/2 17:30
 */
public class ZeusVsHadesResultLib extends SlotsResultLib<ZeusVsHadesAwardLineInfo> {
    //本次触发的jackpotId
    private int jackpotId;
    //增加的免费次数
    private int addFreeCount;
    //vs 有哈里斯或者宙斯倍数 <列数,倍数>
    private Map<Integer,Integer> vsTimes;
    //替换成wild的坐标 key=>0 表示列替换 1 表示哈里斯替换 value=>坐标set
    private Map<Integer,Set<Integer>> replaceWildIndexs;
    //vs输赢 1宙斯赢 2哈迪斯赢
    private Map<Integer,Integer> vsStatus;

    public Map<Integer, Integer> getVsStatus() {
        return vsStatus;
    }

    public void setVsStatus(Map<Integer, Integer> vsStatus) {
        this.vsStatus = vsStatus;
    }

    public Map<Integer, Integer> getVsTimes() {
        return vsTimes;
    }

    public void setVsTimes(Map<Integer, Integer> vsTimes) {
        this.vsTimes = vsTimes;
    }

    public Map<Integer, Set<Integer>> getReplaceWildIndexs() {
        return replaceWildIndexs;
    }

    public void setReplaceWildIndexs(Map<Integer, Set<Integer>> replaceWildIndexs) {
        this.replaceWildIndexs = replaceWildIndexs;
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
