package com.jjg.game.ploy.games.airraid.manager;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.ploy.games.airraid.AirRaidCrashCalculator;
import com.jjg.game.ploy.games.airraid.data.AirRaidBetData;
import com.jjg.game.ploy.games.airraid.data.AirRaidGameRoom;
import com.jjg.game.ploy.games.airraid.data.AirRaidPlayerPloyGameData;
import com.jjg.game.ploy.games.airraid.data.AirRaidRuleConfig;
import com.jjg.game.ploy.games.airraid.function.DoCashOutFunction;
import com.jjg.game.ploy.games.airraid.function.QueryPlayerGameDataFunction;
import com.jjg.game.ploy.games.airraid.pb.ResAirRaidCashOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 管理 AirRaid 自动兑现的单次 timer。
 *
 * <p>注意：cashOut tick 和实际 doCashOut 仍由 AirRaidPloyController 持有，
 * 因为它们依赖 master 判断、机器人兑现、玩家分区线程和派奖入口。</p>
 *
 * @author 11
 * @date 2026/5/15
 */
@Component
public class AirRaidAutoCashOutManager {
    private static final String AUTO_CASH_OUT_PREFIX = "auto:";

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private TimerCenter timerCenter;

    private final Map<String, TimerEvent<String>> autoCashOutTimerMap = new ConcurrentHashMap<>();

    /**
     * 进入飞行阶段后，为本节点所有已下注且有 autoCashOutTarget 快照的注单调度精确 timer。
     */
    public void scheduleAllAutoCashOutTimers(TimerListener<String> listener,
                                             Map<Long, AirRaidPlayerPloyGameData> gameDataMap,
                                             AirRaidGameRoom gameRoom,
                                             AirRaidRuleConfig airRaidRuleConfig) {
        long flyStart = gameRoom.getPhaseStartTime();
        int crashMul = gameRoom.getCrashMultiplier();
        if (flyStart <= 0 || crashMul <= 0) {
            return;
        }
        for (AirRaidPlayerPloyGameData playerGameData : gameDataMap.values()) {
            for (Map.Entry<Integer, AirRaidBetData> e : playerGameData.getAirRaidBetDataMap().entrySet()) {
                if (e.getValue().isCurrentRound(gameRoom.getRoundId())) {
                    scheduleAutoCashOutTimer(listener, gameRoom, airRaidRuleConfig,
                            playerGameData.playerId(), e.getKey(), e.getValue(), flyStart, crashMul);
                }
            }
        }
    }

    private void scheduleAutoCashOutTimer(TimerListener<String> listener,
                                          AirRaidGameRoom gameRoom,
                                          AirRaidRuleConfig airRaidRuleConfig,
                                          long playerId,
                                          int betIndex,
                                          AirRaidBetData betData,
                                          long flyStart,
                                          int crashMul) {
        if (betData == null || betData.isCashedOut()) {
            return;
        }
        int target = betData.getAutoCashOutTarget();
        if (target <= GameConstant.TEN_THOUSAND) {
            return;
        }
        if (target > crashMul) {
            return;
        }

        long fireAt = flyStart + AirRaidCrashCalculator.calculateFlyDuration(target, airRaidRuleConfig.getGrowthRate());
        int delay = (int) Math.max(0, fireAt - System.currentTimeMillis());

        String key = autoCashOutKey(playerId, betIndex);
        cancelAutoCashOutTimer(key);

        String param = AUTO_CASH_OUT_PREFIX + gameRoom.getRoundId() + ":" + playerId + ":" + betIndex;
        TimerEvent<String> ev = new TimerEvent<>(listener, delay, param).withTimeUnit(TimeUnit.MILLISECONDS);
        autoCashOutTimerMap.put(key, ev);
        timerCenter.add(ev);
    }

    /**
     * 自动兑现 timer 触发处理。
     *
     * <p>timer 参数格式: auto:&lt;roundId&gt;:&lt;playerId&gt;:&lt;betIndex&gt;。
     * 先在 timer 线程校验 roundId，再发布到玩家分区线程后再次校验，避免旧 timer 命中新局。</p>
     */
    public void handleAutoCashOutTimer(String param,
                                       AirRaidGameRoom gameRoom,
                                       DoCashOutFunction doCashOutFunction,
                                       QueryPlayerGameDataFunction queryPlayerGameData) {
        try {
            String body = param.substring(AUTO_CASH_OUT_PREFIX.length());
            String[] parts = body.split(":");
            if (parts.length != 3) {
                return;
            }
            final int eventRoundId = Integer.parseInt(parts[0]);
            long playerId = Long.parseLong(parts[1]);
            int betIndex = Integer.parseInt(parts[2]);

            if (eventRoundId != gameRoom.getRoundId()) {
                return;
            }

            autoCashOutTimerMap.remove(autoCashOutKey(playerId, betIndex));

            long now = System.currentTimeMillis();
            PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(playerId, 0, new BaseHandler<String>() {
                @Override
                public void action() {
                    if (eventRoundId != gameRoom.getRoundId()) {
                        return;
                    }
                    AirRaidPlayerPloyGameData playerGameData = queryPlayerGameData.getPlayerGameData(playerId);
                    if (playerGameData == null) {
                        return;
                    }
                    AirRaidBetData betData = playerGameData.getAirRaidBetDataMap().get(betIndex);
                    if (betData == null || betData.isCashedOut()) {
                        return;
                    }
                    int target = betData.getAutoCashOutTarget();
                    if (target <= GameConstant.TEN_THOUSAND) {
                        return;
                    }
                    ResAirRaidCashOut res = doCashOutFunction.doCashOut(playerId, betIndex, target, now, true);
                    PlayerController controller = playerGameData.getPlayerController();
                    if (res.code == Code.SUCCESS && controller != null) {
                        controller.send(res);
                    }
                }
            }.setHandlerParamWithSelf("airraid autoCashOut"));
        } catch (Exception e) {
            log.error("AirRaid 自动兑现 timer 异常 param={}", param, e);
        }
    }

    public boolean isAutoCashOutTimer(String param) {
        return param != null && param.startsWith(AUTO_CASH_OUT_PREFIX);
    }

    public void cancelAutoCashOutTimer(long playerId, int betIndex) {
        cancelAutoCashOutTimer(autoCashOutKey(playerId, betIndex));
    }

    public void cancelAllAutoCashOutTimers() {
        if (autoCashOutTimerMap.isEmpty()) {
            return;
        }
        for (TimerEvent<String> ev : autoCashOutTimerMap.values()) {
            timerCenter.remove(ev);
        }
        autoCashOutTimerMap.clear();
    }

    private void cancelAutoCashOutTimer(String key) {
        TimerEvent<String> ev = autoCashOutTimerMap.remove(key);
        if (ev != null) {
            timerCenter.remove(ev);
        }
    }

    private String autoCashOutKey(long playerId, int betIndex) {
        return playerId + ":" + betIndex;
    }
}
