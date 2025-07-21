package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;

/**
 * 玩家游戏数据
 * @author 11
 * @date 2025/6/10 18:07
 */
public class DollarExpressPlayerGameData extends SlotsPlayerGameData {

    //最近一次的押注
    private long lastBet;
    //记录lib中againGameMap中的key
    private int lastAgainGameIndex = -1;
    //记录lib中freeGameMap中的key
    private int lastFreeGameIndex = -1;
    //获取的结果库
    private DollarExpressResultLib lib;
    //累计的美钞数量
    private int totalDollars;


    public long getLastBet() {
        return lastBet;
    }

    public void setLastBet(long lastBet) {
        this.lastBet = lastBet;
    }

    public int getLastAgainGameIndex() {
        return lastAgainGameIndex;
    }

    public void setLastAgainGameIndex(int lastAgainGameIndex) {
        this.lastAgainGameIndex = lastAgainGameIndex;
    }

    public int getLastFreeGameIndex() {
        return lastFreeGameIndex;
    }

    public void setLastFreeGameIndex(int lastFreeGameIndex) {
        this.lastFreeGameIndex = lastFreeGameIndex;
    }

    public DollarExpressResultLib getLib() {
        return lib;
    }

    public void setLib(DollarExpressResultLib lib) {
        this.lib = lib;
    }



    public void addLastAgainGameIndex(){
        this.lastAgainGameIndex++;
    }
    public void addLastFreeGameIndex(){
        this.lastFreeGameIndex++;
    }

    public int getTotalDollars() {
        return totalDollars;
    }

    public void setTotalDollars(int totalDollars) {
        this.totalDollars = totalDollars;
    }

    public void addDollasCount(int count){
        this.totalDollars += count;
    }
}
