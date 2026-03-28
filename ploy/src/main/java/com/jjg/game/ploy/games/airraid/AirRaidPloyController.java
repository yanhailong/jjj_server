package com.jjg.game.ploy.games.airraid;

import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.ploy.controller.AbstractMultiPloyController;
import com.jjg.game.ploy.data.PloyBetDivideInfo;
import com.jjg.game.ploy.games.airraid.data.*;
import com.jjg.game.ploy.games.airraid.pb.*;
import com.jjg.game.ploy.games.airraid.pb.cluster.BetSync;
import com.jjg.game.ploy.games.airraid.pb.cluster.CashOutSync;
import com.jjg.game.ploy.games.airraid.pb.cluster.CrashSync;
import com.jjg.game.ploy.games.airraid.pb.cluster.GameStateSync;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.AirRaidCfg;
import com.jjg.game.sampledata.bean.PloygameRoomCfg;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 空袭游戏控制器 (Crash 玩法)
 * <p>
 * 主节点负责回合循环: 下注 -> 飞行 -> 坠毁 -> 结算 -> 下一回合
 * 从节点接收集群消息并推送给本地玩家
 * </p>
 *
 * @author 11
 * @date 2026/3/19
 */
@Component
public class AirRaidPloyController extends AbstractMultiPloyController<AirRaidPlayerPloyGameData> {

    private final String TIMER_BETTING_PHASE = "AIR_RAID_BETTING";
    private final String TIMER_MULTIPLIER_UPDATE = "AIR_RAID_MULTIPLIER";
    private final String TIMER_CRASH_SETTLE = "AIR_RAID_CRASH_SETTLE";

    //是否为主节点
    private volatile boolean leader = false;

    //游戏全局状态(所有回合共享)
    private final AirRaidGame game = new AirRaidGame();
    //定时器事件
    private TimerEvent<String> event;
    //所有玩家(包含所有节点)的下注信息
    private Map<Long, AirRaidPlayerInfo> playerBetMap = new ConcurrentHashMap<>();


    public AirRaidPloyController() {
        super(LoggerFactory.getLogger(AirRaidPloyController.class), AirRaidPlayerPloyGameData.class);
    }

    private void addEvent() {
        this.event = new TimerEvent<>(this, "AirRaid", 1).setInitTime(1).withTimeUnit(TimeUnit.MINUTES);
        this.timerCenter.add(this.event);
    }

    // ==================== 主节点选举回调 ====================

    @Override
    public void isLeader() {
        log.info("AirRaid 当选为主节点，启动游戏循环");
        leader = true;
        addEvent();
    }

    @Override
    public void notLeader() {
        log.info("AirRaid 失去主节点身份，停止定时器");
        leader = false;
        this.timerCenter.remove(this.event);
    }

    // ==================== 回合生命周期 (主节点) ====================

    private void startGameLoop() {
        //获取当前阶段
        switch (game.getPhase()) {
            case BETTING:
        }
    }

    /**
     * 进入下注阶段
     */
    private void startBettingPhase() {
        // 生成本局坠毁倍率
        int crashMultiplier = AirRaidCrashCalculator.generateCrashMultiplier();
        game.startNewRound(crashMultiplier);
        log.info("AirRaid 新回合开始, crashMultiplier={}", crashMultiplier);

        // 广播游戏状态给所有节点
        broadcastGameState();

        // 设置下注阶段定时器: StageTime[0] 秒后进入飞行阶段
//        AirRaidCfg cfg = getAirRaidCfg();
//        int bettingSeconds = (cfg != null && cfg.getStageTime() != null && !cfg.getStageTime().isEmpty()) ? cfg.getStageTime().get(0) : 5;

    }

    /**
     * 进入飞行阶段
     */
    private void startFlyingPhase() {
        if (!leader) return;

        game.startFlying();
        log.info("AirRaid 飞行阶段开始");

        // 广播游戏状态
        broadcastGameState();

        // 启动倍率更新定时器 (每200ms更新一次)
//        multiplierUpdateEvent = new TimerEvent<>(this, TIMER_MULTIPLIER_UPDATE, 200);
//        timerCenter.add(multiplierUpdateEvent);
    }

    /**
     * 更新倍率 (飞行阶段, 主节点每200ms调用)
     */
    private void updateMultiplier() {
        if (!leader || game.getPhase() != AirRaidPhase.FLYING) return;

//        AirRaidCfg cfg = getAirRaidCfg();
//        int growthRate = (cfg != null) ? cfg.getGrowthmultiplier() : 1000;
//        long elapsedMs = System.currentTimeMillis() - game.getFlyStartTime();
//        int currentMultiplier = AirRaidCrashCalculator.calculateCurrentMultiplier(elapsedMs, growthRate);
//        game.setCurrentMultiplier(currentMultiplier);
//
//        // 检查是否达到坠毁倍率
//        if (currentMultiplier >= game.getCrashMultiplier()) {
//            crashPhase();
//        }
    }

    /**
     * 坠毁阶段
     */
    private void crashPhase() {
        if (!leader) return;

        // 停止倍率更新定时器
        stopMultiplierTimer();

        game.crash();
        log.info("AirRaid 坠毁, crashMultiplier={}", game.getCrashMultiplier());

        // 广播坠毁到所有节点
        broadcastCrash();

        // 设置结算阶段定时器: StageTime[1] 秒后进入下一回合
//        AirRaidCfg cfg = getAirRaidCfg();
//        int settleSeconds = (cfg != null && cfg.getStageTime() != null && cfg.getStageTime().size() > 1) ? cfg.getStageTime().get(1) : 3;
//        TimerEvent<String> settleTimer = new TimerEvent<>(this, settleSeconds * 1000, TIMER_CRASH_SETTLE);
//        timerCenter.add(settleTimer);
    }

    // ==================== 定时器回调 ====================

    @Override
    public void onTimer(TimerEvent<String> e) {
        AirRaidPhase phase = game.getPhase();
        long now = System.currentTimeMillis();
        if (phase == AirRaidPhase.BETTING) {
            if (game.getPhaseStartTime() < 1) {
                game.setPhaseStartTime(now);
            } else {
                //获取时间差
                long diff = now - game.getPhaseStartTime();
                //检查该阶段是否结束
                if (diff >= AirRaidConstant.Common.BET_PHASE_TIME_MILLS) {
                    game.setPhase(AirRaidPhase.BETTING_END_BET);
                    game.setPhaseStartTime(now);
                }
            }
        } else if (phase == AirRaidPhase.BETTING_END_BET) {
            //获取时间差
            long diff = now - game.getPhaseStartTime();
            //检查该阶段是否结束
            if (diff >= AirRaidConstant.Common.BET_PHASE_TIME_BEFORE_END_MILLS) {
                game.setPhase(AirRaidPhase.FLYING);
                game.setPhaseStartTime(now);
            }
        } else if (phase == AirRaidPhase.FLYING) {

        } else {
            //获取时间差
            long diff = now - game.getPhaseStartTime();
            //检查该阶段是否结束
            if (diff >= AirRaidConstant.Common.BET_PHASE_TIME_BEFORE_END_MILLS) {
                game.setPhase(AirRaidPhase.FLYING);
                game.setPhaseStartTime(now);
            }
        }

        String param = e.getParameter();
        if (param == null) return;
        switch (param) {
            case TIMER_BETTING_PHASE -> startFlyingPhase();
            case TIMER_MULTIPLIER_UPDATE -> updateMultiplier();
            case TIMER_CRASH_SETTLE -> startBettingPhase();
            default -> log.warn("AirRaid 未知定时器参数: {}", param);
        }
    }

    private void stopMultiplierTimer() {
//        if (multiplierUpdateEvent != null) {
//            timerCenter.remove(multiplierUpdateEvent);
//            multiplierUpdateEvent = null;
//        }
    }

    private void stopAllTimers() {
        stopMultiplierTimer();
        timerCenter.remove(this);
    }

    // ==================== 进入/退出游戏 ====================

    @Override
    protected AbstractResponse buildResEnterGameMessage(int code, int gameType, int roomCfgId, AirRaidPlayerPloyGameData playerGameData) {
        ResAirRaidEnterGame res = new ResAirRaidEnterGame(code);
        if (code != Code.SUCCESS || playerGameData == null) {
            return res;
        }

        // 构建返回消息
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        res.stakeList = cfg.getLineBetScore();
        res.betInfoList = buildBetInfoList();
        res.roundHistory = game.getRoundHistoryList();

        // 填充游戏状态快照字段
        res.phase = game.getPhase().getCode();
        res.currentMultiplier = game.getCurrentMultiplier();
        return res;
    }

    @Override
    protected AbstractMessage buildResBetMessage(int code, AirRaidPlayerPloyGameData playerGameData, int oddsType) {
        // 空袭游戏不使用标准 bet 流程，由 airRaidBet 方法处理
        return null;
    }

    /**
     * 空袭游戏下注
     *
     * @param playerController 玩家控制器
     * @param bet              下注金额
     * @param betIndex         注单索引(0或1)
     * @return 下注响应
     */
    @Override
    public ResAirRaidBet bet(PlayerController playerController, long bet, int betIndex) {
        ResAirRaidBet res = new ResAirRaidBet(Code.SUCCESS);
        try {
            // 验证游戏阶段
            if (this.game.getPhase() != AirRaidPhase.BETTING) {
                res.code = Code.FAIL;
                log.warn("AirRaid 非下注阶段，下注失败 playerId={}, phase={}", playerController.playerId(), this.game.getPhase());
                return res;
            }

            // 验证注单索引
            if (betIndex < 0 || betIndex > 1) {
                res.code = Code.PARAM_ERROR;
                log.warn("AirRaid 注单索引错误 playerId={}, betIndex={}", playerController.playerId(), betIndex);
                return res;
            }

            // 获取玩家游戏数据
            AirRaidPlayerPloyGameData playerGameData = getPlayerGameData(playerController.playerId(), this.roomCfgId);
            if (playerGameData == null) {
                res.code = Code.FAIL;
                log.warn("AirRaid 获取 playerGameData 失败 playerId={}", playerController.playerId());
                return res;
            }

            // 验证是否已在此位置下注
            AirRaidBetData airRaidBetData = playerGameData.getAirRaidBetDataMap().get(betIndex);
            if (airRaidBetData != null) {
                res.code = Code.REPEAT_OP;
                log.warn("AirRaid 重复下注 playerId={}, betIndex={},betValue = {}", playerController.playerId(), betIndex, bet);
                return res;
            }

            //检查押分值
            PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
            boolean match = cfg.getLineBetScore().stream().anyMatch(b -> b == bet);
            if (!match) {
                res.code = Code.PARAM_ERROR;
                log.warn("下注额错误，下注失败 playerId = {},roomCfgId = {},betValue = {}", playerController.playerId(), playerGameData.getRoomCfgId(), bet);
                return res;
            }

            // 扣除金额到奖池
            CommonResult<PloyBetDivideInfo> moneyResult = moneyToPool(playerGameData, bet);
            if (!moneyResult.success()) {
                res.code = moneyResult.code;
                return res;
            }

            // 更新活跃时间
            playerGameData.setLastActiveTime(System.currentTimeMillis());
            // 添加注单
            playerGameData.addBetValue(betIndex, betIndex);
            AirRaidPlayerInfo airRaidPlayerInfo = this.playerBetMap.computeIfAbsent(playerController.playerId(), k -> {
                AirRaidPlayerInfo info = new AirRaidPlayerInfo();
                info.playerId = playerController.playerId();
                info.headImgId = playerController.getPlayer().getHeadImgId();
                return info;
            });
            airRaidPlayerInfo.bet += bet;

            playerGameData.setBeforeMoney(moneyResult.data.getPlayerBeforeMoney());
            playerGameData.setAfterMoney(moneyResult.data.getPlayerAfterMoney());

            // 构建返回消息
            res.betInfoList = buildBetInfoList();

            // 广播下注
            BetSync syncMsg = new BetSync();
            syncMsg.playerId = playerController.playerId();
            syncMsg.betAmount = bet;
            syncMsg.betIndex = betIndex;
            messageSync(syncMsg, true);

            log.info("AirRaid 下注成功 playerId={}, betAmount={}, betIndex={}", playerController.playerId(), bet, betIndex);
        } catch (Exception e) {
            log.error("AirRaid 下注异常", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 空袭游戏兑现
     *
     * @param playerController 玩家控制器
     * @param betIndex         注单索引(0或1)
     * @return 兑现响应
     */
    public ResAirRaidCashOut cashOut(PlayerController playerController, int betIndex) {
        ResAirRaidCashOut res = new ResAirRaidCashOut(Code.SUCCESS);
        try {
            // 验证游戏阶段
            if (game.getPhase() != AirRaidPhase.FLYING) {
                res.code = Code.FAIL;
                log.warn("AirRaid 非飞行阶段，兑现失败 playerId={}, phase={}", playerController.playerId(), game.getPhase());
                return res;
            }

            // 获取玩家游戏数据
            AirRaidPlayerPloyGameData playerGameData = getPlayerGameData(playerController.playerId(), playerController.getPlayer().getRoomCfgId());
            if (playerGameData == null) {
                res.code = Code.FAIL;
                log.warn("AirRaid 获取 playerGameData 失败 playerId={}", playerController.playerId());
                return res;
            }

            // 验证注单是否存在
            AirRaidBetData airRaidBetData = playerGameData.getAirRaidBetDataMap().get(betIndex);
            if (airRaidBetData == null) {
                res.code = Code.NOT_FOUND;
                log.warn("AirRaid 注单不存在 playerId={}, betIndex={}", playerController.playerId(), betIndex);
                return res;
            }

            // 验证是否已兑现
            if (airRaidBetData.isCashedOut()) {
                res.code = Code.REPEAT_OP;
                log.warn("AirRaid 重复兑现 playerId={}, betIndex={}", playerController.playerId(), betIndex);
                return res;
            }

            // 兑现: 计算赢得金额
            int currentMultiplier = game.getCurrentMultiplier();
            airRaidBetData.cashOut(currentMultiplier);
            long winAmount = airRaidBetData.getWinAmount();

            // 从奖池给玩家发奖
            AirRaidCfg airRaidCfg = null;
            int taxRate = (airRaidCfg != null) ? airRaidCfg.getWinRatio() : 0;
            CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, winAmount, taxRate);
            if (!winResult.success()) {
                // 回滚兑现状态
                airRaidBetData.setCashedOut(false);
                airRaidBetData.setCashOutMultiplier(0);
                airRaidBetData.setWinAmount(0);
                res.code = winResult.code;
                log.warn("AirRaid 从奖池发奖失败 playerId={}, winAmount={}", playerController.playerId(), winAmount);
                return res;
            }

            // 构建返回消息
            res.playerId = playerController.playerId();
            res.cashOutMultiplier = currentMultiplier;
            res.winAmount = winAmount;
            res.betIndex = betIndex;

            // 广播兑现到所有节点(所有节点均需广播，确保多节点同步)
            broadcastCashOut(playerController.playerId(), currentMultiplier, winAmount, betIndex);

            log.info("AirRaid 兑现成功 playerId={}, multiplier={}, winAmount={}, betIndex={}", playerController.playerId(), currentMultiplier, winAmount, betIndex);
        } catch (Exception e) {
            log.error("AirRaid 兑现异常", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    // ==================== 广播方法 ====================

    /**
     * 广播游戏状态到所有节点
     */
    private void broadcastGameState() {
        GameStateSync syncMsg = new GameStateSync();
        syncMsg.phase = game.getPhase().getCode();
        syncMsg.currentMultiplier = game.getCurrentMultiplier();
        syncMsg.crashMultiplier = game.getCrashMultiplier();
        syncMsg.betInfoList = buildBetInfoList();
        syncMsg.roundHistory = game.getRoundHistoryList();
        messageSync(syncMsg, true);
    }

    /**
     * 广播兑现到所有节点
     */
    private void broadcastCashOut(long playerId, int cashOutMultiplier, long winAmount, int betIndex) {
        CashOutSync syncMsg = new CashOutSync();
        syncMsg.playerId = playerId;
        syncMsg.cashOutMultiplier = cashOutMultiplier;
        syncMsg.winAmount = winAmount;
        syncMsg.betIndex = betIndex;

        messageSync(syncMsg, true);
    }

    /**
     * 广播坠毁到所有节点
     */
    private void broadcastCrash() {
        CrashSync syncMsg = new CrashSync();
        syncMsg.crashMultiplier = game.getCrashMultiplier();
        syncMsg.roundHistory = game.getRoundHistoryList();

        messageSync(syncMsg, true);
    }

    // ==================== 集群消息回调 ====================

    /**
     * 收到主节点的游戏状态同步
     */
    public void onGameStateSync(GameStateSync msg) {
        try {
            log.info("收到游戏状态同步 phase={}, currentMultiplier={}", msg.phase, msg.currentMultiplier);

            // 更新本地游戏状态
            game.setPhase(AirRaidPhase.fromCode(msg.phase));
            game.setCurrentMultiplier(msg.currentMultiplier);
            game.setCrashMultiplier(msg.crashMultiplier);

            // 推送给本地玩家
            ResAirRaidGameState res = new ResAirRaidGameState(Code.SUCCESS);
            res.phase = msg.phase;
            res.remainTime = msg.remainTime;
            res.currentMultiplier = msg.currentMultiplier;
            broadcastLocalPlayers(res);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 收到主节点的下注同步
     */
    public void onBetSync(BetSync msg) {
        try {
            log.info("收到下注同步 playerId={}, betAmount={}, betIndex={}", msg.playerId, msg.betAmount, msg.betIndex);

            // 更新本地注单数据
//            game.addBet(msg.playerId, msg.betIndex, msg.betAmount, msg.headImgId);
//
//            // 推送给本地玩家
//            ResAirRaidBet res = new ResAirRaidBet(Code.SUCCESS);
//            res.betInfoList = buildBetInfoList();
//            broadcastLocalPlayers(res);
        } catch (Exception e) {
            log.error("", e);
        }

    }

    /**
     * 收到主节点的兑现同步
     */
    public void onCashOutSync(CashOutSync msg) {
        try {
            log.info("收到兑现同步 playerId={}, cashOutMultiplier={}, winAmount={}", msg.playerId, msg.cashOutMultiplier, msg.winAmount);

            // 更新本地注单数据
//            AirRaidBetData slot = game.getBetSlot(msg.playerId, msg.betIndex);
//            if (slot != null) {
//                slot.cashOut(msg.cashOutMultiplier);
//            }
//
//            // 推送给本地玩家
//            ResAirRaidCashOut res = new ResAirRaidCashOut(Code.SUCCESS);
//            res.playerId = msg.playerId;
//            res.cashOutMultiplier = msg.cashOutMultiplier;
//            res.winAmount = msg.winAmount;
//            res.betIndex = msg.betIndex;
//            broadcastLocalPlayers(res);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 收到主节点的坠毁同步
     */
    public void onCrashSync(CrashSync msg) {
        try {
            log.info("收到飞机坠毁同步 crashMultiplier={}", msg.crashMultiplier);
            // 更新本地游戏状态
            game.setPhase(AirRaidPhase.CRASHED);
            game.setCurrentMultiplier(msg.crashMultiplier);
            game.setCrashMultiplier(msg.crashMultiplier);

            // 推送给本地玩家
            ResAirRaidCrash res = new ResAirRaidCrash(Code.SUCCESS);
            res.crashMultiplier = msg.crashMultiplier;
            res.roundHistory = msg.roundHistory;
            broadcastLocalPlayers(res);
        } catch (Exception e) {
            log.error("", e);
        }

    }

    // ==================== 辅助方法 ====================

    /**
     * 构建当前所有下注信息列表
     */
    private List<AirRaidBetInfo> buildBetInfoList() {
        List<AirRaidBetInfo> list = new ArrayList<>();
//        for (Map.Entry<Long, AirRaidBetData> entry : game.getBetSlotMap().entrySet()) {
//            long key = entry.getKey();
//            AirRaidBetData slot = entry.getValue();
//            long playerId = key / 10;
//
//            AirRaidBetInfo info = new AirRaidBetInfo();
//            info.playerId = playerId;
//            info.bet = slot.getBetAmount();
//            if (slot.isCashedOut()) {
//                info.times = slot.getCashOutMultiplier();
//                info.win = slot.getWinAmount();
//            }
//
//            // 设置头像ID(从注单数据获取，支持跨节点)
//            info.headImgId = slot.getHeadImgId();
//
//            list.add(info);
//        }
        return list;
    }
}
