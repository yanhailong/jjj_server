package com.jjg.game.ploy.games.airraid;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RobotPlayer;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.ploy.controller.AbstractMultiPloyController;
import com.jjg.game.ploy.data.PloyBetDivideInfo;
import com.jjg.game.ploy.data.PropInfo;
import com.jjg.game.ploy.games.airraid.dao.AirRaidRankDao;
import com.jjg.game.ploy.games.airraid.data.*;
import com.jjg.game.ploy.games.airraid.pb.*;
import com.jjg.game.ploy.games.airraid.pb.cluster.*;
import com.jjg.game.ploy.pb.ReqPloyRecord;
import com.jjg.game.ploy.utils.PropUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.AirstrikeRobotCfg;
import com.jjg.game.sampledata.bean.PloygameRoomCfg;
import com.jjg.game.sampledata.bean.PoolResultLibCfg;
import com.jjg.game.sampledata.bean.RobotCfg;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    @Autowired
    private RobotUtil robotUtil;
    @Autowired
    private AirRaidSendMessageManager sendMessageManager;
    @Autowired
    private AirRaidRankDao airRaidRankDao;

    //游戏全局状态(所有回合共享)
    private final AirRaidGameRoom gameRoom = new AirRaidGameRoom();

    //当前房间本回合的下注展示簿，按注单槽位维度维护
    private final AirRaidRoundBetBook roundBetBook = new AirRaidRoundBetBook();

    //游戏循环定时器引用
    private TimerEvent<String> event;
    //机器人下注定时器(仅主节点)
    private TimerEvent<String> robotBetEvent;
    //飞行阶段每秒 tick 定时器(所有节点) — 主节点用来触发机器人兑现 + 推送累积兑现，从节点仅推送累积兑现
    private TimerEvent<String> cashOutTickEvent;
    //本节点活跃的自动兑现 timer (key = playerId + ":" + betIndex)
    private final Map<String, TimerEvent<String>> autoCashOutTimerMap = new ConcurrentHashMap<>();

    //玩家下注信息(增量)
    private final Queue<AirRaidPlayerInfo> pendingBets = new ConcurrentLinkedQueue<>();
    //玩家兑现信息(增量)
    private final Queue<AirRaidCashOutInfo> pendingCashOuts = new ConcurrentLinkedQueue<>();

    //自动兑现 timer 参数前缀: "auto:<roundId>:<playerId>:<betIndex>"
    private final String AUTO_CASH_OUT_PREFIX = "auto:";

    //已完成本地结算的回合号，避免 crash 同步重复记账
    private volatile int lastSettledRoundId;
    //初始的机器人人数
    private volatile int initRobotCount;
    //本回合参与的机器人
    private Map<Long, RobotPlayer> robotPlayerMap = new ConcurrentHashMap<>();
    //机器人下注金额
    private PropInfo robotBetPropInfo = null;

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
        if (this.robotBetEvent != null) {
            this.timerCenter.remove(this.robotBetEvent);
            this.robotBetEvent = null;
        }
        stopCashOutTick();
        cancelAllAutoCashOutTimers();
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
            handleRobotBetEvent();
        } else if (e == this.cashOutTickEvent) {
            handleCashOutTick();
        } else {
            String param = e.getParameter();
            if (param != null && param.startsWith(AUTO_CASH_OUT_PREFIX)) {
                handleAutoCashOutTimer(param);
            }
        }
    }

    /**
     * 开始新的回合-进入下注阶段
     */
    private void startNewRound() {
        clearRoundData();
        gameRoom.startNewRound();
        gameRoom.setPhaseStopTime(gameRoom.getPhaseStartTime() + bettingDurationMs);
        gameRoom.setNotifyPhase(true);
        broadcastPhaseChange(bettingDurationMs);
        addPhaseEvent(AirRaidPhase.BETTING_END_BET, bettingDurationMs);

        // 创建机器人下注定时器
        this.robotBetEvent = new TimerEvent<>(this, "robotBet", 950).withTimeUnit(TimeUnit.MILLISECONDS);
        timerCenter.add(this.robotBetEvent);
        log.info("新回合开始 round={}", gameRoom.getRoundCounter().get());
    }

    /**
     * 清除旧数据
     */
    private void clearRoundData() {
        this.initRobotCount = 0;
        this.robotPlayerMap.clear();
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
        //移除机器人的定时器
        if (this.robotBetEvent != null) {
            this.timerCenter.remove(this.robotBetEvent);
            this.robotBetEvent = null;
        }

        long now = System.currentTimeMillis();
        gameRoom.setPhase(AirRaidPhase.BETTING_END_BET);
        gameRoom.setPhaseStartTime(now);
        gameRoom.setPhaseStopTime(now + stopBetDurationMs);
        gameRoom.setNotifyPhase(true);
        broadcastPhaseChange(stopBetDurationMs);
        addPhaseEvent(AirRaidPhase.FLYING, stopBetDurationMs);
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
        addPhaseEvent(AirRaidPhase.CRASHED, crashTimeSec * 1000);

        // 启动飞行阶段的兑现 tick(每秒触发机器人兑现 + 推送累积兑现)
        startCashOutTick();
        // 为本地玩家已下注且带 autoCashOutTarget 快照的注单调度精确兑现 timer
        scheduleAllAutoCashOutTimers();
        log.info("飞行阶段， 坠毁时间 = {},坠毁倍率 ={}", crashTimeSec, crashMultiplier / 10000.0);
    }

    /**
     * 坠毁结算阶段
     */
    private void handleCrashedPhaseTimerEvent() {
        //停止兑现 tick，并把残留的兑现推送出去
        stopCashOutTick();
        //队列里的兑现推送给本地玩家
        sendMessageManager.flushCashOutQueue(this.gameDataMap, this.pendingCashOuts);
        //坠毁，剩余未触发的自动兑现 timer 一律取消(也不会再有意义)
        cancelAllAutoCashOutTimers();
        doCrash();
        gameRoom.setPhaseStopTime(gameRoom.getPhaseStartTime() + settleDurationMs);
        gameRoom.setNotifyPhase(true);
        settleCurrentRoundIfNeeded();
        airRaidRankDao.add(gameRoom.getPhaseStopTime(), gameRoom.getCrashMultiplier());
        //广播阶段变化
        broadcastPhaseChange(settleDurationMs);
        addPhaseEvent(AirRaidPhase.BETTING, settleDurationMs);
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
     * 处理机器人下注事件
     */
    private void handleRobotBetEvent() {
        AirstrikeRobotCfg cfg = GameDataManager.getAirstrikeRobotCfg(CoreConst.GameType.AIR_STRIKE);
        if (cfg == null || cfg.getInitial() == null || cfg.getInitial().size() < 2
                || cfg.getIncrease() == null || cfg.getIncrease().size() < 2
                || robotBetPropInfo == null) {
            return;
        }

        //计算本次应该添加的机器人人数
        int addRobotCount = 0;
        if (initRobotCount < 1) {
            initRobotCount = RandomUtils.randomMinMax(cfg.getInitial().get(0), cfg.getInitial().get(1));
            addRobotCount = initRobotCount;
        } else {
            int addProp = RandomUtils.randomMinMax(cfg.getIncrease().get(0), cfg.getIncrease().get(1));
            addRobotCount = PropUtils.propBase(addProp, initRobotCount);
        }

        if (addRobotCount < 1) {
            return;
        }

        BetSync syncMsg = new BetSync();
        syncMsg.roundId = gameRoom.getRoundId();

        for (int i = 0; i < addRobotCount; i++) {
            Integer betCfgId = this.robotBetPropInfo.getRandKey();
            if (betCfgId == null) {
                continue;
            }
            int[] dataSection = this.robotBetPropInfo.getDataSection(betCfgId);
            if (dataSection == null) {
                continue;
            }
            //获取一个机器人
            RobotPlayer robotPlayer = getRobotPlayer();
            if (robotPlayer == null) {
                continue;
            }

            //获取一个下注金额
            int bet = RandomUtils.randomMinMax(dataSection[0], dataSection[1]);
            roundBetBook.recordBet(robotPlayer.getId(), robotPlayer.getHeadImgId(), 0, bet);

            AirRaidPlayerInfo airRaidPlayerInfo = new AirRaidPlayerInfo();
            airRaidPlayerInfo.playerId = robotPlayer.getId();
            airRaidPlayerInfo.headImgId = robotPlayer.getHeadImgId();
            airRaidPlayerInfo.headFrame = robotPlayer.getHeadFrameId();
            airRaidPlayerInfo.bet = bet;
            airRaidPlayerInfo.betIndex = 0;
            syncMsg.playerBetInfoList.add(airRaidPlayerInfo);
            this.pendingBets.offer(airRaidPlayerInfo);
        }
        sendMessageManager.messageSync(syncMsg);
    }

    /**
     * 处理机器人兑现事件
     * 获取AirstrikeRobotCfg.CashIn，数据格式为 1000_2000,即在这个区间随机一个数得到万分比，每秒钟有(机器人人数*万分比)的机器人进行兑现操作
     * 仅主节点执行；产生的兑现 → 入本节点队列(等 tick 推送) + 通过 CashOutSync 同步到其他节点
     */
    private void handRobotCashoutEvent() {
        try {
            long now = System.currentTimeMillis();
            //必须仍在飞行阶段且未到坠毁
            if (!gameRoom.canCashOut(now)) {
                return;
            }

            AirstrikeRobotCfg cfg = GameDataManager.getAirstrikeRobotCfg(CoreConst.GameType.AIR_STRIKE);
            if (cfg == null || cfg.getCashIn() == null || cfg.getCashIn().size() < 2) {
                return;
            }

            //取所有未兑现的机器人下注
            List<AirRaidPlayerInfo> robotBets = new ArrayList<>();
            for (AirRaidPlayerInfo info : roundBetBook.getAllBets()) {
                if (!info.cashedOut && RobotUtil.isRobot(info.playerId)) {
                    robotBets.add(info);
                }
            }
            if (robotBets.isEmpty()) {
                return;
            }

            //本次应兑现的机器人数 = 总数 * 万分比
            int cashInProp = RandomUtils.randomMinMax(cfg.getCashIn().get(0), cfg.getCashIn().get(1));
            int cashOutCount = PropUtils.propBase(cashInProp, robotBets.size());
            if (cashOutCount < 1) {
                return;
            }

            int currentMultiplier = getAuthoritativeCurrentMultiplier(now);
            Collections.shuffle(robotBets);
            int limit = Math.min(cashOutCount, robotBets.size());

            List<PlayerCashOut> syncList = new ArrayList<>(limit);
            for (int i = 0; i < limit; i++) {
                AirRaidPlayerInfo info = robotBets.get(i);
                RobotPlayer robotPlayer = this.robotPlayerMap.get(info.playerId);
                if (robotPlayer == null) {
                    continue;
                }
                long winAmount = info.bet * currentMultiplier / 10000;
                roundBetBook.recordCashOut(info.playerId, info.betIndex, currentMultiplier, winAmount);

                // 写入排行榜
                airRaidRankDao.addCashOut(info.playerId, robotPlayer.getHeadImgId(), robotPlayer.getHeadFrameId(), now, info.bet,
                        winAmount, currentMultiplier, gameRoom.getCrashMultiplier(), info.betIndex, gameRoom.getRoundId());

                //本节点入队
                enqueueCashOut(info.playerId, info.betIndex, currentMultiplier, winAmount);

                //集群同步给其他节点
                PlayerCashOut playerCashOut = new PlayerCashOut();
                playerCashOut.playerId = info.playerId;
                playerCashOut.cashOutMultiplier = currentMultiplier;
                playerCashOut.winAmount = winAmount;
                playerCashOut.betIndex = info.betIndex;
                syncList.add(playerCashOut);
            }
            if (!syncList.isEmpty()) {
                syncCashOutsToCluster(syncList);
            }
        } catch (Exception e) {
            log.error("AirRaid 机器人兑现异常", e);
        }
    }

    /**
     * 飞行阶段每秒 tick — 主节点处理机器人兑现；所有节点把累积兑现推送给本地玩家
     */
    private void handleCashOutTick() {
        try {
            if (this.marsCurator.isMaster()) {
                handRobotCashoutEvent();
            }
            sendMessageManager.flushCashOutQueue(this.gameDataMap, this.pendingCashOuts);
        } catch (Exception e) {
            log.error("AirRaid cashOut tick异常", e);
        }
    }

    /**
     * 启动飞行阶段每秒 tick(幂等)
     */
    private void startCashOutTick() {
        if (this.cashOutTickEvent != null) {
            this.timerCenter.remove(this.cashOutTickEvent);
        }
        this.cashOutTickEvent = new TimerEvent<>(this, "cashOutTick", 950).withTimeUnit(TimeUnit.MILLISECONDS);
        this.timerCenter.add(this.cashOutTickEvent);
    }

    /**
     * 停止飞行阶段每秒 tick(幂等)
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

    private String autoCashOutKey(long playerId, int betIndex) {
        return playerId + ":" + betIndex;
    }

    /**
     * 进入飞行阶段后，为本节点所有已下注且有 autoCashOutTarget 快照的注单调度精确 timer
     */
    private void scheduleAllAutoCashOutTimers() {
        long flyStart = gameRoom.getPhaseStartTime();
        int crashMul = gameRoom.getCrashMultiplier();
        if (flyStart <= 0 || crashMul <= 0) {
            return;
        }
        for (AirRaidPlayerPloyGameData playerGameData : this.gameDataMap.values()) {
            for (Map.Entry<Integer, AirRaidBetData> e : playerGameData.getAirRaidBetDataMap().entrySet()) {
                if (e.getValue().isCurrentRound(gameRoom.getRoundId())) {
                    scheduleAutoCashOutTimer(playerGameData.playerId(), e.getKey(), e.getValue(), flyStart, crashMul);
                }
            }
        }
    }

    /**
     * 为单个注单调度自动兑现 timer
     * - target 缺失或 ≤ 1.0x: 不调度
     * - target > 坠毁倍率: 本回合不可达，不调度(飞机会先坠毁)
     * - 已兑现: 不调度
     * <p>
     * timer param 格式: "auto:<roundId>:<playerId>:<betIndex>" — 触发时校验 roundId,
     * 避免已进入线程池队列但回合已变的旧 timer 误命中新回合(P2)
     */
    private void scheduleAutoCashOutTimer(long playerId, int betIndex, AirRaidBetData betData,
                                          long flyStart, int crashMul) {
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
        long fireAt = flyStart + AirRaidCrashCalculator.calculateFlyDuration(target, growthRate);
        int delay = (int) Math.max(0, fireAt - System.currentTimeMillis());

        String key = autoCashOutKey(playerId, betIndex);
        cancelAutoCashOutTimer(key);

        int roundId = gameRoom.getRoundId();
        String param = AUTO_CASH_OUT_PREFIX + roundId + ":" + playerId + ":" + betIndex;
        TimerEvent<String> ev = new TimerEvent<>(this, delay, param)
                .withTimeUnit(TimeUnit.MILLISECONDS);
        autoCashOutTimerMap.put(key, ev);
        timerCenter.add(ev);
    }

    private void cancelAutoCashOutTimer(long playerId, int betIndex) {
        cancelAutoCashOutTimer(autoCashOutKey(playerId, betIndex));
    }

    private void cancelAutoCashOutTimer(String key) {
        TimerEvent<String> ev = autoCashOutTimerMap.remove(key);
        if (ev != null) {
            timerCenter.remove(ev);
        }
    }

    private void cancelAllAutoCashOutTimers() {
        if (autoCashOutTimerMap.isEmpty()) {
            return;
        }
        for (TimerEvent<String> ev : autoCashOutTimerMap.values()) {
            timerCenter.remove(ev);
        }
        autoCashOutTimerMap.clear();
    }

    /**
     * 自动兑现 timer 触发处理
     * <p>
     * param 格式: "auto:<roundId>:<playerId>:<betIndex>"
     * 先校验 roundId 等于当前回合，过期 timer 静默丢弃(P2)
     */
    private void handleAutoCashOutTimer(String param) {
        try {
            String body = param.substring(AUTO_CASH_OUT_PREFIX.length());
            String[] parts = body.split(":");
            if (parts.length != 3) {
                return;
            }
            final int eventRoundId = Integer.parseInt(parts[0]);
            long playerId = Long.parseLong(parts[1]);
            int betIndex = Integer.parseInt(parts[2]);

            // 回合不匹配 — 旧 timer 残留任务，直接丢弃
            if (eventRoundId != gameRoom.getRoundId()) {
                return;
            }

            autoCashOutTimerMap.remove(autoCashOutKey(playerId, betIndex));

            PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(playerId, 0, new BaseHandler<String>() {
                @Override
                public void action() {
                    // 再次校验回合不匹配 — 旧 timer 残留任务，直接丢弃
                    if (eventRoundId != gameRoom.getRoundId()) {
                        return;
                    }
                    AirRaidPlayerPloyGameData playerGameData = getPlayerGameData(playerId);
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
                    ResAirRaidCashOut res = doCashOut(playerId, betIndex, target, System.currentTimeMillis());
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

    /**
     * 获取一个机器人
     *
     * @return
     */
    private RobotPlayer getRobotPlayer() {
        List<RobotCfg> robotCfgList = GameDataManager.getRobotCfgList();
        if (robotCfgList == null || robotCfgList.isEmpty()) {
            return null;
        }
        for (int i = 0; i < 100; i++) {
            RobotCfg robotCfg = RandomUtil.randomEle(robotCfgList);
            if (robotCfg == null) {
                continue;
            }
            //获取一个机器人
            RobotPlayer robotPlayer = robotUtil.initRobotPlayer(robotCfg);
            if (robotPlayer == null || this.robotPlayerMap.containsKey(robotPlayer.getId())) {
                continue;
            }
            this.robotPlayerMap.put(robotPlayer.getId(), robotPlayer);
            return robotPlayer;
        }
        return null;
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
        res.currentMultiplier = getAuthoritativeCurrentMultiplier(System.currentTimeMillis());

        // 阶段配置信息(客户端用于倒计时展示)
        List<KVInfo> phaseCfgList = new ArrayList<>();
        phaseCfgList.add(new KVInfo(AirRaidPhase.BETTING.getCode(), bettingDurationMs));
        phaseCfgList.add(new KVInfo(AirRaidPhase.BETTING_END_BET.getCode(), stopBetDurationMs));
        phaseCfgList.add(new KVInfo(AirRaidPhase.CRASHED.getCode(), settleDurationMs));
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

        res.growthRate = this.growthRate;
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
     * <p>
     * 验证流程: 阶段检查 → 注单存在 → 未兑现 → 计算奖金 → 从奖池发奖 → 广播
     * 兑现金额 = betAmount × currentMultiplier / 10000
     * </p>
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
        int multiplier = AirRaidCrashCalculator.calculateCurrentMultiplier(flyTime, growthRate);
        return doCashOut(playerController.playerId(), betIndex, multiplier, now);
    }

    /**
     * 兑现内核 — 手动/自动兑现复用
     * 自动兑现传入 betData.autoCashOutTarget；手动兑现传入当前实时倍率
     * <p>
     * 线程模型: 本方法仅在玩家所属的 disruptor 分区线程上执行 — 手动兑现来自 @Command 派发的玩家线程,
     * 自动兑现的 timer 触发后也会 tryPublish 到同一分区, 因此对同一玩家的 doCashOut 调用严格 FIFO,
     * 同时 doSettle 也按玩家分区 publish, 与 doCashOut 在同一线程上排队 — 无需任何同步原语。
     * <p>
     * 异常路径:
     * - winResult.success()==false: 明确失败码, 调用方保证无副作用, 回滚 cashedOut 让后续可重试
     * - winFromPool 抛异常: 内部可能已扣池/部分加金, 不回滚 cashedOut 防止重复落账, 记 ERROR 等人工核对
     * - 派奖成功后的异常: cashedOut 已置 true, 记 ERROR 便于人工补偿展示簿/集群同步, 对调用者按成功返回
     */
    private ResAirRaidCashOut doCashOut(long playerId, int betIndex, int multiplier, long now) {
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

            cancelAutoCashOutTimer(playerId, betIndex);
            enqueueCashOut(playerId, betIndex, multiplier, winAmount);

            PlayerCashOut playerCashOut = new PlayerCashOut();
            playerCashOut.playerId = playerId;
            playerCashOut.cashOutMultiplier = multiplier;
            playerCashOut.winAmount = winAmount;
            playerCashOut.betIndex = betIndex;
            syncCashOutsToCluster(List.of(playerCashOut));

            log.info("AirRaid 兑现成功 playerId={}, multiplier={}x, win={}, betIndex={}", playerId, multiplier / 10000.0, winAmount, betIndex);
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
        msg.stopTime = resolvePhaseStopTime(phaseDurationMs);
        msg.currentMultiplier = getAuthoritativeCurrentMultiplier(System.currentTimeMillis());
        msg.crashMultiplier = gameRoom.getCrashMultiplier();
        sendMessageManager.messageSync(msg);
        sendMessageManager.notifyGameState(this.gameDataMap, msg);
    }

    // ==================== 集群消息回调(从节点接收) ====================

    /**
     * 收到主节点的游戏状态同步 — 更新本地状态并推送给本地玩家
     */
    public void onGameStateSync(GameStateSync msg) {
        try {
//            log.info("收到主节点的游戏状态同步 begin, msg = {}", JSON.toJSONString(msg));
            AirRaidPhase newPhase = AirRaidPhase.fromCode(msg.phase);
            int oldRoundId = gameRoom.getRoundId();

            // 在切换到新一轮 BETTING 之前先 publish 清空, 保证 clear 排在所有新 bet 之前 FIFO 执行
            // (applyAuthoritativeState 一旦把 phase 切到 BETTING, 玩家 bet 就能通过 canBet 检查并被 publish)
            boolean enteringNewRoundBetting = msg.roundId > oldRoundId && newPhase == AirRaidPhase.BETTING;
            if (enteringNewRoundBetting) {
                clearRoundData();
            }

            if (!gameRoom.applyAuthoritativeState(msg.roundId, newPhase, msg.phaseStartTime, msg.stopTime, msg.currentMultiplier, msg.crashMultiplier)) {
                return;
            }

            //从节点感知飞行阶段，启停每秒兑现 tick(主节点 onGameStateSync 不会被自己触发，故主节点不受影响)
            if (newPhase == AirRaidPhase.CRASHED) {
                gameRoom.recordCrashHistory(msg.roundId, msg.crashMultiplier);
            }

            if (newPhase == AirRaidPhase.FLYING) {
                startCashOutTick();
                scheduleAllAutoCashOutTimers();
            } else {
                //离开飞行阶段时把残留兑现推出去再停 tick
                if (this.cashOutTickEvent != null) {
                    sendMessageManager.flushCashOutQueue(this.gameDataMap, this.pendingCashOuts);
                    stopCashOutTick();
                }
                //同时清空尚未触发的自动兑现 timer
                cancelAllAutoCashOutTimers();
            }

            sendMessageManager.notifyGameState(this.gameDataMap, msg);
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

            for (AirRaidPlayerInfo airRaidPlayerInfo : msg.playerBetInfoList) {
                roundBetBook.recordBet(airRaidPlayerInfo.playerId, airRaidPlayerInfo.headImgId, airRaidPlayerInfo.betIndex, airRaidPlayerInfo.bet);
                this.pendingBets.offer(airRaidPlayerInfo);
            }
        } catch (Exception e) {
            log.error("AirRaid onBetSync异常", e);
        }
    }

    /**
     * 收到其他节点的兑现同步 — 更新本地展示簿并加入 pending 队列，等 tick 批量推送给本地玩家
     */
    public void onCashOutSync(CashOutSync msg) {
        try {
            log.info("收到节点的兑现同步 begin, msg = {}", JSON.toJSONString(msg));
            if (msg.roundId != gameRoom.getRoundId()) {
                return;
            }
            if (msg.playerCashOuts == null || msg.playerCashOuts.isEmpty()) {
                return;
            }
            for (PlayerCashOut c : msg.playerCashOuts) {
                roundBetBook.recordCashOut(c.playerId, c.betIndex, c.cashOutMultiplier, c.winAmount);
                enqueueCashOut(c.playerId, c.betIndex, c.cashOutMultiplier, c.winAmount);
            }
        } catch (Exception e) {
            log.error("AirRaid onCashOutSync异常", e);
        }
    }

    // ==================== 辅助方法 ====================

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
        for (Map.Entry<Integer, PloygameRoomCfg> en : GameDataManager.getPloygameRoomCfgMap().entrySet()) {
            PloygameRoomCfg cfg = en.getValue();
            if (cfg.getGameType() != getGameType()) {
                continue;
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
            break;
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
        AirstrikeRobotCfg cfg = GameDataManager.getAirstrikeRobotCfg(CoreConst.GameType.AIR_STRIKE);
        if (cfg == null) {
            log.warn("初始化空袭机器人配置失败");
            return;
        }

        PropInfo pInfo = new PropInfo();

        int begin, end = 0;

        //cfg.getStake()数据格式 1000_10000_5000|10001_50000_3000|50001_200000_1500|200001_500000_400|500001_1000000_100
        //[0] = 下注区间begin ， [1] = 下注区间end ，  [2] = 该区间对应的权重
        for (int i = 0; i < cfg.getStake().size(); i++) {
            List<Integer> list = cfg.getStake().get(i);

            begin = end;
            end += list.get(2);
            pInfo.addProp(i, begin, end);
            pInfo.addData(i, list.get(0), list.get(1));
        }

        this.robotBetPropInfo = pInfo;
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
        cancelAllAutoCashOutTimers();
        super.shutdown();
    }
}
