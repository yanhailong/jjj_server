package com.jjg.game.ploy.games.airraid.data;

import com.jjg.game.ploy.games.airraid.pb.AirRaidPlayerInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 当前回合的下注展示簿。
 */
public class AirRaidRoundBetBook {

    private final Map<Long, AirRaidPlayerInfo> betSlotInfoMap = new ConcurrentHashMap<>();

    //上一回合的坠毁倍率(万分比)
    private volatile int lastRoundCrashMultiplier;
    //上一回合的玩家下注/兑现快照(已构造为只读副本)
    private volatile List<AirRaidPlayerInfo> lastRoundPlayerInfoList = Collections.emptyList();

    public void clear() {
        betSlotInfoMap.clear();
    }

    /**
     * 把当前回合的数据快照为"上一回合" — 调用方需在 clear() 之前调用
     */
    public void snapshotLastRound(int crashMultiplier) {
        this.lastRoundPlayerInfoList = buildBetInfoList();
        this.lastRoundCrashMultiplier = crashMultiplier;
    }

    public int getLastRoundCrashMultiplier() {
        return lastRoundCrashMultiplier;
    }

    public List<AirRaidPlayerInfo> getLastRoundPlayerInfoList() {
        return lastRoundPlayerInfoList;
    }

    public void recordBet(long playerId, int headImgId, int betIndex, long betAmount,String nick) {
        AirRaidPlayerInfo info = new AirRaidPlayerInfo();
        info.playerId = playerId;
        info.headImgId = headImgId;
        info.betIndex = betIndex;
        info.bet = betAmount;
        info.nick = nick;
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

    public Collection<AirRaidPlayerInfo> getAllBets() {
        return Collections.unmodifiableCollection(betSlotInfoMap.values());
    }

    public List<AirRaidPlayerInfo> buildBetInfoList() {
        List<AirRaidPlayerInfo> list = new ArrayList<>(betSlotInfoMap.size());
        betSlotInfoMap.values().stream()
                .sorted(Comparator.comparingLong((AirRaidPlayerInfo info) -> info.playerId)
                        .thenComparingInt(info -> info.betIndex))
                .forEach(info -> {
                    AirRaidPlayerInfo betInfo = new AirRaidPlayerInfo();
                    betInfo.playerId = info.playerId;
                    betInfo.headImgId = info.headImgId;
                    betInfo.betIndex = info.betIndex;
                    betInfo.bet = info.bet;
                    betInfo.cashedOut = info.cashedOut;
                    betInfo.cashOutMultiplier = info.cashOutMultiplier;
                    betInfo.winAmount = info.winAmount;
                    betInfo.nick = info.nick;
                    list.add(betInfo);
                });
        return list;
    }

    private long buildKey(long playerId, int betIndex) {
        return playerId * 10 + betIndex;
    }
}
