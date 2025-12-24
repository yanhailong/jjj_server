package com.jjg.game.slots.game.moneyrabbit.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.goldsnakefortune.pb.GoldSnakeFortuneWinIconInfo;
import com.jjg.game.slots.game.moneyrabbit.pb.MoneyRabbitWinIconInfo;

import java.util.List;

public class MoneyRabbitGameRunInfo extends GameRunInfo<MoneyRabbitPlayerGameData> {
    //中奖线信息
    private List<MoneyRabbitWinIconInfo> awardLineInfos;


    public MoneyRabbitGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public List<MoneyRabbitWinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<MoneyRabbitWinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }
}
