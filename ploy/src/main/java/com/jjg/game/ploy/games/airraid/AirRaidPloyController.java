package com.jjg.game.ploy.games.airraid;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.ploy.controller.AbstractMultiPloyController;
import com.jjg.game.ploy.data.PloyBetDivideInfo;
import com.jjg.game.ploy.games.airraid.data.*;
import com.jjg.game.ploy.games.airraid.pb.*;
import com.jjg.game.ploy.games.airraid.pb.cluster.BetSync;
import com.jjg.game.ploy.games.airraid.pb.cluster.CashOutSync;
import com.jjg.game.ploy.games.airraid.pb.cluster.CrashSync;
import com.jjg.game.ploy.games.airraid.pb.cluster.GameStateSync;
import com.jjg.game.ploy.games.luckypoker.pb.ResLuckyPokerBet;
import com.jjg.game.ploy.pb.ReqPloyRecord;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PloygameRoomCfg;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 空袭游戏控制器 (Crash 玩法)
 * <p>
 * 游戏流程: 下注 → 停止下注 → 飞行(倍率增长) → 坠毁结算 → 下一回合
 * <p>
 * 架构:
 * - 由 Hall Master 节点驱动回合循环
 * - 从节点(slots/table/poker)接收集群消息并推送给本地玩家
 * - 玩家操作(下注/兑现)在所属节点执行，通过集群消息同步到其他节点
 * <p>
 * 核心公式:
 * - 倍率增长: Mt = (1+r)^t (r=增长率万分比, t=秒)
 * - 坠毁概率: Pt = p0 + (t*k) (p0=初始坠毁率, k=风险增量, 万分比/秒)
 * </p>
 *
 * @author 11
 * @date 2026/3/19
 */
@Component
public class AirRaidPloyController extends AbstractMultiPloyController<AirRaidPlayerPloyGameData, AirRaidGameRoom> {
    //游戏全局状态(所有回合共享)
    private final AirRaidGameRoom gameRoom = new AirRaidGameRoom();

    //当前房间本回合的下注展示簿，按注单槽位维度维护
    private final AirRaidRoundBetBook roundBetBook = new AirRaidRoundBetBook();

    //游戏循环定时器引用
    private TimerEvent<String> event;

    //已完成本地结算的回合号，避免 crash 同步重复记账
    private volatile int lastSettledRoundId;

    //倍数增长率 r (万分比, 如1200=12%)
    private int growthRate;
    //风险增量 k (万分比/秒, 如60=0.6%)
    private int riskK;
    //初始坠毁率 p0 (万分比, 如300=3%)
    private int crashP0;
    //下注阶段时长(ms)
    private int bettingDurationMs;
    //停止下注时长(ms)
    private int stopBetDurationMs;
    //结算阶段时长(ms)
    private int settleDurationMs;

    private AtomicBoolean init = new AtomicBoolean(false);

    public AirRaidPloyController() {
        super(LoggerFactory.getLogger(AirRaidPloyController.class), AirRaidPlayerPloyGameData.class, AirRaidGameRoom.class);
    }

    @Override
    public void init(int gameType) {
        super.init(gameType);
        init.compareAndSet(false, true);
        if (NodeType.HALL.name().equals(this.clusterSystem.nodeConfig.getType()) && this.marsCurator.isMaster()) {
            start();
        }

//        start();
    }

    /**
     * 获取到主节点权限后开始执行
     */
    private void start() {
        //检查当前的阶段
        AirRaidPhase phase = gameRoom.getPhase();
        if (phase == AirRaidPhase.BETTING) {
            if (gameRoom.getPhaseStartTime() < 1) {
                startNewRound();
            } else {
                long diff = gameRoom.getPhaseStopTime() - System.currentTimeMillis();
                addEvent(AirRaidPhase.BETTING_END_BET, diff < 0 ? 0 : (int) diff);
            }
        } else if (phase == AirRaidPhase.BETTING_END_BET) {
            long diff = gameRoom.getPhaseStopTime() - System.currentTimeMillis();
            addEvent(AirRaidPhase.FLYING, diff < 0 ? 0 : (int) diff);
        } else if (phase == AirRaidPhase.FLYING) {
            long diff = gameRoom.getPhaseStopTime() - System.currentTimeMillis();
            addEvent(AirRaidPhase.CRASHED, diff < 0 ? 0 : (int) diff);
        } else {
            long diff = gameRoom.getPhaseStopTime() - System.currentTimeMillis();
            addEvent(AirRaidPhase.BETTING, diff < 0 ? 0 : (int) diff);
        }
    }

    @Override
    public void isLeader() {
        // 必须是大厅主节点才参与计算
        if (init.get() && NodeType.HALL.name().equals(this.clusterSystem.nodeConfig.getType())) {
            log.info("AirRaid 当选为主节点，启动游戏循环");
            start();
        }
    }

    @Override
    public void notLeader() {
        if (this.event != null) {
            this.timerCenter.remove(this.event);
            this.event = null;
            log.info("AirRaid 不再是主节点，停止游戏循环");
        }
    }

    /**
     * 添加定时器
     */
    private void addEvent(AirRaidPhase phase, int initTime) {
        TimerEvent<String> previousEvent = this.event;
        if (previousEvent != null) {
            this.timerCenter.remove(previousEvent);
        }
        // 创建定时器
        this.event = new TimerEvent<>(this, initTime, phase.name()).withTimeUnit(TimeUnit.MILLISECONDS);
        timerCenter.add(this.event);
    }

    @Override
    public void onTimer(TimerEvent<String> e) {
        String param = e.getParameter();
        if (AirRaidPhase.BETTING.name().equals(param)) {
            startNewRound();
        } else if (AirRaidPhase.BETTING_END_BET.name().equals(param)) {
            handleStopBetPhaseTimerEvent();
        } else if (AirRaidPhase.FLYING.name().equals(param)) {
            handleFlyingPhaseTimerEvent();
        } else if (AirRaidPhase.CRASHED.name().equals(param)) {
            handleCrashedPhaseTimerEvent();
        } else {
            log.warn("AirRaid unknown timer phase={}", param);
        }
    }

    /**
     * 开始新的回合-进入下注阶段
     */
    private void startNewRound() {
        roundBetBook.clear();
        gameRoom.startNewRound();
        gameRoom.setPhaseStopTime(gameRoom.getPhaseStartTime() + bettingDurationMs);
        gameRoom.setNotifyPhase(true);
        broadcastPhaseChange(bettingDurationMs);
        addEvent(AirRaidPhase.BETTING_END_BET, bettingDurationMs);
        // 清空所有本节点玩家的投注数据
        clearLocalPlayerBets();
        log.info("新回合开始 round={}", gameRoom.getRoundCounter().get());
    }

    /**
     * 停止下注阶段
     */
    private void handleStopBetPhaseTimerEvent() {
        long now = System.currentTimeMillis();
        gameRoom.setPhase(AirRaidPhase.BETTING_END_BET);
        gameRoom.setPhaseStartTime(now);
        gameRoom.setPhaseStopTime(now + stopBetDurationMs);
        gameRoom.setNotifyPhase(true);
        broadcastPhaseChange(stopBetDurationMs);
        addEvent(AirRaidPhase.FLYING, stopBetDurationMs);
        log.debug("停止下注");
    }

    /**
     * 飞行阶段
     */
    private void handleFlyingPhaseTimerEvent() {
        //坠毁时间
        int crashTimeSec = AirRaidCrashCalculator.generateCrashTime(crashP0, riskK);
        //坠毁倍率
        int crashMultiplier = AirRaidCrashCalculator.calculateCrashMultiplier(crashTimeSec, growthRate);
        gameRoom.startFlying(crashMultiplier, crashTimeSec);
        gameRoom.setNotifyPhase(true);
        broadcastPhaseChange(0);
        addEvent(AirRaidPhase.CRASHED, crashTimeSec * 1000);
        log.info("飞行阶段， 坠毁时间 = {},坠毁倍率 ={}", crashTimeSec, crashMultiplier / 10000.0);
    }

    /**
     * 坠毁结算阶段
     */
    private void handleCrashedPhaseTimerEvent() {
        doCrash();
        gameRoom.setPhaseStopTime(gameRoom.getPhaseStartTime() + settleDurationMs);
        gameRoom.setNotifyPhase(true);
        settleCurrentRoundIfNeeded();
        //广播阶段变化
        broadcastPhaseChange(settleDurationMs);
//        broadcastCrash();
        addEvent(AirRaidPhase.BETTING, settleDurationMs);
        log.debug("坠毁结算阶段");
    }

    /**
     * 执行坠毁 — 设置最终倍率并进入结算阶段
     */
    private void doCrash() {
        gameRoom.crash();
        log.info("AirRaid 坠毁! round={}, crashMultiplier={}x", gameRoom.getRoundCounter().get(), gameRoom.getCrashMultiplier() / 10000.0);
    }

    /**
     * 结算处理 — 记录所有玩家的投注结果
     * 未兑现的玩家: 投注金额留在奖池(已在下注时扣除)
     * 已兑现的玩家: 奖金已在兑现时发放
     */
    private void doSettle() {
        Map<Long, AirRaidPlayerPloyGameData> playerMap = this.gameDataMap.get(this.roomCfgId);
        if (playerMap == null || playerMap.isEmpty()) {
            return;
        }

        for (AirRaidPlayerPloyGameData playerData : playerMap.values()) {
            for (Map.Entry<Integer, AirRaidBetData> betEntry : playerData.getAirRaidBetDataMap().entrySet()) {
                AirRaidBetData betData = betEntry.getValue();
                try {
                    // 保存每笔注单的游戏记录
                    AirRaidRecord record = new AirRaidRecord();
                    record.setPlayerId(playerData.playerId());
                    record.setRoomCfgId(playerData.getRoomCfgId());
                    record.setBetAmount(betData.getBetAmount());
                    record.setCrashMultiplier(gameRoom.getCrashMultiplier());
                    record.setCashOutMultiplier(betData.getCashOutMultiplier());
                    record.setWinAmount(betData.getWinAmount());
                    record.setCashedOut(betData.isCashedOut());
                    recordDao.saveRecord(record);
                } catch (Exception e) {
                    log.error("AirRaid 保存记录异常 playerId={}", playerData.playerId(), e);
                }
            }
        }
    }

    private void settleCurrentRoundIfNeeded() {
        int roundId = gameRoom.getRoundId();
        if (roundId <= 0 || roundId == lastSettledRoundId) {
            return;
        }
        doSettle();
        lastSettledRoundId = roundId;
    }

    /**
     * 清空本节点所有玩家的当局投注数据
     */
    private void clearLocalPlayerBets() {
        Map<Long, AirRaidPlayerPloyGameData> playerMap = this.gameDataMap.get(this.roomCfgId);
        if (playerMap != null) {
            playerMap.values().forEach(p -> p.getAirRaidBetDataMap().clear());
        }
    }

    @Override
    protected AbstractResponse buildResEnterGameMessage(int code, int gameType, int roomCfgId, AirRaidPlayerPloyGameData playerGameData) {
        ResAirRaidEnterGame res = new ResAirRaidEnterGame(code);
        if (code != Code.SUCCESS || playerGameData == null) {
            return res;
        }

        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }

        // 可用押注列表
        res.stakeList = cfg.getLineBetScore();
        // 当前回合投注信息
        res.betInfoList = roundBetBook.buildBetInfoList();
        // 最近20局历史
        res.roundHistory = gameRoom.getRoundHistoryList();
        // 当前游戏状态
        res.phase = gameRoom.getPhase().getCode();
        res.currentMultiplier = getAuthoritativeCurrentMultiplier(System.currentTimeMillis());

        // 阶段配置信息(客户端用于倒计时展示)
        List<KVInfo> phaseCfgList = new ArrayList<>();
        phaseCfgList.add(new KVInfo(AirRaidPhase.BETTING.getCode(), bettingDurationMs));
        phaseCfgList.add(new KVInfo(AirRaidPhase.BETTING_END_BET.getCode(), stopBetDurationMs));
        phaseCfgList.add(new KVInfo(AirRaidPhase.CRASHED.getCode(), settleDurationMs));
        res.phaseCfgList = phaseCfgList;

        // 当前阶段到期时间(客户端据此计算剩余时间)
        res.phaseStopTime = gameRoom.getPhaseStopTime();

        return res;
    }

    @Override
    protected AbstractResponse buildResBetMessage(int code, AirRaidPlayerPloyGameData playerGameData, long betValue, int value) {
        //这个方法不会被调用
        return null;
    }

    /**
     * 空袭游戏下注
     * <p>
     * 验证流程: 阶段检查 → 索引检查 → 重复下注检查 → 押分值检查 → 扣款 → 记录 → 广播
     * </p>
     *
     * @param playerController 玩家控制器
     * @param bet              下注金额
     * @param betIndex         注单索引(0或1，每人最多2注)
     * @return 下注响应
     */
    @Override
    public ResAirRaidBet bet(PlayerController playerController, long bet, int betIndex) {
        ResAirRaidBet res = new ResAirRaidBet(Code.SUCCESS);
        try {
            long now = System.currentTimeMillis();
            // 验证游戏阶段: 仅下注阶段可投注
            if (!this.gameRoom.canBet(now)) {
                res.code = Code.FAIL;
                log.warn("AirRaid 非下注阶段 playerId={}, phase={}", playerController.playerId(), this.gameRoom.getPhase());
                return res;
            }

            // 验证注单索引: 仅允许0和1
            if (betIndex < 0 || betIndex > 1) {
                res.code = Code.PARAM_ERROR;
                log.warn("AirRaid 注单索引错误 playerId={}, betIndex={}", playerController.playerId(), betIndex);
                return res;
            }

            // 验证押分值是否在配置列表中
            PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(this.roomCfgId);
            if (cfg == null || cfg.getLineBetScore() == null) {
                res.code = Code.SAMPLE_ERROR;
                return res;
            }
            boolean match = cfg.getLineBetScore().stream().anyMatch(b -> b == bet);
            if (!match) {
                res.code = Code.PARAM_ERROR;
                log.warn("AirRaid 下注额不在配置中 playerId={}, bet={}", playerController.playerId(), bet);
                return res;
            }

            // 获取玩家游戏数据
            AirRaidPlayerPloyGameData playerGameData = getPlayerGameData(playerController.playerId(), this.roomCfgId);
            if (playerGameData == null) {
                res.code = Code.FAIL;
                log.warn("AirRaid playerGameData为空 playerId={}", playerController.playerId());
                return res;
            }

            // 检查是否已在此槽位下注(每个槽位只能下一次)
            AirRaidBetData existingBet = playerGameData.getAirRaidBetDataMap().get(betIndex);
            if (existingBet != null) {
                res.code = Code.REPEAT_OP;
                log.warn("AirRaid 重复下注 playerId={}, betIndex={}", playerController.playerId(), betIndex);
                return res;
            }

            // 扣除金额到奖池
            CommonResult<PloyBetDivideInfo> moneyResult = moneyToPool(playerGameData, bet);
            if (!moneyResult.success()) {
                res.code = moneyResult.code;
                return res;
            }

            // 记录下注数据
            playerGameData.setLastActiveTime(now);
            playerGameData.addBetValue(bet, betIndex);

            // 更新本回合展示簿
            roundBetBook.recordBet(playerController.playerId(), playerController.getPlayer().getHeadImgId(), betIndex, bet);

            // 构建响应
            res.betInfoList = roundBetBook.buildBetInfoList();

            // 通过集群消息同步到其他节点，本节点玩家单独推送正式协议消息
            BetSync syncMsg = new BetSync();
            syncMsg.roundId = gameRoom.getRoundId();
            syncMsg.playerId = playerController.playerId();
            syncMsg.headImgId = playerController.getPlayer().getHeadImgId();
            syncMsg.betAmount = bet;
            syncMsg.betIndex = betIndex;
            messageSync(syncMsg);
            broadcastLocalPlayersExcept(res, playerController.playerId());

            log.info("AirRaid 下注成功 playerId={}, bet={}, betIndex={}", playerController.playerId(), bet, betIndex);
        } catch (Exception e) {
            log.error("AirRaid 下注异常", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 空袭游戏兑现
     * <p>
     * 验证流程: 阶段检查 → 注单存在 → 未兑现 → 计算奖金 → 从奖池发奖 → 广播
     * 兑现金额 = betAmount × currentMultiplier / 10000
     * </p>
     *
     * @param playerController 玩家控制器
     * @param betIndex         注单索引(0或1)
     * @return 兑现响应
     */
    public ResAirRaidCashOut cashOut(PlayerController playerController, int betIndex) {
        ResAirRaidCashOut res = new ResAirRaidCashOut(Code.SUCCESS);
        try {
            long now = System.currentTimeMillis();

            // 仅飞行阶段且未到坠毁边界时可兑现
            if (!gameRoom.canCashOut(now)) {
                res.code = Code.FAIL;
                log.warn("AirRaid 非飞行阶段 playerId={}, phase={}", playerController.playerId(), gameRoom.getPhase());
                return res;
            }

            if (betIndex < 0 || betIndex > 1) {
                res.code = Code.PARAM_ERROR;
                return res;
            }

            // 获取玩家游戏数据
            AirRaidPlayerPloyGameData playerGameData = getPlayerGameData(playerController.playerId(), this.roomCfgId);
            if (playerGameData == null) {
                res.code = Code.FAIL;
                log.warn("AirRaid playerGameData为空 playerId={}", playerController.playerId());
                return res;
            }

            PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
            if (cfg == null) {
                res.code = Code.SAMPLE_ERROR;
                return res;
            }

            // 验证注单存在
            AirRaidBetData betData = playerGameData.getAirRaidBetDataMap().get(betIndex);
            if (betData == null) {
                res.code = Code.NOT_FOUND;
                log.warn("AirRaid 注单不存在 playerId={}, betIndex={}", playerController.playerId(), betIndex);
                return res;
            }

            // 验证未兑现
            if (betData.isCashedOut()) {
                res.code = Code.REPEAT_OP;
                log.warn("AirRaid 重复兑现 playerId={}, betIndex={}", playerController.playerId(), betIndex);
                return res;
            }

            // 执行兑现: 计算赢得金额 = betAmount × currentMultiplier / 10000
            int currentMultiplier = getAuthoritativeCurrentMultiplier(now);
            betData.cashOut(currentMultiplier);
            long winAmount = betData.getWinAmount();

            // 从奖池扣钱发给玩家(扣税)
            CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, winAmount, cfg.getTaxRate());
            if (!winResult.success()) {
                // 回滚兑现状态
                betData.setCashedOut(false);
                betData.setCashOutMultiplier(0);
                betData.setWinAmount(0);
                res.code = winResult.code;
                log.warn("AirRaid 从奖池发奖失败 playerId={}, winAmount={}", playerController.playerId(), winAmount);
                return res;
            }

            // 更新本回合展示簿中的兑现信息
            roundBetBook.recordCashOut(playerController.playerId(), betIndex, currentMultiplier, winAmount);

            // 构建响应
            res.playerId = playerController.playerId();
            res.cashOutMultiplier = currentMultiplier;
            res.winAmount = winAmount;
            res.betIndex = betIndex;

            // 广播兑现到所有节点
            broadcastCashOut(playerController.playerId(), currentMultiplier, winAmount, betIndex, playerController.playerId());

            log.info("AirRaid 兑现成功 playerId={}, multiplier={}x, win={}, betIndex={}", playerController.playerId(), currentMultiplier / 10000.0, winAmount, betIndex);
        } catch (Exception e) {
            log.error("AirRaid 兑现异常", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    // ==================== 记录查询 ====================

    @Override
    public AbstractMessage reqPloyRecord(PlayerController playerController, ReqPloyRecord req) {
        // TODO: 后续实现历史记录查询
        return null;
    }

    /**
     * 广播阶段切换到所有节点
     *
     * @param phaseDurationMs 当前阶段总时长(ms), 飞行阶段传0
     */
    private void broadcastPhaseChange(long phaseDurationMs) {
        GameStateSync msg = new GameStateSync();
        msg.roundId = gameRoom.getRoundId();
        msg.phase = gameRoom.getPhase().getCode();
        msg.phaseStartTime = gameRoom.getPhaseStartTime();
        msg.stopTime = resolvePhaseStopTime(phaseDurationMs);
        msg.currentMultiplier = getAuthoritativeCurrentMultiplier(System.currentTimeMillis());
        msg.crashMultiplier = gameRoom.getCrashMultiplier();
        messageSync(msg);
        broadcastLocalPlayers(buildGameStateResponse(msg));
    }

    /**
     * 广播兑现到所有节点
     */
    private void broadcastCashOut(long playerId, int cashOutMultiplier, long winAmount, int betIndex, long excludePlayerId) {
        CashOutSync syncMsg = new CashOutSync();
        syncMsg.roundId = gameRoom.getRoundId();
        syncMsg.playerId = playerId;
        syncMsg.cashOutMultiplier = cashOutMultiplier;
        syncMsg.winAmount = winAmount;
        syncMsg.betIndex = betIndex;
        messageSync(syncMsg);

        ResAirRaidCashOut res = new ResAirRaidCashOut(Code.SUCCESS);
        res.playerId = playerId;
        res.cashOutMultiplier = cashOutMultiplier;
        res.winAmount = winAmount;
        res.betIndex = betIndex;
        broadcastLocalPlayersExcept(res, excludePlayerId);
    }

    // ==================== 集群消息回调(从节点接收) ====================

    /**
     * 收到主节点的游戏状态同步 — 更新本地状态并推送给本地玩家
     */
    public void onGameStateSync(GameStateSync msg) {
        try {
            log.info("收到主节点的游戏状态同步 begin, msg = {}", JSON.toJSONString(msg));
            AirRaidPhase newPhase = AirRaidPhase.fromCode(msg.phase);
            int oldRoundId = gameRoom.getRoundId();
            if (!gameRoom.applyAuthoritativeState(msg.roundId, newPhase, msg.phaseStartTime, msg.stopTime, msg.currentMultiplier, msg.crashMultiplier)) {
                return;
            }

            if (msg.roundId > oldRoundId && newPhase == AirRaidPhase.BETTING) {
                roundBetBook.clear();
                clearLocalPlayerBets();
            }

            broadcastLocalPlayers(buildGameStateResponse(msg));
//            log.info("收到主节点的游戏状态同步 end");
        } catch (Exception e) {
            log.error("AirRaid onGameStateSync异常", e);
        }
    }

    /**
     * 收到主节点的下注同步 — 更新本地投注列表并推送
     */
    public void onBetSync(BetSync msg) {
        try {
            log.info("收到主节点的下注同步 begin, msg = {}", JSON.toJSONString(msg));
            if (msg.roundId != gameRoom.getRoundId()) {
                return;
            }
            roundBetBook.recordBet(msg.playerId, msg.headImgId, msg.betIndex, msg.betAmount);

            // 推送给本地玩家
            ResAirRaidBet res = new ResAirRaidBet(Code.SUCCESS);
            res.betInfoList = roundBetBook.buildBetInfoList();
            broadcastLocalPlayers(res);
        } catch (Exception e) {
            log.error("AirRaid onBetSync异常", e);
        }
    }

    /**
     * 收到主节点的兑现同步 — 更新本地状态并推送
     */
    public void onCashOutSync(CashOutSync msg) {
        try {
            log.info("收到主节点的兑现同步 begin, msg = {}", JSON.toJSONString(msg));
            if (msg.roundId != gameRoom.getRoundId()) {
                return;
            }
            roundBetBook.recordCashOut(msg.playerId, msg.betIndex, msg.cashOutMultiplier, msg.winAmount);

            // 推送给本地玩家
            ResAirRaidCashOut res = new ResAirRaidCashOut(Code.SUCCESS);
            res.playerId = msg.playerId;
            res.cashOutMultiplier = msg.cashOutMultiplier;
            res.winAmount = msg.winAmount;
            res.betIndex = msg.betIndex;
            broadcastLocalPlayers(res);
        } catch (Exception e) {
            log.error("AirRaid onCashOutSync异常", e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建当前所有玩家的投注信息列表
     * 用于返回给客户端展示投注面板
     */
    private List<AirRaidBetInfo> buildBetInfoList() {
        return roundBetBook.buildBetInfoList();
    }

    /**
     * 计算阶段停止时间
     *
     * @param phaseDurationMs
     * @return
     */
    private long resolvePhaseStopTime(long phaseDurationMs) {
        long stopTime;
        if (gameRoom.getPhase() == AirRaidPhase.FLYING) {
            stopTime = gameRoom.getPhaseStopTime();
        } else {
            stopTime = phaseDurationMs > 0 ? gameRoom.getPhaseStartTime() + phaseDurationMs : 0;
        }
        gameRoom.setPhaseStopTime(stopTime);
        return stopTime;
    }

    private ResAirRaidGameState buildGameStateResponse(GameStateSync msg) {
        ResAirRaidGameState res = new ResAirRaidGameState(Code.SUCCESS);
        res.phase = msg.phase;
        res.currentMultiplier = getAuthoritativeCurrentMultiplier(System.currentTimeMillis());
        if (msg.stopTime > 0) {
            res.remainTime = Math.max(0, msg.stopTime - System.currentTimeMillis());
        }
        return res;
    }

    /**
     * 计算当前的倍数
     *
     * @param now
     * @return
     */
    private int getAuthoritativeCurrentMultiplier(long now) {
        if (gameRoom.getPhase() != AirRaidPhase.FLYING) {
            return gameRoom.getCurrentMultiplier();
        }
        long stopTime = gameRoom.getPhaseStopTime();
        if (stopTime > 0 && now >= stopTime) {
            return gameRoom.getCrashMultiplier();
        }
        long flyStartTime = gameRoom.getPhaseStartTime();
        if (flyStartTime <= 0) {
            return gameRoom.getCurrentMultiplier();
        }
        int multiplier = AirRaidCrashCalculator.calculateCurrentMultiplier(now - flyStartTime, growthRate);
        gameRoom.setCurrentMultiplier(multiplier);
        return multiplier;
    }

    @Override
    protected void loadPloyGameRoomCfg() {
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(this.roomCfgId);
        if (cfg == null) {
            log.error("AirRaid 配置加载失败 roomCfgId={}", this.roomCfgId);
            return;
        }

        // 加载公式参数(从 odds 字段)
        Map<Integer, Integer> odds = cfg.getOdds();

        this.growthRate = odds.getOrDefault(AirRaidConstant.Odds.GROWTH, 1200);
        this.riskK = odds.getOrDefault(AirRaidConstant.Odds.RISK, 60);
        this.crashP0 = odds.getOrDefault(AirRaidConstant.Odds.CRASH, 300);

        // 加载阶段时长(从 information 字段)
        List<Integer> info = cfg.getInformation();
        this.bettingDurationMs = info.get(0);
        this.stopBetDurationMs = info.get(1);
        this.settleDurationMs = info.get(2);
        log.info("AirRaid 配置加载完成: growthRate={}, riskK={}, crashP0={}, betting={}ms, stopBet={}ms, settle={}ms", growthRate, riskK, crashP0, bettingDurationMs, stopBetDurationMs, settleDurationMs);
    }

    @Override
    public void shutdown() {
        if (this.event != null) {
            this.timerCenter.remove(this.event);
            this.event = null;
        }
        super.shutdown();
    }
}
