package com.jjg.game.slots.game.hulk.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.hulk.pb.HulkWinIconInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/1/15
 */
public class HulkGameRunInfo extends GameRunInfo<HulkPlayerGameData> {
    //中奖线信息
    private List<HulkWinIconInfo> awardLineInfos;
    //汽车赢取的奖励
    private List<Long> carsWinGold;
    //飞机的倍数
    private int airplane;
    //汽车游戏是否结束
    private boolean carOver;

    public HulkGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public List<HulkWinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<HulkWinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }

    public List<Long> getCarsWinGold() {
        return carsWinGold;
    }

    public void setCarsWinGold(List<Long> carsWinGold) {
        this.carsWinGold = carsWinGold;
    }

    public int getAirplane() {
        return airplane;
    }

    public void setAirplane(int airplane) {
        this.airplane = airplane;
    }

    public boolean isCarOver() {
        return carOver;
    }

    public void setCarOver(boolean carOver) {
        this.carOver = carOver;
    }
}
