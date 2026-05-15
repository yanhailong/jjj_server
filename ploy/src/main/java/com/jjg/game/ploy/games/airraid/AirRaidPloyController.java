package com.jjg.game.ploy.games.airraid;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.ploy.controller.AbstractMultiPloyController;
import com.jjg.game.ploy.data.PloyBetDivideInfo;
import com.jjg.game.ploy.games.airraid.dao.AirRaidRankDao;
import com.jjg.game.ploy.games.airraid.data.*;
import com.jjg.game.ploy.games.airraid.manager.*;
import com.jjg.game.ploy.games.airraid.pb.*;
import com.jjg.game.ploy.games.airraid.pb.cluster.*;
import com.jjg.game.ploy.pb.ReqPloyRecord;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.AirstrikeRobotCfg;
import com.jjg.game.sampledata.bean.PloygameRoomCfg;
import com.jjg.game.sampledata.bean.PoolResultLibCfg;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 空袭游戏控制器
 *
 * @author 11
 * @date 2026/3/19
 */
@Component
public class AirRaidPloyController extends AbstractMultiPloyController<AirRaidPlayerPloyGameData, AirRaidGameRoom> {


    @Autowired
    private AirRaidSendMessageManager sendMessageManager;
    @Autowired
    private AirRaidRankDao airRaidRankDao;
    @Autowired
    private AirRaidPhaseStateManager airRaidPhaseStateManager;
    @Autowired
    private AirRaidRobotManager airRaidRobotManager;
    @Autowired
    private AirRaidClusterMessageManager airRaidClusterMessageManager;
    @Autowired
    private AirRaidAutoCashOutManager airRaidAutoCashOutManager;

    //游戏全局状态(所有回合共享)
    private final AirRaidGameRoom gameRoom = new AirRaidGameRoom();

    //当前房间本回合的下注展示簿，按注单槽位维度维护
    private final AirRaidRoundBetBook roundBetBook = new AirRaidRoundBetBook();

    //游戏循环定时器引用
    private TimerEvent<String> event;
    //机器人下注定时器(仅主节点)
    private TimerEvent<String> robotBetEvent;
    //飞行阶段每秒 tick：主节点处理机器人兑现，所有节点批量推送 pending 兑现
    private TimerEvent<String> cashOutTickEvent;

    //玩家下注信息(增量)
    private final Queue<AirRaidPlayerInfo> pendingBets = new ConcurrentLinkedQueue<>();
    //玩家兑现信息(增量)
    private final Queue<AirRaidCashOutInfo> pendingCashOuts = new ConcurrentLinkedQueue<>();

    //已完成本地结算的回合号，避免 crash 同步重复记账
    private volatile int lastSettledRoundId;

    //配置的规则抽取解析后的对象
    private AirRaidRuleConfig airRaidRuleConfig;

    private AtomicBoolean init = new AtomicBoolean(false);

    public AirRaidPloyController() {
        super(LoggerFactory.getLogger(AirRaidPloyController.class), AirRaidPlayerPloyGameData.class, AirRaidGameRoom.class);
    }

    @Override
    public void init() {
        super.init();
        init.compareAndSet(false, true);
        if (this.marsCurator.isMaster()) {
            start();
        }
        log.info("初始化空袭控制器");
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
                addPhaseEvent(AirRaidPhase.BETTING_END_BET, diff < 0 ? 0 : (int) diff);
            }
        } else if (phase == AirRaidPhase.BETTING_END_BET) {
            long diff = gameRoom.getPhaseStopTime() - System.currentTimeMillis();
            addPhaseEvent(AirRaidPhase.FLYING, diff < 0 ? 0 : (int) diff);
        } else if (phase == AirRaidPhase.FLYING) {
            long diff = gameRoom.getPhaseStopTime() - System.currentTimeMillis();
            addPhaseEvent(AirRaidPhase.CRASHED, diff < 0 ? 0 : (int) diff);
        } else {
            long diff = gameRoom.getPhaseStopTime() - System.currentTimeMillis();
            addPhaseEvent(AirRaidPhase.BETTING, diff < 0 ? 0 : (int) diff);
        }
    }

    @Override
    public void isLeader() {
        // 必须是主节点才参与计算
        if (init.get()) {
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
        stopCashOutTick();
        this.airRaidAutoCashOutManager.cancelAllAutoCashOutTimers();
    }

    /**
     * 添加定时器
     */
    private void addPhaseEvent(AirRaidPhase phase, int initTime) {
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
        if (e == this.event) {
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
        } else if (e == this.robotBetEvent) {
            handleBetTick();
        } else if (e == this.cashOutTickEvent) {
            handleCashOutTick();
        } else {
            String param = e.getParameter();
            if (this.airRaidAutoCashOutManager.isAutoCashOutTimer(param)) {
                this.airRaidAutoCashOutManager.handleAutoCashOutTimer(param, this.gameRoom, this::doCashOut, this::getPlayerGameData);
            }
        }
    }

    /**
     * 开始新的回合-进入下注阶段
     */
    private void startNewRound() {
        clearRoundData();
        gameRoom.startNewRound();
        gameRoom.setPhaseStopTime(gameRoom.getPhaseStartTime() + this.airRaidRuleConfig.getBettingDurationMs());
        gameRoom.setNotifyPhase(true);
        broadcastPhaseChange(this.airRaidRuleConfig.getBettingDurationMs());
        addPhaseEvent(AirRaidPhase.BETTING_END_BET, this.airRaidRuleConfig.getBettingDurationMs());

        // 创建机器人下注定时器
        this.robotBetEvent = new TimerEvent<>(this, "robotBet", 950).withTimeUnit(TimeUnit.MILLISECONDS);
        timerCenter.add(this.robotBetEvent);
        log.info("新回合开始 round={}", gameRoom.getRoundCounter().get());
    }

    /**
     * 清除旧数据
     */
    private void clearRoundData() {
        this.airRaidRobotManager.clear();
        this.pendingCashOuts.clear();
        roundBetBook.clear();
        // 清空所有本节点玩家的投注数据
        this.gameDataMap.values().forEach(playerData -> {
            long playerId = playerData.playerId();
            PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(playerId, 0,
                    new BaseHandler<String>() {
                        @Override
                        public void action() {
                            playerData.getAirRaidBetDataMap().clear();
                        }
                    }.setHandlerParamWithSelf("airraid clearBets"));
        });
    }

    /**
     * 停止下注阶段
     */
    private void handleStopBetPhaseTimerEvent() {
        this.sendMessageManager.flushBetQueue(this.gameDataMap,this.pendingBets);
        //移除机器人的定时器
        if (this.robotBetEvent != null) {
            this.timerCenter.remove(this.robotBetEvent);
            this.robotBetEvent = null;
        }

        long now = System.currentTimeMillis();
        gameRoom.setPhase(AirRaidPhase.BETTING_END_BET);
        gameRoom.setPhaseStartTime(now);
        gameRoom.setPhaseStopTime(now + this.airRaidRuleConfig.getStopBetDurationMs());
        gameRoom.setNotifyPhase(true);
        broadcastPhaseChange(this.airRaidRuleConfig.getStopBetDurationMs());
        addPhaseEvent(AirRaidPhase.FLYING, this.airRaidRuleConfig.getStopBetDurationMs());
        log.debug("停止下注");
    }

    /**
     * 飞行阶段
     */
    private void handleFlyingPhaseTimerEvent() {
        //坠毁时间
        int crashTimeSec = AirRaidCrashCalculator.generateCrashTime(this.airRaidRuleConfig.getCrashP0(), this.airRaidRuleConfig.getRiskK());
        //坠毁倍率
        int crashMultiplier = AirRaidCrashCalculator.calculateCrashMultiplier(crashTimeSec, this.airRaidRuleConfig.getGrowthRate());
        gameRoom.startFlying(crashMultiplier, crashTimeSec);
        gameRoom.setNotifyPhase(true);
        broadcastPhaseChange(0);
        addPhaseEvent(AirRaidPhase.CRASHED, crashTimeSec * 1000);

        // 启动飞行阶段的兑现 tick(每秒触发机器人兑现 + 推送累积兑现)
        startCashOutTickAndScheduleAutoCashOut();
        log.info("飞行阶段， 坠毁时间 = {},坠毁倍率 ={}", crashTimeSec, crashMultiplier / 10000.0);
    }

    /**
     * 坠毁结算阶段
     */
    private void handleCrashedPhaseTimerEvent() {
        //停止兑现 tick，并把残留的兑现推送出去
        flushAndStopCashOutTick();
        doCrash();
        gameRoom.setPhaseStopTime(gameRoom.getPhaseStartTime() + this.airRaidRuleConfig.getSettleDurationMs());
        gameRoom.setNotifyPhase(true);
        settleCurrentRoundIfNeeded();
        airRaidRankDao.add(gameRoom.getPhaseStopTime(), gameRoom.getCrashMultiplier());
        //广播阶段变化
        broadcastPhaseChange(this.airRaidRuleConfig.getSettleDurationMs());
        addPhaseEvent(AirRaidPhase.BETTING, this.airRaidRuleConfig.getSettleDurationMs());
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
        int crashMul = gameRoom.getCrashMultiplier();
        int settleRoundId = gameRoom.getRoundId();
        for (AirRaidPlayerPloyGameData playerData : this.gameDataMap.values()) {
            long playerId = playerData.playerId();
            int roomCfgId = playerData.getRoomCfgId();
            // 持有引用即可 — betData 仅在该玩家的 disruptor 分区上被写;
            // 把记录写入发布到同一分区, 保证排在所有 in-flight cashOut 之后, 读到的是最终态
            Map<Integer, AirRaidBetData> betMap = new HashMap<>(playerData.getAirRaidBetDataMap());
            if (betMap.isEmpty()) {
                continue;
            }

            PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(playerData.playerId(), 0, new BaseHandler<String>() {
                @Override
                public void action() {
                    for (Map.Entry<Integer, AirRaidBetData> e : betMap.entrySet()) {
                        AirRaidBetData betData = e.getValue();
                        if (!betData.isCurrentRound(settleRoundId)) {
                            continue;
                        }
                        try {
                            AirRaidRecord record = new AirRaidRecord();
                            record.setPlayerId(playerId);
                            record.setRoomCfgId(roomCfgId);
                            record.setRoundId(betData.getRoundId());
                            record.setBetIndex(e.getKey());
                            record.setBetAmount(betData.getBetAmount());
                            record.setCrashMultiplier(crashMul);
                            record.setCashOutMultiplier(betData.getCashOutMultiplier());
                            record.setWinAmount(betData.getWinAmount());
                            record.setCashedOut(betData.isCashedOut());
                            recordDao.saveRecord(record);
                        } catch (Exception ex) {
                            log.error("AirRaid 保存记录异常 playerId={}, betIndex={}", playerId, e.getKey(), ex);
                        }
                    }
                }
            }.setHandlerParamWithSelf("airraid settle"));
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
     * 飞行阶段每秒 tick — 主节点处理机器人兑现；所有节点把累积兑现推送给本地玩家
     */
    private void handleBetTick() {
        try {
            if (this.marsCurator.isMaster()) {
                this.airRaidRobotManager.handleRobotBetEvent(this.gameRoom, this.roundBetBook, this.pendingBets);
            }
            sendMessageManager.flushBetQueue(this.gameDataMap, this.pendingBets);
        } catch (Exception e) {
            log.error("AirRaid cashOut tick异常", e);
        }
    }

    /**
     * 飞行阶段每秒 tick — 主节点处理机器人兑现；所有节点把累积兑现推送给本地玩家
     */
    private void handleCashOutTick() {
        try {
            if (this.marsCurator.isMaster()) {
                this.airRaidRobotManager.handRobotCashoutEvent(this.gameRoom, this.roundBetBook, this.airRaidRuleConfig, this::enqueueCashOut, this::syncCashOutsToCluster);
            }
            sendMessageManager.flushCashOutQueue(this.gameDataMap, this.pendingCashOuts);
        } catch (Exception e) {
            log.error("AirRaid cashOut tick异常", e);
        }
    }

    private void startCashOutTickAndScheduleAutoCashOut() {
        startCashOutTick();
        this.airRaidAutoCashOutManager.scheduleAllAutoCashOutTimers(this, this.gameDataMap, this.gameRoom, this.airRaidRuleConfig);
    }

    /**
     * 启动飞行阶段每秒 tick，幂等。
     */
    private void startCashOutTick() {
        if (this.cashOutTickEvent != null) {
            this.timerCenter.remove(this.cashOutTickEvent);
        }
        this.cashOutTickEvent = new TimerEvent<>(this, "cashOutTick", 950).withTimeUnit(TimeUnit.MILLISECONDS);
        this.timerCenter.add(this.cashOutTickEvent);
    }

    private void flushAndStopCashOutTick() {
        sendMessageManager.flushCashOutQueue(this.gameDataMap, this.pendingCashOuts);
        stopCashOutTick();
        this.airRaidAutoCashOutManager.cancelAllAutoCashOutTimers();
    }

    /**
     * 停止飞行阶段每秒 tick，幂等。
     */
    private void stopCashOutTick() {
        if (this.cashOutTickEvent != null) {
            this.timerCenter.remove(this.cashOutTickEvent);
            this.cashOutTickEvent = null;
        }
    }

    /**
     * 把一条兑现加入本节点 pending 队列
     */
    private void enqueueCashOut(long playerId, int betIndex, int cashOutMultiplier, long winAmount) {
        AirRaidCashOutInfo info = new AirRaidCashOutInfo();
        info.playerId = playerId;
        info.betIndex = betIndex;
        info.cashOutMultiplier = cashOutMultiplier;
        info.winAmount = winAmount;
        this.pendingCashOuts.offer(info);
    }


    /**
     * 把兑现列表同步到其他节点(不发本地)
     */
    private void syncCashOutsToCluster(List<PlayerCashOut> playerCashOutList) {
        CashOutSync syncMsg = new CashOutSync();
        syncMsg.roundId = gameRoom.getRoundId();
        syncMsg.playerCashOuts = playerCashOutList;
        sendMessageManager.messageSync(syncMsg);
    }

    // ==================== 自动兑现 ====================

    /**
     * 更新玩家自动兑现配置 — 仅维护 autoCashOutTargetMap; 不影响已下注注单的快照
     *
     * @param playerId         玩家id
     * @param betIndex         注单索引
     * @param open             是否开启
     * @param targetMultiplier 目标倍率(万分比), open=true 时必须 > 10000
     * @return Code
     */
    public int updateAutoCashOutConfig(long playerId, int betIndex, boolean open, int targetMultiplier) {
        if (betIndex < 0 || betIndex > 1) {
            return Code.PARAM_ERROR;
        }
        AirRaidPlayerPloyGameData playerGameData = getPlayerGameData(playerId);
        if (playerGameData == null) {
            return Code.FAIL;
        }
        if (open) {
            if (targetMultiplier <= GameConstant.TEN_THOUSAND) {
                return Code.PARAM_ERROR;
            }
            playerGameData.getAutoCashOutTargetMap().put(betIndex, targetMultiplier);
        } else {
            playerGameData.getAutoCashOutTargetMap().remove(betIndex);
        }
        return Code.SUCCESS;
    }


    @Override
    protected AbstractResponse buildResPloyConfigMessage(int code, int gameType, int roomCfgId, AirRaidPlayerPloyGameData playerGameData) {
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
        res.currentMultiplier = this.airRaidPhaseStateManager.getAuthoritativeCurrentMultiplier(this.gameRoom, this.airRaidRuleConfig, System.currentTimeMillis());

        // 阶段配置信息(客户端用于倒计时展示)
        List<KVInfo> phaseCfgList = new ArrayList<>();
        phaseCfgList.add(new KVInfo(AirRaidPhase.BETTING.getCode(), this.airRaidRuleConfig.getBettingDurationMs()));
        phaseCfgList.add(new KVInfo(AirRaidPhase.BETTING_END_BET.getCode(), this.airRaidRuleConfig.getStopBetDurationMs()));
        phaseCfgList.add(new KVInfo(AirRaidPhase.CRASHED.getCode(), this.airRaidRuleConfig.getSettleDurationMs()));
        res.phaseCfgList = phaseCfgList;

        // 当前阶段到期时间(客户端据此计算剩余时间)
        res.phaseStopTime = AirRaidGameRoom.clientStopTime(gameRoom.getPhase(), gameRoom.getPhaseStopTime());

        // 玩家自身的自动兑现配置 — 重连后客户端据此恢复 UI, 避免与服务端 autoCashOutTargetMap 状态错位
        Map<Integer, Integer> autoMap = playerGameData.getAutoCashOutTargetMap();
        if (autoMap != null && !autoMap.isEmpty()) {
            List<KVInfo> autoList = new ArrayList<>(autoMap.size());
            for (Map.Entry<Integer, Integer> en : autoMap.entrySet()) {
                autoList.add(new KVInfo(en.getKey(), en.getValue()));
            }
            res.autoCashOutTargets = autoList;
        }

        res.growthRate = this.airRaidRuleConfig.getGrowthRate();
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
     * @param playerGameData 玩家控制器
     * @param bet            下注金额
     * @param betIndex       注单索引(0或1，每人最多2注)
     * @return 下注响应
     */
    @Override
    public ResAirRaidBet bet(AirRaidPlayerPloyGameData playerGameData, long bet, int betIndex) {
        ResAirRaidBet res = new ResAirRaidBet(Code.SUCCESS);
        try {
            if (clusterSystem.nodeConfig.weight < 1) {
                res.code = Code.FAIL;
                log.warn("该节点准备关闭，无法下注 playerId={}", playerGameData.playerId());
                return res;
            }

            long now = System.currentTimeMillis();
            // 验证游戏阶段: 仅下注阶段可投注
            if (!this.gameRoom.canBet(now)) {
                res.code = Code.FAIL;
                log.warn("AirRaid 非下注阶段 playerId={}, phase={}", playerGameData.playerId(), this.gameRoom.getPhase());
                return res;
            }

            // 验证注单索引: 仅允许0和1
            if (betIndex < 0 || betIndex > 1) {
                res.code = Code.PARAM_ERROR;
                log.warn("AirRaid 注单索引错误 playerId={}, betIndex={}", playerGameData.playerId(), betIndex);
                return res;
            }

            // 验证押分值是否在配置列表中
            PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
            if (cfg == null || cfg.getLineBetScore() == null) {
                res.code = Code.SAMPLE_ERROR;
                return res;
            }
            boolean match = cfg.getLineBetScore().stream().anyMatch(b -> b == bet);
            if (!match) {
                res.code = Code.PARAM_ERROR;
                log.warn("AirRaid 下注额不在配置中 playerId={}, bet={}", playerGameData.playerId(), bet);
                return res;
            }

            // 检查是否已在此槽位下注(每个槽位只能下一次)
            AirRaidBetData existingBet = playerGameData.getAirRaidBetDataMap().get(betIndex);
            if (existingBet != null && existingBet.isCurrentRound(gameRoom.getRoundId())) {
                res.code = Code.REPEAT_OP;
                log.warn("AirRaid 重复下注 playerId={}, betIndex={}", playerGameData.playerId(), betIndex);
                return res;
            }

            // 扣除金额到奖池
            if (existingBet != null) {
                playerGameData.getAirRaidBetDataMap().remove(betIndex);
            }
            CommonResult<PloyBetDivideInfo> moneyResult = moneyToPool(playerGameData, bet);
            if (!moneyResult.success()) {
                res.code = moneyResult.code;
                return res;
            }

            // 记录下注数据
            playerGameData.setLastActiveTime(now);
            playerGameData.addBetValue(bet, betIndex, gameRoom.getRoundId());

            // 下注瞬间把自动兑现目标倍率快照到注单上(玩家后续改配置不影响本注单)
            Integer autoTarget = playerGameData.getAutoCashOutTargetMap().get(betIndex);
            if (autoTarget != null && autoTarget > GameConstant.TEN_THOUSAND) {
                AirRaidBetData betData = playerGameData.getAirRaidBetDataMap().get(betIndex);
                if (betData != null) {
                    betData.setAutoCashOutTarget(autoTarget);
                }
            }

            // 更新本回合展示簿
            roundBetBook.recordBet(playerGameData.playerId(), playerGameData.getPlayerController().getPlayer().getHeadImgId(), betIndex, bet);

            res.gold = moneyResult.data.getPlayerAfterMoney();
            res.betIndex = betIndex;

            // 通过集群消息同步到其他节点，本节点玩家单独推送正式协议消息
            BetSync syncMsg = new BetSync();
            syncMsg.roundId = gameRoom.getRoundId();

            AirRaidPlayerInfo airRaidPlayerInfo = new AirRaidPlayerInfo();
            airRaidPlayerInfo.playerId = playerGameData.playerId();
            airRaidPlayerInfo.headImgId = playerGameData.getPlayerController().getPlayer().getHeadImgId();
            airRaidPlayerInfo.bet = bet;
            airRaidPlayerInfo.betIndex = betIndex;
            syncMsg.playerBetInfoList.add(airRaidPlayerInfo);

            pendingBets.offer(airRaidPlayerInfo);
            sendMessageManager.messageSync(syncMsg);
            log.info("AirRaid 下注成功 playerId={}, bet={}, betIndex={}", playerGameData.playerId(), bet, betIndex);
        } catch (Exception e) {
            log.error("AirRaid 下注异常", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 空袭游戏兑现
     * 验证流程: 阶段检查 → 注单存在 → 未兑现 → 计算奖金 → 从奖池发奖 → 广播
     * 兑现金额 = betAmount × currentMultiplier / 10000
     *
     * @param playerController 玩家控制器
     * @param betIndex         注单索引(0或1)
     * @return 兑现响应
     */
    public ResAirRaidCashOut cashOut(PlayerController playerController, int betIndex, long flyTime) {
        if (flyTime < 0) {
            return new ResAirRaidCashOut(Code.PARAM_ERROR);
        }
        //实际最大能飞行的时间
        long realMaxDiffTime = gameRoom.getPhaseStopTime() - gameRoom.getPhaseStartTime();
        if (flyTime > realMaxDiffTime) {
            log.warn("玩家空袭兑现失败，时间参数错误 phase={},startTime = {},stopTime={},realMaxDiffTime={},flyTime={}", gameRoom.getPhase(), gameRoom.getPhaseStartTime(), gameRoom.getPhaseStopTime(), realMaxDiffTime, flyTime);
            return new ResAirRaidCashOut(Code.PARAM_ERROR);
        }

        long now = System.currentTimeMillis();
        int multiplier = AirRaidCrashCalculator.calculateCurrentMultiplier(flyTime, this.airRaidRuleConfig.getGrowthRate());
        return doCashOut(playerController.playerId(), betIndex, multiplier, now, false);
    }

    /**
     * 兑现内核 — 手动/自动兑现复用
     * 自动兑现传入 betData.autoCashOutTarget；手动兑现传入当前实时倍率
     */
    private ResAirRaidCashOut doCashOut(long playerId, int betIndex, int multiplier, long now, boolean auto) {
        ResAirRaidCashOut res = new ResAirRaidCashOut(Code.SUCCESS);
        boolean payoutAttempted = false;
        boolean paid = false;
        try {
            if (!gameRoom.canCashOut(now)) {
                res.code = Code.FAIL;
                log.warn("AirRaid 非飞行阶段 playerId={}, phase={}", playerId, gameRoom.getPhase());
                return res;
            }
            if (betIndex < 0 || betIndex > 1) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            AirRaidPlayerPloyGameData playerGameData = getPlayerGameData(playerId);
            if (playerGameData == null) {
                res.code = Code.FAIL;
                log.warn("AirRaid playerGameData为空 playerId={}", playerId);
                return res;
            }
            PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
            if (cfg == null) {
                res.code = Code.SAMPLE_ERROR;
                return res;
            }
            AirRaidBetData betData = playerGameData.getAirRaidBetDataMap().get(betIndex);
            if (betData == null) {
                res.code = Code.NOT_FOUND;
                log.warn("AirRaid 注单不存在 playerId={}, betIndex={}", playerId, betIndex);
                return res;
            }
            if (!betData.isCurrentRound(gameRoom.getRoundId())) {
                res.code = Code.NOT_FOUND;
                log.warn("AirRaid bet round expired playerId={}, betIndex={}, betRound={}, currentRound={}",
                        playerId, betIndex, betData.getRoundId(), gameRoom.getRoundId());
                return res;
            }
            if (betData.isCashedOut()) {
                res.code = Code.REPEAT_OP;
                log.warn("AirRaid 重复兑现 playerId={}, betIndex={}", playerId, betIndex);
                return res;
            }

            // 单线程模型下可一次性写入 cashedOut + multiplier + winAmount
            betData.cashOut(multiplier);
            long winAmount = betData.getWinAmount();

            // 派奖之前置标 — winFromPool 抛异常时不可回滚, 防止重复扣池/重复发金
            payoutAttempted = true;
            CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, winAmount, cfg.getTaxRate());
            if (!winResult.success()) {
                // 明确失败码 — 假定无副作用, 回滚 cashedOut 让后续可重试
                betData.rollbackCashOut();
                res.code = winResult.code;
                log.warn("AirRaid 从奖池发奖失败 playerId={}, winAmount={}", playerId, winAmount);
                return res;
            }
            paid = true;

            roundBetBook.recordCashOut(playerId, betIndex, multiplier, winAmount);

            res.playerId = playerId;
            res.cashOutMultiplier = multiplier;
            res.winAmount = winAmount;
            res.betIndex = betIndex;
            res.timestamp = now;

            // 写入排行榜
            airRaidRankDao.addCashOut(playerId, playerGameData.getPlayerController().getPlayer().getHeadImgId(), playerGameData.getPlayerController().getPlayer().getHeadFrameId(), now, betData.getBetAmount(),
                    winAmount, multiplier, gameRoom.getCrashMultiplier(), betIndex, gameRoom.getRoundId());

            this.airRaidAutoCashOutManager.cancelAutoCashOutTimer(playerId, betIndex);
            enqueueCashOut(playerId, betIndex, multiplier, winAmount);

            PlayerCashOut playerCashOut = new PlayerCashOut();
            playerCashOut.playerId = playerId;
            playerCashOut.cashOutMultiplier = multiplier;
            playerCashOut.winAmount = winAmount;
            playerCashOut.betIndex = betIndex;
            syncCashOutsToCluster(List.of(playerCashOut));

            log.info("AirRaid 兑现成功 playerId={}, multiplier={}x, win={}, betIndex={},auto = {}", playerId, multiplier, winAmount, betIndex, auto);
        } catch (Exception e) {
            if (paid) {
                log.error("AirRaid 兑现已派奖但后续流程异常 playerId={}, betIndex={}, multiplier={} — 请人工补偿展示簿/集群同步",
                        playerId, betIndex, multiplier, e);
                res.code = Code.SUCCESS; // 钱已经到账，对调用者按成功返回
            } else if (payoutAttempted) {
                // winFromPool 抛异常 — 内部可能已扣池/部分加金, 保留 cashedOut 防止重复落账, 记 ERROR 等人工核对
                log.error("AirRaid 派奖异常(可能产生部分副作用) playerId={}, betIndex={}, multiplier={} — 请核对奖池与玩家金额",
                        playerId, betIndex, multiplier, e);
                res.code = Code.EXCEPTION;
            } else {
                log.error("AirRaid 兑现异常 playerId={}, betIndex={}", playerId, betIndex, e);
                res.code = Code.EXCEPTION;
            }
        }
        return res;
    }

    // ==================== 记录查询 ====================

    @Override
    public AbstractMessage reqPloyRecord(PlayerController playerController, ReqPloyRecord req) {
        return null;
    }

    public ResAirRaidRank queryRank(int rankType, int period) {
        ResAirRaidRank res = new ResAirRaidRank(Code.SUCCESS);

        int currentRoundId = gameRoom.getRoundId();
        boolean currentRoundCrashed = gameRoom.getPhase() == AirRaidPhase.CRASHED;
        switch (rankType) {
            case 1 -> res.rankList = airRaidRankDao.getMultiplierRank(period, currentRoundId, currentRoundCrashed);
            case 2 -> res.rankList = airRaidRankDao.getWinRank(period, currentRoundId, currentRoundCrashed);
            default -> res.rankList = airRaidRankDao.getRoundRank(period);
        }
        return res;
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
        msg.stopTime = this.airRaidPhaseStateManager.resolvePhaseStopTime(this.gameRoom, phaseDurationMs);
        msg.currentMultiplier = this.airRaidPhaseStateManager.getAuthoritativeCurrentMultiplier(this.gameRoom, this.airRaidRuleConfig, System.currentTimeMillis());
        msg.crashMultiplier = gameRoom.getCrashMultiplier();
        sendMessageManager.messageSync(msg);
        sendMessageManager.notifyGameState(this.gameDataMap, msg);
    }

    // ==================== 集群消息回调(从节点接收) ====================

    /**
     * 收到主节点的游戏状态同步 — 更新本地状态并推送给本地玩家
     */
    public void onGameStateSync(GameStateSync msg) {
        this.airRaidClusterMessageManager.onGameStateSync(msg, this.gameDataMap, this.gameRoom,
                this::clearRoundData, this::startCashOutTickAndScheduleAutoCashOut, this::flushAndStopCashOutTick);
    }

    /**
     * 收到主节点的下注同步 — 更新本地投注列表并推送
     */
    public void onBetSync(BetSync msg) {
        this.airRaidClusterMessageManager.onBetSync(msg, this.gameRoom, this.roundBetBook, this.pendingBets);
    }

    /**
     * 收到其他节点的兑现同步 — 更新本地展示簿并加入 pending 队列，等 tick 批量推送给本地玩家
     */
    public void onCashOutSync(CashOutSync msg) {
        this.airRaidClusterMessageManager.onCashOutSync(msg, this.gameRoom, this.roundBetBook, this::enqueueCashOut);
    }

    // ==================== 辅助方法 ====================


    @Override
    protected void loadPloyGameRoomCfg() {
        AirRaidRuleConfig tmpAirRaidRuleConfig = null;

        for (Map.Entry<Integer, PloygameRoomCfg> en : GameDataManager.getPloygameRoomCfgMap().entrySet()) {
            PloygameRoomCfg cfg = en.getValue();
            if (cfg.getGameType() != getGameType()) {
                continue;
            }

            tmpAirRaidRuleConfig = new AirRaidRuleConfig();
            // 加载公式参数(从 odds 字段)
            Map<Integer, Integer> odds = cfg.getOdds();
            tmpAirRaidRuleConfig.setGrowthRate(odds.getOrDefault(AirRaidConstant.Odds.GROWTH, 1200));
            tmpAirRaidRuleConfig.setRiskK(odds.getOrDefault(AirRaidConstant.Odds.RISK, 60));
            tmpAirRaidRuleConfig.setCrashP0(odds.getOrDefault(AirRaidConstant.Odds.CRASH, 300));

            // 加载阶段时长(从 information 字段)
            List<Integer> info = cfg.getInformation();

            tmpAirRaidRuleConfig.setBettingDurationMs(info.get(0));
            tmpAirRaidRuleConfig.setStopBetDurationMs(info.get(1));
            tmpAirRaidRuleConfig.setSettleDurationMs(info.get(2));
            break;
        }

        if (tmpAirRaidRuleConfig != null) {
            this.airRaidRuleConfig = tmpAirRaidRuleConfig;
            log.info("AirRaid 配置加载完成: airRaidRuleConfig={}", JSON.toJSONString(this.airRaidRuleConfig));
        } else {
            log.warn("加载AirRaid 配置失败");
        }
    }

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(PloygameRoomCfg.EXCEL_NAME, this::loadPloyGameRoomCfg);
        addInitSampleFileObserveWithCallBack(PoolResultLibCfg.EXCEL_NAME, this::loadPoolResultLibCfg);
        addInitSampleFileObserveWithCallBack(AirstrikeRobotCfg.EXCEL_NAME, this::loadAirstrikeRobotConfig);
    }

    /**
     * 加载机器人配置
     */
    private void loadAirstrikeRobotConfig() {
        this.airRaidRobotManager.loadAirstrikeRobotConfig();
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.AIR_STRIKE;
    }

    @Override
    public void shutdown() {
        if (this.event != null) {
            this.timerCenter.remove(this.event);
            this.event = null;
        }
        if (this.robotBetEvent != null) {
            this.timerCenter.remove(this.robotBetEvent);
            this.robotBetEvent = null;
        }
        stopCashOutTick();
        this.airRaidAutoCashOutManager.cancelAllAutoCashOutTimers();
        super.shutdown();
    }
}
