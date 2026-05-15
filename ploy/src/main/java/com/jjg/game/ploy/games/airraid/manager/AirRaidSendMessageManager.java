package com.jjg.game.ploy.games.airraid.manager;

import com.jjg.game.ploy.games.airraid.data.AirRaidGameRoom;
import com.jjg.game.ploy.games.airraid.data.AirRaidPhase;
import com.jjg.game.ploy.games.airraid.data.AirRaidPlayerPloyGameData;
import com.jjg.game.ploy.games.airraid.pb.*;
import com.jjg.game.ploy.games.airraid.pb.cluster.GameStateSync;
import com.jjg.game.ploy.manager.AbstractPloySendMesageManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * @author 11
 * @date 2026/5/13
 */
@Component
public class AirRaidSendMessageManager extends AbstractPloySendMesageManager<AirRaidPlayerPloyGameData, AirRaidGameRoom> {

    /**
     * 通知所有客户端，当前的游戏阶段
     *
     * @param gameDataMap
     * @param msg
     */
    public void notifyGameState(Map<Long, AirRaidPlayerPloyGameData> gameDataMap, GameStateSync msg) {
        broadcastLocalPlayers(gameDataMap, buildGameStateResponse(msg));
    }

    /**
     * 构建游戏阶段消息
     *
     * @param msg
     * @return
     */
    private NotifyAirRaidGameState buildGameStateResponse(GameStateSync msg) {
        NotifyAirRaidGameState res = new NotifyAirRaidGameState();
        res.phase = msg.phase;
        AirRaidPhase phase = AirRaidPhase.fromCode(msg.phase);
        res.crashMultiplier = AirRaidGameRoom.clientCrashMultiplier(phase, msg.crashMultiplier);
        res.startTime = msg.phaseStartTime;
        res.stopTime = AirRaidGameRoom.clientStopTime(phase, msg.stopTime);
        return res;
    }

    /**
     * 把本节点 pending 队列里的兑现合并推送给本地玩家
     */
    public void flushCashOutQueue(Map<Long, AirRaidPlayerPloyGameData> gameDataMap, Queue<AirRaidCashOutInfo> pendingCashOuts) {
        if (pendingCashOuts.isEmpty()) {
            return;
        }
        List<AirRaidCashOutInfo> batch = new ArrayList<>();
        AirRaidCashOutInfo item;
        while ((item = pendingCashOuts.poll()) != null) {
            batch.add(item);
        }
        if (batch.isEmpty()) {
            return;
        }
        NotifyAirRaidCashOut notice = new NotifyAirRaidCashOut();
        notice.playerCashOuts = batch;
        broadcastLocalPlayers(gameDataMap, notice);
    }

    /**
     * 把本节点 pending 队列里的押注推送给本地玩家
     */
    public void flushBetQueue(Map<Long, AirRaidPlayerPloyGameData> gameDataMap, Queue<AirRaidPlayerInfo> pendingBets) {
        if (pendingBets.isEmpty()) {
            return;
        }
        List<AirRaidPlayerInfo> batch = new ArrayList<>();
        AirRaidPlayerInfo item;
        while ((item = pendingBets.poll()) != null) {
            batch.add(item);
        }
        if (batch.isEmpty()) {
            return;
        }
        NotifyAirRaidBet notice = new NotifyAirRaidBet();
        notice.betInfoList = batch;
        broadcastLocalPlayers(gameDataMap, notice);
    }
}
