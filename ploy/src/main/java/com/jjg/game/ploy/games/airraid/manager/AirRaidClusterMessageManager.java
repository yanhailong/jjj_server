package com.jjg.game.ploy.games.airraid.manager;

import com.jjg.game.ploy.games.airraid.data.*;
import com.jjg.game.ploy.games.airraid.function.EnqueueCashOutFunction;
import com.jjg.game.ploy.games.airraid.pb.AirRaidPlayerInfo;
import com.jjg.game.ploy.games.airraid.pb.cluster.BetSync;
import com.jjg.game.ploy.games.airraid.pb.cluster.CashOutSync;
import com.jjg.game.ploy.games.airraid.pb.cluster.GameStateSync;
import com.jjg.game.ploy.games.airraid.pb.cluster.PlayerCashOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Queue;

/**
 * @author 11
 * @date 2026/5/15
 */
@Component
public class AirRaidClusterMessageManager {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private AirRaidSendMessageManager sendMessageManager;

    /**
     * 收到主节点的游戏状态同步 — 更新本地状态并推送给本地玩家
     */
    public void onGameStateSync(GameStateSync msg, Map<Long, AirRaidPlayerPloyGameData> gameDataMap, AirRaidGameRoom gameRoom,
                                Runnable clearRoundData, Runnable startCashOutTickAndScheduleAutoCashOut,
                                Runnable flushAndStopCashOutTick) {
        try {
//            log.info("收到主节点的游戏状态同步 begin, msg = {}", JSON.toJSONString(msg));
            AirRaidPhase newPhase = AirRaidPhase.fromCode(msg.phase);
            int oldRoundId = gameRoom.getRoundId();

            // 在切换到新一轮 BETTING 之前先 publish 清空, 保证 clear 排在所有新 bet 之前 FIFO 执行
            // (applyAuthoritativeState 一旦把 phase 切到 BETTING, 玩家 bet 就能通过 canBet 检查并被 publish)
            boolean enteringNewRoundBetting = msg.roundId > oldRoundId && newPhase == AirRaidPhase.BETTING;
            if (enteringNewRoundBetting) {
                clearRoundData.run();
            }

            if (!gameRoom.applyAuthoritativeState(msg.roundId, newPhase, msg.phaseStartTime, msg.stopTime, msg.currentMultiplier, msg.crashMultiplier)) {
                return;
            }

            //从节点感知飞行阶段，启停每秒兑现 tick(主节点 onGameStateSync 不会被自己触发，故主节点不受影响)
            if (newPhase == AirRaidPhase.CRASHED) {
                gameRoom.recordCrashHistory(msg.roundId, msg.crashMultiplier);
            }

            if (newPhase == AirRaidPhase.FLYING) {
                startCashOutTickAndScheduleAutoCashOut.run();
            } else {
                //离开飞行阶段时把残留兑现推出去再停 tick
                flushAndStopCashOutTick.run();
            }

            sendMessageManager.notifyGameState(gameDataMap, msg);
        } catch (Exception e) {
            log.error("AirRaid onGameStateSync异常", e);
        }
    }

    /**
     * 收到主节点的下注同步 — 更新本地投注列表并推送
     */
    public void onBetSync(BetSync msg, AirRaidGameRoom gameRoom, AirRaidRoundBetBook roundBetBook, Queue<AirRaidPlayerInfo> pendingBets) {
        try {
//            log.info("收到主节点的下注同步 begin, msg = {}", JSON.toJSONString(msg));
            if (msg.roundId != gameRoom.getRoundId()) {
                return;
            }

            for (AirRaidPlayerInfo airRaidPlayerInfo : msg.playerBetInfoList) {
                roundBetBook.recordBet(airRaidPlayerInfo.playerId, airRaidPlayerInfo.headImgId, airRaidPlayerInfo.betIndex, airRaidPlayerInfo.bet);
                pendingBets.offer(airRaidPlayerInfo);
            }
        } catch (Exception e) {
            log.error("AirRaid onBetSync异常", e);
        }
    }

    /**
     * 收到其他节点的兑现同步 — 更新本地展示簿并加入 pending 队列，等 tick 批量推送给本地玩家
     */
    public void onCashOutSync(CashOutSync msg, AirRaidGameRoom gameRoom, AirRaidRoundBetBook roundBetBook, EnqueueCashOutFunction enqueueCashOut) {
        try {
//            log.info("收到节点的兑现同步 begin, msg = {}", JSON.toJSONString(msg));
            if (msg.roundId != gameRoom.getRoundId()) {
                return;
            }
            if (msg.playerCashOuts == null || msg.playerCashOuts.isEmpty()) {
                return;
            }
            for (PlayerCashOut c : msg.playerCashOuts) {
                roundBetBook.recordCashOut(c.playerId, c.betIndex, c.cashOutMultiplier, c.winAmount);
                enqueueCashOut.apply(c.playerId, c.betIndex, c.cashOutMultiplier, c.winAmount);
            }
        } catch (Exception e) {
            log.error("AirRaid onCashOutSync异常", e);
        }
    }
}
