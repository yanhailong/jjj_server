package com.jjg.game.ploy.games.airraid.data;

import com.jjg.game.ploy.games.airraid.pb.AirRaidBetInfo;
import com.jjg.game.ploy.games.airraid.pb.AirRaidPlayerInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 当前回合的下注展示簿。
 */
public class AirRaidRoundBetBook {

    private final Map<Long, AirRaidPlayerInfo> betSlotInfoMap = new ConcurrentHashMap<>();

    public void clear() {
        betSlotInfoMap.clear();
    }

    public void recordBet(long playerId, int headImgId, int betIndex, long betAmount) {
        AirRaidPlayerInfo info = new AirRaidPlayerInfo();
        info.playerId = playerId;
        info.headImgId = headImgId;
        info.betIndex = betIndex;
        info.bet = betAmount;
        betSlotInfoMap.put(buildKey(playerId, betIndex), info);
    }

    public void recordCashOut(long playerId, int betIndex, int cashOutMultiplier, long winAmount) {
        AirRaidPlayerInfo info = betSlotInfoMap.get(buildKey(playerId, betIndex));
        if (info == null) {
            return;
        }
        info.cashedOut = true;
        info.cashOutMultiplier = cashOutMultiplier;
        info.winAmount = winAmount;
    }

    public List<AirRaidBetInfo> buildBetInfoList() {
        List<AirRaidBetInfo> list = new ArrayList<>(betSlotInfoMap.size());
        betSlotInfoMap.values().stream()
                .sorted(Comparator.comparingLong((AirRaidPlayerInfo info) -> info.playerId)
                        .thenComparingInt(info -> info.betIndex))
                .forEach(info -> {
                    AirRaidBetInfo betInfo = new AirRaidBetInfo();
                    betInfo.playerId = info.playerId;
                    betInfo.headImgId = info.headImgId;
                    betInfo.betIndex = info.betIndex;
                    betInfo.bet = info.bet;
                    betInfo.cashedOut = info.cashedOut;
                    betInfo.times = info.cashOutMultiplier;
                    betInfo.win = info.winAmount;
                    list.add(betInfo);
                });
        return list;
    }

    private long buildKey(long playerId, int betIndex) {
        return playerId * 10 + betIndex;
    }
}
