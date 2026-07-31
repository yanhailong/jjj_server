package com.jjg.game.slots.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.activity.common.data.ActivityTargetType;
import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.activity.wealthroulette.controller.WealthRouletteController;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.base.gameevent.PlayerEventCategory.PlayerEffectiveFlowingEvent;
import com.jjg.game.core.constant.*;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.service.PlayerStatService;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.core.task.param.TaskConditionParam10001;
import com.jjg.game.core.task.param.TaskConditionParam10003;
import com.jjg.game.core.task.param.TaskConditionParam12001;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.core.utils.PropUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.season.data.SeasonFreeSpinResult;
import com.jjg.game.season.data.SeasonSlotsSessionData;
import com.jjg.game.season.pb.res.ResSeasonMatch;
import com.jjg.game.season.service.SeasonFreeGameService;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.data.SimVisitTrialSession;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.sim.data.VisitTrialSpinPermit;
import com.jjg.game.sim.service.SimNodeService;
import com.jjg.game.sim.service.SimVisitQuotaService;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.dao.*;
import com.jjg.game.slots.data.*;
import com.jjg.game.slots.logger.SlotsLogger;
import com.jjg.game.slots.pb.NoticeSlotsLibChange;
import com.jjg.game.slots.pb.NotifySlotsStatus;
import com.jjg.game.slots.service.SlotsPlayerService;
import com.jjg.game.slots.service.SlotsSkillService;
import io.netty.util.Timeout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * slots游戏管理器抽象类
 *
 * @author 11
 * @date 2025/7/1 16:42
 */
public abstract class AbstractSlotsGameManager<T extends SlotsPlayerGameData, L extends SlotsResultLib, G extends GameRunInfo<T>> implements TimerListener, ConfigExcelChangeListener {
    protected Logger log;

    @Autowired
    protected SlotsPlayerService slotsPlayerService;
    @Autowired
    protected PlayerHistorySlotsDao historySlotsDao;
    @Autowired
    protected SlotsPoolDao slotsPoolDao;
    @Autowired
    protected TimerCenter timerCenter;
    @Autowired
    protected CoreMarqueeManager marqueeManager;
    @Autowired
    protected SlotsLogger logger;
    @Autowired
    protected ActivityManager activityManager;
    @Autowired
    protected GameEventManager gameEventManager;
    @Autowired
    protected TaskManager taskManager;
    @Autowired
    protected PlayerStatService playerStatService;
    @Autowired
    protected WealthRouletteController wealthRouletteController;
    @Autowired
    protected RoomSlotsPoolDao roomSlotsPoolDao;
    @Autowired
    protected SlotsRoomManager slotsRoomManager;
    @Autowired
    protected CoopRoomManager coopRoomManager;
    @Autowired
    protected ClusterSystem clusterSystem;
    @Autowired
    protected PlayerGameDataDao playerGameDataDao;
    @Autowired
    protected PlayerAllSlotsDataDao playerAllSlotsDataDao;
    @Autowired
    protected SlotsSkillService slotsSkillService;
    @Autowired
    protected SimNodeService simNodeService;
    @Autowired
    protected SlotsRPCLinkManager slotsRPCLinkManager;
    @Autowired
    protected SimVisitQuotaService simVisitQuotaService;
    @Autowired
    protected SeasonFreeGameService seasonFreeGameService;

    protected AtomicBoolean open = new AtomicBoolean(false);

    //四档奖池 (mini/minor/major/grand) 的档位数量, 只配单个特殊奖池的游戏不按档位统计
    private static final int JACKPOT_TIER_COUNT = 4;
    //赛季被动匹配的本地冷却: 开局后 1 分钟内的旋转不再向 sim 发起被动匹配
    private static final long PASSIVE_MATCH_CD_MILLIS = 60L * 1000;

    //游戏类型
    protected int gameType;
    //当前游戏所有免费状态；由 resetFreeStateIfInvalid 的 freeStatus 入参统一登记
    private final Set<Integer> freeStatusSet = ConcurrentHashMap.newKeySet();

    //playerId -> gameData
    protected Map<Long, T> gameDataMap = new ConcurrentHashMap<>();

    protected int oneHundredMillion = 100000000;
    protected BigDecimal oneHundredMillionBigDecimal = BigDecimal.valueOf(oneHundredMillion);

    protected Class<T> playerGameDataClass;
    protected Class<L> libClass;
    protected Class<G> gameRunInfoClass;

    //roomCfgId -> cfg
    protected Map<Integer, BaseRoomCfg> roomCfgMap;
    //gameMode -> lineId -> cfg
    protected Map<Integer, Map<Integer, BaseLineCfg>> baseLineCfgMap = null;

    //大奖展示倍数区间
    protected Map<Integer, int[]> bigWinShowMap = null;
    //倍场的奖池数据
    protected Map<Integer, Long> poolValueMap;

    //更新获取奖池的事件
    private TimerEvent<String> gameUpdatePoolEvent;

    //总押分 roomCfgId -> [0] = 单线押分  [1] = 总押分
    protected Map<Integer, List<long[]>> allStakeMap;

    //单个类型批量生成条数
    protected int batchGenCount = 200000;
    //批量保存到mongo的条数
    protected int batchSaveCount = 100;

    //在更新结果库后，要开启清除旧结果库的定时事件
    protected TimerEvent<String> clearAllLibEvent;
    //房间Slot游戏超时提出的倒计时
    protected long playerRoomIldeIimeMills = 0;
    //玩家状态检查任务句柄
    private volatile Timeout checkPlayerStatusTimeout;


    public AbstractSlotsGameManager(Class<T> playerGameDataClass, Class<L> libClass, Class<G> gameRunInfoClass) {
        this.playerGameDataClass = playerGameDataClass;
        this.libClass = libClass;
        this.gameRunInfoClass = gameRunInfoClass;
    }

    /**
     * 初始化
     */
    public void init() {
        this.gameType = getGameType();

        getResultLibDao().init(this.gameType);
        getGenerateManager().init(this.gameType);
        initConfig();
        //初始化离线处理定时器
        if (checkPlayerStatusTimeout != null && !checkPlayerStatusTimeout.isCancelled()) {
            checkPlayerStatusTimeout.cancel();
        }
        checkPlayerStatusTimeout = WheelTimerUtil.scheduleAtFixedRate(this::checkPlayerStatus, 1, 2, TimeUnit.SECONDS);
    }

    public long getDefaultBetValue(G gameRunInfo, BaseRoomCfg config) {
        if (gameRunInfo.getData() == null || gameRunInfo.getData().getAllBetScore() < 1) {
            return oneLineToAllStake(config.getDefaultBet().getFirst());
        }
        return gameRunInfo.getData().getAllBetScore();
    }

    /**
     * 生成结果集
     *
     * @param libTypeCountMap 生成条数
     */
    public void generate(Map<Integer, Integer> libTypeCountMap, boolean saveToDB) {
        String redisTableName = null;
        try {
            if (saveToDB) {
                boolean lock = getResultLibDao().addGenerateLock(this.gameType);
                if (!lock) {
                    log.info("生成结果库时添加锁失败，gameType = {}", this.gameType);
                    return;
                }
            }

            redisTableName = getResultLibDao().getNewRedisTableName();

            log.info("开始生成结果库，gameType = {},redisTableName = {},libTypeCountMap = {}", this.gameType, redisTableName, libTypeCountMap);

            //计算出每个区间需要的条数
            Map<Integer, Map<Integer, Integer>> exceptGenCountMap = getGenerateManager().splitLibBySection(libTypeCountMap);
            //拷贝一份
            Map<Integer, Map<Integer, Integer>> tmpExceptGenCountMap = exceptGenCountMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new HashMap<>(e.getValue())));

            //记录当前生成的条数
            Map<Integer, Map<Integer, Integer>> currentGenCountMap = new HashMap<>();

            //移除条数为0的
            getGenerateManager().removeCount0(tmpExceptGenCountMap);

            Map<Integer, Map<Integer, int[]>> sectionMap = getGenerateManager().getSpecialResultLibCacheData().getResultLibSectionMap();

            //实际循环次数
            int currentForCount = 0;
            //累计保存到数据库的条数
            int saveCount = 0;
            //每个libtype对应的实际的条数
            Map<Integer, Integer> realLibTypeCountMap = new HashMap<>();

            List<SlotsResultLib> libList = new ArrayList<>();

            for (Map.Entry<Integer, Integer> countEn : libTypeCountMap.entrySet()) {
                int libType = countEn.getKey();

                for (int i = 0; i < this.batchGenCount; i++) {
                    Map<Integer, Integer> exceptGenSectionCountMap = tmpExceptGenCountMap.get(libType);
                    if (exceptGenSectionCountMap == null) {
                        break;
                    }
                    if (exceptGenSectionCountMap.isEmpty()) {
                        tmpExceptGenCountMap.remove(libType);
                        break;
                    }

                    currentForCount++;

                    SlotsResultLib lib = getGenerateManager().generateOne(libType);
                    if (lib == null) {
                        continue;
                    }

//                    log.debug("打印lib = {}",JSON.toJSONString(lib));

                    for (Object o : lib.getLibTypeSet()) {
                        int tmpLibType = (int) o;
                        int times = (int) lib.getTimes();
                        Map<Integer, int[]> tmpSectionMap = sectionMap.get(tmpLibType);
                        Map<Integer, Integer> temMap = currentGenCountMap.computeIfAbsent(tmpLibType, k -> new HashMap<>());

                        //获取生成结果倍数所在的区间
                        Map.Entry<Integer, int[]> resEn = tmpSectionMap.entrySet().stream().filter(en -> {
                            int[] arr = en.getValue();

                            if (times >= arr[0] && times < arr[1]) {
                                return true;
                            }
                            return false;
                        }).findFirst().orElse(null);

                        if (resEn == null) {
//                            log.warn("未找到对应的倍数区间 libType = {}, times = {}", tmpLibType, times);
                            continue;
                        }
                        int index = resEn.getKey();

                        Integer exceptCount = exceptGenSectionCountMap.get(index);
                        if (exceptCount == null || exceptCount < 1) {
                            continue;
                        }

                        //获取在该区间已经生成的条数
                        Integer currentCount = temMap.get(index);
                        if (currentCount == null) {
                            temMap.merge(index, 1, Integer::sum);
                            libList.add(lib);
                            realLibTypeCountMap.merge(libType, 1, Integer::sum);
                            continue;
                        }

                        if (currentCount < exceptCount) {
                            temMap.merge(index, 1, Integer::sum);
                            libList.add(lib);
                            realLibTypeCountMap.merge(libType, 1, Integer::sum);
                        } else {
                            exceptGenSectionCountMap.remove(index);
                        }
                    }

                    if (libList.size() >= this.batchSaveCount) {
                        if (saveToDB) {
                            getResultLibDao().batchSaveToRedis(redisTableName, libList, getGenerateManager().getSpecialResultLibCacheData().getResultLibSectionMap());
                        }
                        saveCount += libList.size();
                        libList.clear();
                    }
                }
            }

            if (saveToDB) {
                if (!libList.isEmpty()) {
                    getResultLibDao().batchSaveToRedis(redisTableName, libList, getGenerateManager().getSpecialResultLibCacheData().getResultLibSectionMap());
                }
                getResultLibDao().afterSave(redisTableName);
                logger.addGenSlotsLib(this.gameType, realLibTypeCountMap);
                log.info("生成结果库结束，gameType = {},实际循环次数 = {},成功保存到数据库 {} 条,redisName = {}", this.gameType, currentForCount, saveCount, redisTableName);

                this.clearAllLibEvent = new TimerEvent<>(this, 1, "clearLibEvent").withTimeUnit(TimeUnit.MINUTES);
                this.timerCenter.add(this.clearAllLibEvent);

                //通知其他节点，结果库变更
                noticeNodeLibChange(SlotsConst.LibChangeType.LIB_CHANGE, Collections.EMPTY_LIST);
            } else {
                log.debug("生成结束，gameType = {},实际循环次数 = {},总计条数 = {}", this.gameType, currentForCount, saveCount);
            }
        } catch (Exception e) {
            if (StringUtils.isNotEmpty(redisTableName)) {
                getResultLibDao().clearRedisLib(redisTableName, this.gameType);
            }
            getResultLibDao().removeGenerateLock(this.gameType);
            log.error("", e);
        }
    }


    public G enterGame(PlayerController playerController) throws Exception {
        //获取玩家游戏数据
        T playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return createGameRunInfo(playerController.playerId(), Code.NOT_FOUND);
        }
        EGameType type = EGameType.getGameByTypeId(getGameType());
        String gameName = type == null ? String.valueOf(getGameType()) : type.getGameDesc();
        resetFreeStateIfInvalid(playerGameData, SlotsConst.Status.FREE, SlotsConst.Status.NORMAL, gameName);

        G gameRunInfo = createGameRunInfo(playerController.playerId(), Code.SUCCESS);
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }

    protected void resetFreeState(T gameData) {
        gameData.setFreeLib(null);
        gameData.setFreeIndex(new AtomicInteger(0));
        gameData.setRemainFreeCount(new AtomicInteger(0));
        gameData.setFreeAllWin(0);
    }

    protected void resetFreeStateIfInvalid(T gameData, int freeStatus, int normalStatus, String gameName) {
        registerFreeStatus(freeStatus);
        AtomicInteger remainFreeCount = gameData.getRemainFreeCount();
        if (isFreeMode(gameData, freeStatus) && (gameData.getFreeLib() == null || remainFreeCount == null || remainFreeCount.get() <= 0)) {
            gameData.setStatus(normalStatus);
            resetFreeState(gameData);
            log.info("{}玩家状态异常，重置为正常状态,状态为{}, playerId = {}", gameName, gameData.getStatus(), gameData.getPlayerId());
        }
    }

    protected void registerFreeStatus(int freeStatus) {
        freeStatusSet.add(freeStatus);
    }

    protected boolean isFreeMode(T gameData, int freeStatus) {
        return gameData != null && gameData.getStatus() == freeStatus;
    }

    protected boolean isFreeMode(T gameData) {
        for (int freeStatus : freeStatusSet) {
            if (isFreeMode(gameData, freeStatus)) {
                return true;
            }
        }
        return false;
    }

    public G playerStartGame(PlayerController playerController, long betValue) throws Exception {
        //获取玩家游戏数据
        T playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return createGameRunInfo(playerController.playerId(), Code.NOT_FOUND);
        }

        //检查游戏是否开启
        if (!playerGameData.isSeason()) {
            if (!this.open.get()) {
                return createGameRunInfo(playerController.playerId(), Code.GAME_IS_MAINTAIN);
            }
        }

        if (getRoomType() != null) {
            int code = slotsRoomManager.checkCanPlay(this, playerController);
            if (code != Code.SUCCESS) {
                log.debug("该游戏无法继续 playerId = {},gameType = {},roomCfgId = {},code = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), code);
                return createGameRunInfo(playerController.playerId(), code);
            }
        }
        //协作任务房间校验: 未开始禁转/血条耗尽禁转 (非协作玩家零成本放行)
        int coopCode = coopRoomManager.beforeSpin(playerController.playerId(), getGameType());
        if (coopCode != Code.SUCCESS) {
            return createGameRunInfo(playerController.playerId(), coopCode);
        }
        //免费模式和协作任务特殊模式触发检测都需要旋转前的状态
        int statusBefore = playerGameData.getStatus();
        boolean freeMode = isFreeMode(playerGameData);
        CommonResult<VisitTrialSpinPermit> permitResult = slotsRPCLinkManager.prepareVisitTrialSpin(
                playerGameData, getGameType(), freeMode);
        if (permitResult == null || !permitResult.success()) {
            int code = permitResult == null ? Code.EXCEPTION : permitResult.code;
            return createGameRunInfo(playerController.playerId(), code);
        }
        VisitTrialSpinPermit trialPermit = permitResult.data;
        G gameRunInfo;
        try {
            gameRunInfo = startGame(playerController, playerGameData, betValue, false);
        } catch (Exception e) {
            slotsRPCLinkManager.cancelVisitTrialSpin(playerGameData, trialPermit);
            throw e;
        }
        //公共: 旋转成功后通知 sim 联动 (扣能量/加经验/赌场升级/道具掉落), winTimes 取各游戏写入的 allWinTimes
        if (gameRunInfo != null && gameRunInfo.success()) {
            //赛季被动匹配: 须先于 notifySpin, 开局后本次旋转即计入对局第一局
            tryPassiveSeasonMatch(playerController, playerGameData, gameRunInfo.getStake(), freeMode);
            //主动/被动匹配共用 expectedSpins；达到局数后清除本地进行中状态，允许后续被动匹配
            playerGameData.recordSeasonMatchSpin(gameRunInfo.getStake());
            slotsRPCLinkManager.notifySpin(playerGameData, getGameType(), gameRunInfo.getAllWinTimes(),
                    buildSpinStatInfo(gameRunInfo, freeMode, !freeMode && isFreeMode(playerGameData)), trialPermit);
            //协作任务联动: 扣血/共享事件累计/成败判定 (内部吞异常, 不影响旋转主流程)
            coopRoomManager.onSpin(playerController.playerId(), getGameType(), statusBefore, gameRunInfo);
        } else {
            slotsRPCLinkManager.cancelVisitTrialSpin(playerGameData, trialPermit);
        }
        return gameRunInfo;
    }

    /**
     * 由本次旋转结果构建上报 sim 的统计明细 (经营信息 SPINE游戏面板)
     */
    private SpinStatInfo buildSpinStatInfo(G gameRunInfo, boolean freeMode, boolean triggerFree) {
        SpinStatInfo statInfo = new SpinStatInfo();
        statInfo.setFreeMode(freeMode);
        statInfo.setTriggerFree(triggerFree);
        statInfo.setBet(gameRunInfo.getStake());
        statInfo.setWin(gameRunInfo.getAllWinGold());
        statInfo.setMultiple(gameRunInfo.getAllWinTimes());
        statInfo.setBigShowId(gameRunInfo.getBigShowId());
        statInfo.setJackpotCounts(gameRunInfo.getJackpotCounts());
        statInfo.setRemainFreeCount(gameRunInfo.getRemainFreeCount());
        //结果库的模式类型/图标: 赛季试炼任务判定 "触发特殊模式/图标出现次数" 用
        SlotsResultLib resultLib = gameRunInfo.getResultLib();
        if (resultLib != null) {
            if (resultLib.getLibTypeSet() != null && !resultLib.getLibTypeSet().isEmpty()) {
                statInfo.setSpecialModes(List.copyOf(resultLib.getLibTypeSet()));
            }
            if (resultLib.getIconArr() != null && resultLib.getIconArr().length > 0) {
                statInfo.setIcons(Arrays.stream(resultLib.getIconArr()).boxed().toList());
            }
        }
        return statInfo;
    }

    /**
     * 开始游戏
     *
     * @param betValue
     * @return
     */
    protected abstract G startGame(PlayerController playerController, T playerGameData, long betValue, boolean auto);


    /**
     * 普通正常流程
     *
     * @param gameRunInfo
     * @param playerGameData
     * @param betValue
     * @return
     */
    protected G normal(G gameRunInfo, T playerGameData, long betValue) {
        CommonResult<Pair<L, BetDivideInfo>> libResult = normalGetLib(playerGameData, betValue, 1);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        L resultLib = libResult.data.getFirst();
        if (resultLib == null) {
            log.debug("获取的结果为空 playerId = {},gameType = {},betValue = {}", playerGameData.getPlayerId(), this.gameType, betValue);
            gameRunInfo.setCode(Code.FAIL);
            return gameRunInfo;
        }
        gameRunInfo.setBetDivideInfo(libResult.data.getSecond());
        normal(gameRunInfo, playerGameData, betValue, resultLib);
        return gameRunInfo;
    }

    protected abstract G normal(G gameRunInfo, T playerGameData, long betValue, L resultLib);

    /**
     * 普通流程获取结果库
     *
     * @param playerGameData
     * @param betValue
     * @return 返回结果库和税收
     */
    protected CommonResult<Pair<L, BetDivideInfo>> normalGetLib(T playerGameData, long betValue, int specialModeNormalType) {
        CommonResult<Pair<L, BetDivideInfo>> result = new CommonResult<>(Code.SUCCESS);
        log.debug("开始正常流程 playerId = {},roomId = {},betValue = {}", playerGameData.getPlayerId(), playerGameData.getRoomId(), betValue);
        //检查押分是否合法
        long[] betScoreArr = getBetScore(playerGameData, betValue);
        if (betScoreArr == null) {
            log.warn("押分值不合法 playerId = {},gameType = {},roomCfgId = {},betValue = {}", playerGameData.getPlayerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), betValue);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        //记录押分值
        playerGameData.setOneBetScore(betScoreArr[0]);
        playerGameData.setAllBetScore(betScoreArr[1]);

        //从数据库获取结果库
        CommonResult<L> libResult = getLibFromDB(playerGameData, specialModeNormalType);
        if (!libResult.success()) {
            result.code = libResult.code;
            return result;
        }

        L resultLib = libResult.data;

        log.debug("获取到结果库 lib = {}", JSONObject.toJSONString(resultLib));

        //给池子加钱
        CommonResult<Pair<Player, BetDivideInfo>> poolResult = moneyToPool(playerGameData, betValue);
        if (!poolResult.success()) {
            result.code = poolResult.code;
            return result;
        }
        playerStatService.recordSlotBet(playerGameData.getPlayerId(), this.gameType);

        Player player = poolResult.data.getFirst();

        //更新玩家数据
        playerGameData.updatePlayer(poolResult.data.getFirst());

        PlayerController playerController = playerGameData.getPlayerController();
        //获取最新的玩家
        if (playerController != null) {
            playerController.setPlayer(player);
        }

        result.data = new Pair<>(resultLib, poolResult.data.getSecond());
        return result;
    }

    /**
     * 免费模式获取结果库
     *
     * @param playerGameData
     * @return
     */
    protected CommonResult<L> freeGetLib(T playerGameData, int specialModeFreeLibType) {
        return freeGetLib(playerGameData, specialModeFreeLibType, 0);
    }

    /**
     * 免费模式获取结果库
     *
     * @param playerGameData
     * @return
     */
    protected CommonResult<L> freeGetLib(T playerGameData, int specialModeFreeLibType, int specialAuxiliary) {
        CommonResult<L> result = new CommonResult<>(Code.SUCCESS);
        log.debug("开始获取免费结果库 playerId = {}", playerGameData.getPlayerId());

        SlotsResultLib<?> freeLib = playerGameData.getFreeLib();

        if (freeLib == null) {
            //缓存中没有，就从数据库获取
            CommonResult<L> libResult = getLibFromDB(playerGameData, specialModeFreeLibType);
            if (!libResult.success()) {
                result.code = libResult.code;
                return result;
            }

            freeLib = libResult.data;
        }

        if (freeLib == null) {
            log.warn("未在该条结果库中找到免费转信息 gameType = {},modelId = {}", this.gameType, playerGameData.getLastModelId());
            result.code = Code.NOT_FOUND;
            return result;
        }

        if (freeLib.getSpecialAuxiliaryInfoList() == null || freeLib.getSpecialAuxiliaryInfoList().isEmpty()) {
            log.warn("未在该条结果库中找到免费转信息1 gameType = {},libId = {}", this.gameType, freeLib.getId());
            result.code = Code.NOT_FOUND;
            return result;
        }

        log.debug("找到免费旋转的结果库 libId = {}", freeLib.getId());

        //找到结果库中免费游戏的结果
        SpecialAuxiliaryInfo specialAuxiliaryInfo = null;
        for (SpecialAuxiliaryInfo tmpInfo : freeLib.getSpecialAuxiliaryInfoList()) {
            SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(tmpInfo.getCfgId());
            if (specialAuxiliary > 0 && specialAuxiliaryCfg.getType() != specialAuxiliary) {
                continue;
            }
            if (tmpInfo.getFreeGames() == null || tmpInfo.getFreeGames().isEmpty()) {
                continue;
            }
            specialAuxiliaryInfo = tmpInfo;
            break;
        }

        if (specialAuxiliaryInfo == null) {
            log.warn("未在该条结果库中找到免费转信息2 gameType = {},libId = {}", this.gameType, freeLib.getId());
            result.code = Code.NOT_FOUND;
            return result;
        }

        int index = playerGameData.getFreeIndex().getAndAdd(1);
        if (index >= specialAuxiliaryInfo.getFreeGames().size()) {
            resetFreeState(playerGameData);
            result.code = Code.FAIL;
            log.error("免费游戏中的下标值越界 playerId = {},index = {},freeGameSize = {}", playerGameData.getPlayerId(), index, specialAuxiliaryInfo.getFreeGames().size());
            return result;
        }

        JSONObject jsonObject = specialAuxiliaryInfo.getFreeGames().get(index);
        log.debug("获取免费游戏的下标 index = {},allLen = {}", index, specialAuxiliaryInfo.getFreeGames().size());
        L freeGame = JSON.parseObject(jsonObject.toJSONString(), this.libClass);

        if (freeGame == null) {
            log.warn("未在该条结果库中找到免费转信息3 gameType = {},libId = {}", this.gameType, freeLib.getId());
            playerGameData.setFreeLib(null);
            result.code = Code.NOT_FOUND;
            return result;
        }

        //缓存获取到的freeLib
        playerGameData.setFreeLib(freeLib);
        result.data = freeGame;
        return result;
    }

    /**
     * 赛季每日免费局: 仅当前赛季机台默认下注的普通旋转可用 (提高下注额/客座试玩不享受);
     * 次数由 sim 按玩家赛季时间每日重置, 这里按系统日缓存"已耗尽"避免多余 RPC。
     * 消耗成功即视为已授权, RPC 失败按正常扣费处理。
     *
     * @return true 表示本次旋转已消耗一次免费次数, 不再扣下注额
     */
    protected boolean trySeasonFreeSpin(T gameData, long betValue) {
        if (!gameData.isSeasonFreeGameCandidate() || gameData.getVisitOwnerId() > 0) {
            return false;
        }
        BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(gameData.getRoomCfgId());
        //仅默认(最低档)下注享受免费
        if (baseRoomCfg == null || betValue != oneLineToAllStake(baseRoomCfg.getDefaultBet().getFirst())) {
            return false;
        }
        int today = SeasonFreeGameService.dailyKey(System.currentTimeMillis());
        if (gameData.getSeasonFreeExhaustedDailyKey() == today) {
            return false;
        }
        SeasonFreeSpinResult result = slotsRPCLinkManager.useSeasonFreeSpin(gameData, this.gameType);
        if (result == null) {
            return false;
        }
        if (result.isFree()) {
            if (result.getRemaining() <= 0) {
                gameData.setSeasonFreeExhaustedDailyKey(today);
            }
            log.info("赛季免费局生效, 本次旋转不扣下注额 playerId = {},gameType = {},betValue = {},remaining = {}",
                    gameData.getPlayerId(), this.gameType, betValue, result.getRemaining());
            return true;
        }
        if (result.getReason() == SeasonFreeSpinResult.REASON_EXHAUSTED) {
            gameData.setSeasonFreeExhaustedDailyKey(today);
        } else if (result.getReason() == SeasonFreeSpinResult.REASON_UNAVAILABLE) {
            gameData.setSeasonFreeGameCandidate(false);
        }
        return false;
    }

    /**
     * 赛季被动匹配: 赛季入口 (enterType=1) 的每次普通旋转按全局概率触发一次匹配, 玩家不可拒绝。
     * 命中后与客户端主动发起的 ReqSeasonMatch 走同一个 RPC 与同一份 {@link ResSeasonMatch} 应答,
     * 由本节点下发; 押注赛季币/选对手/每日次数等都在 sim 侧完成。
     * 须在通知 sim 本次旋转之前调用, 使开局先于本次旋转上报到达, 本次旋转即计入对局第一局。
     * 本地冷却只是避免无谓 RPC, 权威冷却仍由 sim 按 SeasonMatch.MatchCD 校验。
     */
    protected void tryPassiveSeasonMatch(PlayerController playerController, T gameData, long stake, boolean freeMode) {
        //免费模式没有真实下注, 不触发按下注额押注的对局
        if (!gameData.isSeason() || gameData.isSeasonMatchActive() || freeMode || stake <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - gameData.getPassiveMatchTime() < PASSIVE_MATCH_CD_MILLIS) {
            return;
        }
        GlobalConfigCfg probCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Global.ID_SEASON_PASSIVE_MATCH_PROB);
        if (probCfg == null || !RandomUtils.getRandomBoolean100(probCfg.getIntValue())) {
            return;
        }
        ResSeasonMatch res = slotsRPCLinkManager.seasonMatch(gameData, this.gameType, stake);
        //开局被拒(冷却中/次数已满/赛季币不足等)时 matchId 为 "0", 静默跳过, 不影响本次旋转
        if (res == null || res.code != Code.SUCCESS || res.matchId == null || "0".equals(res.matchId)) {
            return;
        }
        gameData.setSeasonCoinBalance(res.seasonCoin);
        gameData.beginSeasonMatch(res.stake, res.expectedSpins);
        gameData.setPassiveMatchTime(now);
        playerController.send(res);
        log.info("赛季被动匹配触发 playerId = {},gameType = {},stake = {},matchId = {},opponentId = {}",
                gameData.getPlayerId(), this.gameType, stake, res.matchId, res.opponentId);
    }

    /**
     * 从赛季进入的下注: 同步 RPC 扣除赛季币, 成功后刷新本地余额缓存 (用于响应展示)。
     * 返回 code=SUCCESS 表示扣除成功; 余额不足返回 NOT_ENOUGH, sim 不可达返回失败。
     */
    protected CommonResult<Long> seasonDeduct(T gameData, long amount) {
        long before = gameData.getSeasonCoinBalance();
        CommonResult<Long> result = slotsRPCLinkManager.deductSeasonCoin(gameData, amount);
        if (result.success() && result.data != null) {
            gameData.setSeasonCoinBalance(result.data);
            log.info("赛季币结算 type=bet,playerId={},gameType={},roomCfgId={},currency=season,amount={},before={},after={}",
                    gameData.getPlayerId(), this.gameType, gameData.getRoomCfgId(), amount, before, result.data);
        }
        return result;
    }

    /**
     * 从赛季进入的中奖: 先经 RPC 给玩家发赛季币(幂等+超时重试), 成功后刷新本地余额缓存。
     * 采用"先发币、后扣池"(credit-first): 发放失败即不扣池、不算中奖, 无需反向 increment 回补,
     * 从根本上避免超时回补造成的凭空增发与假池被破坏。返回 true 表示发放成功。
     */
    protected boolean seasonReward(T gameData, long amount) {
        long before = gameData.getSeasonCoinBalance();
        CommonResult<Long> result = slotsRPCLinkManager.addSeasonCoin(gameData, amount);
        if (!result.success() || result.data == null) {
            log.warn("赛季币发放失败 playerId={},gameType={},roomCfgId={},amount={},code={}",
                    gameData.getPlayerId(), this.gameType, gameData.getRoomCfgId(), amount, result.code);
            return false;
        }
        gameData.setSeasonCoinBalance(result.data);
        log.info("赛季币结算 type=reward,playerId={},gameType={},roomCfgId={},currency=season,amount={},before={},after={}",
                gameData.getPlayerId(), this.gameType, gameData.getRoomCfgId(), amount, before, result.data);
        return true;
    }

    /**
     * 从标准池扣数值并给玩家发钱: 赛季币走 RPC(credit-first), 否则金币走 addGold。池子共用不区分货币。
     * 成功时 data 为最新玩家对象(赛季模式返回当前玩家, 余额已刷新到 gameData); 供直接调用 DAO 的玩法复用。
     */
    protected CommonResult<Player> rewardBigPoolCurrency(T playerGameData, long value, AddType addType) {
        if (playerGameData.isSeason()) {
            if (value < 1 || !seasonReward(playerGameData, value)) {
                return new CommonResult<>(Code.FAIL);
            }
            //发币成功后再扣池(redis 可靠), 无回补
            slotsPoolDao.addToBigPool(this.gameType, playerGameData.getRoomCfgId(), -value);
            return new CommonResult<>(Code.SUCCESS, playerGameData.getPlayer());
        }
        return slotsPoolDao.rewardFromBigPool(playerGameData.getPlayerId(), this.gameType, playerGameData.getRoomCfgId(), value, addType);
    }

    /**
     * 从小池(固定值)扣数值并给玩家发钱: 赛季币走 RPC(credit-first), 否则金币走 addGold。
     * 成功时 data 为最新玩家对象; 供直接调用 DAO 的玩法复用。
     */
    protected CommonResult<Player> rewardSmallPoolCurrency(T playerGameData, long value, int poolId, AddType addType) {
        if (playerGameData.isSeason()) {
            if (value < 1 || !seasonReward(playerGameData, value)) {
                return new CommonResult<>(Code.FAIL);
            }
            slotsPoolDao.addToSmallPool(this.gameType, playerGameData.getRoomCfgId(), -value);
            slotsPoolDao.updatePoolCD(poolId);
            return new CommonResult<>(Code.SUCCESS, playerGameData.getPlayer());
        }
        return slotsPoolDao.rewardFromSmallPool(playerGameData.getPlayerId(), this.gameType, playerGameData.getRoomCfgId(), value, poolId, addType, poolId + "");
    }

    /**
     * 从小池(按占比)扣数值并给玩家发钱: 赛季币走 RPC(credit-first), 否则金币走 addGold。
     * 成功时 data 为本次中奖值; 统一收口所有 rewardByRatioFromSmallPool 直连 DAO 的玩法。
     */
    protected CommonResult<Long> rewardByRatioSmallPoolCurrency(T playerGameData, int ratio, int poolId, AddType addType) {
        PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
        long stake = playerGameData.getAllBetScore();
        int maxMultiple = poolCfg == null ? 0 : poolCfg.getMaxMultiple();
        if (playerGameData.isSeason()) {
            Number poolNum = slotsPoolDao.getSmallPoolByRoomCfgId(this.gameType, playerGameData.getRoomCfgId());
            long value = poolNum == null ? 0 : PropUtil.calProp(ratio, poolNum.longValue());
            if (maxMultiple > 0 && stake > 0) {
                long maxValue = stake > Long.MAX_VALUE / maxMultiple
                        ? Long.MAX_VALUE : stake * maxMultiple;
                value = Math.min(value, maxValue);
            }
            if (value < 1 || !seasonReward(playerGameData, value)) {
                return new CommonResult<>(Code.FAIL);
            }
            slotsPoolDao.addToSmallPool(this.gameType, playerGameData.getRoomCfgId(), -value);
            slotsPoolDao.updatePoolCD(poolId);
            return new CommonResult<>(Code.SUCCESS, value);
        }
        return slotsPoolDao.rewardByRatioFromSmallPool(playerGameData.getPlayerId(), this.gameType,
                playerGameData.getRoomCfgId(), ratio, poolId, stake, maxMultiple, addType);
    }

    /**
     * 给池子加钱
     *
     * @param gameData
     * @param betValue
     * @return player对象和税收
     */
    protected CommonResult<Pair<Player, BetDivideInfo>> moneyToPool(T gameData, long betValue) {
        SlotsRoomController slotsRoomController = gameData.getSlotsRoomController();
        if (slotsRoomController == null) {
            //赛季每日免费局: 命中时不扣下注额、不给池子加钱, 返回全 0 的分账信息 (收益照常)
            PlayerController freeSpinController = gameData.getPlayerController();
            Player freeSpinPlayer = freeSpinController == null ? null : freeSpinController.getPlayer();
            if (freeSpinPlayer != null && trySeasonFreeSpin(gameData, betValue)) {
                return new CommonResult<>(Code.SUCCESS, new Pair<>(freeSpinPlayer, new BetDivideInfo()));
            }
            Player player;
            if (gameData.isSeason()) {
                //从赛季进入: 扣赛季币 (同步 RPC 到 sim), 池子共用不区分货币; 余额不足/sim不可达则拒绝本次旋转
                CommonResult<Long> deductResult = seasonDeduct(gameData, betValue);
                if (!deductResult.success()) {
                    log.warn("扣除赛季币失败,拒绝本次旋转 playerId = {},betValue = {},code = {}", gameData.getPlayerId(), betValue, deductResult.code);
                    return new CommonResult<>(deductResult.code);
                }
                final Player p = gameData.getPlayer();
                if (p == null) {
                    return new CommonResult<>(Code.FAIL);
                }
                //赛季币不计入金币道具的活动/有效流水; 仅触发货币无关的下注/局数任务
                taskManager.trigger(p.getId(), TaskConstant.ConditionType.PLAYER_BET_ALL, () -> {
                    TaskConditionParam12001 param = new TaskConditionParam12001();
                    param.setGameId(getGameType());
                    param.setAddValue(betValue);
                    return param;
                }, false);
                taskManager.trigger(p.getId(), TaskConstant.ConditionType.BET_COUNT, () -> {
                    TaskConditionParam10001 param = new TaskConditionParam10001();
                    param.setAddValue(betValue);
                    param.setGameId(getGameType());
                    return param;
                }, false);
                player = p;
                log.info("玩家扣除赛季币成功 playerId = {},reduce = {},afterSeasonCoin = {}", gameData.getPlayerId(), betValue, gameData.getSeasonCoinBalance());
            } else {
                CommonResult<Pair<Player, Long>> result = slotsPlayerService.betDeductGold(gameData.getPlayerId(), betValue, true, AddType.SLOTS_BET);
                if (!result.success()) {
                    log.warn("把钱添加到池子失败,扣除玩家金额失败 playerId = {},betValue = {},code = {}", gameData.getPlayerId(), betValue, result.code);
                    return new CommonResult<>(result.code);
                }

                final Player p = result.data.getFirst();
                PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(p.getId(), 0, new BaseHandler<String>() {
                    @Override
                    public void action() {
                        activityManager.addActivityProgress(p, ActivityTargetType.getTagetKey(ActivityTargetType.BET, ActivityTargetType.EFFECTIVE_BET), betValue, ItemUtils.getGoldItemId());
                        activityManager.addPlayerActivityProgress(p, ActivityTargetType.getTagetKey(ActivityTargetType.BET, ActivityTargetType.EFFECTIVE_BET), betValue, ItemUtils.getGoldItemId());
                        // 触发有效流水事件
                        gameEventManager.triggerEvent(new PlayerEffectiveFlowingEvent(p, gameData.getRoomCfgId(), betValue, 0));
                    }
                }.setHandlerParamWithSelf("goldToPool"));
                //触发任务
                taskManager.trigger(p.getId(), TaskConstant.ConditionType.PLAYER_BET_ALL, () -> {
                    TaskConditionParam12001 param = new TaskConditionParam12001();
                    param.setGameId(getGameType());
                    param.setAddValue(betValue);
                    return param;
                }, false);
                //触发下注
                taskManager.trigger(p.getId(), TaskConstant.ConditionType.BET_COUNT, () -> {
                    TaskConditionParam10001 param = new TaskConditionParam10001();
                    param.setAddValue(betValue);
                    param.setGameId(getGameType());
                    return param;
                }, false);
                player = p;
                log.info("玩家扣除金币成功 playerId = {},reduceGold = {},afterGold = {}", gameData.getPlayerId(), betValue, p.getGold());
            }
            BigDecimal bet = BigDecimal.valueOf(betValue);

            BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(gameData.getRoomCfgId());
            //给标准池子加钱
            BigDecimal toBigPoolProp = BigDecimal.valueOf(baseRoomCfg.getInitBasePoolProportion()).divide(GameConstant.TEN_THOUSAND_BD, 4, RoundingMode.HALF_UP);
            long toBigPoolGold = bet.multiply(toBigPoolProp).setScale(0, RoundingMode.HALF_UP).longValue();
            if (toBigPoolGold > 0) {
                long poolCoin = slotsPoolDao.addToBigPool(this.gameType, gameData.getRoomCfgId(), toBigPoolGold);
                log.info("给标准池加钱成功 playerId = {},gameType = {},roomCfgId = {},modelId = {},add = {},afterGold = {}", gameData.getPlayerId(), gameData.getGameType(), gameData.getRoomCfgId(), gameData.getLastModelId(), toBigPoolGold, poolCoin);
            }

            //给小池子加钱
            BigDecimal toSmallPoolProp = BigDecimal.valueOf(baseRoomCfg.getCommissionProp()).divide(GameConstant.TEN_THOUSAND_BD, 4, RoundingMode.HALF_UP);
            long toSmallPoolGold = bet.multiply(toSmallPoolProp).setScale(0, RoundingMode.HALF_UP).longValue();
            if (toSmallPoolGold > 0) {
                long poolCoin = slotsPoolDao.addToSmallPool(this.gameType, gameData.getRoomCfgId(), toSmallPoolGold);
                gameData.addAllBet(betValue);
                long contribtGold = gameData.addContribtPoolGold(toSmallPoolGold);
                log.info("给小池子加钱成功 playerId = {},gameType = {},roomCfgId = {},modelId = {},add = {},afterGold = {},contribtGold={}", gameData.getPlayerId(), gameData.getGameType(), gameData.getRoomCfgId(), gameData.getLastModelId(), toSmallPoolGold, poolCoin, contribtGold);
            }

            CommonResult<Pair<Player, BetDivideInfo>> commonResult = new CommonResult<>(Code.SUCCESS);
            long tax = betValue - toBigPoolGold - toSmallPoolGold;

            BetDivideInfo betDivideInfo = new BetDivideInfo();
            betDivideInfo.setToBigPool(toBigPoolGold);
            betDivideInfo.setToSmallPool(toSmallPoolGold);
            betDivideInfo.setTax(tax);

            //增加下注次数
            gameData.getPlayerAllSlotsData().incrementBetCount();

            if (tax < 1) {
                log.warn("tax 小于1， gameType = {},roomCfgId = {},betValue = {},toBigPoolGold = {},toSmallPoolGold = {}", gameData.getGameType(), gameData.getRoomCfgId(), betValue, toBigPoolGold, toSmallPoolGold);
            }
            commonResult.data = new Pair<>(player, betDivideInfo);
            return commonResult;
        } else if (slotsRoomController.getRoom().getType() == RoomType.SLOTS_TEAM_UP_ROOM) { //slots好友房
            return roomMoneyToPool(gameData, betValue);
        } else {
            log.warn("moneyToPool 不支持的房间类型 playerId = {},roomType = {}", gameData.getPlayerId(), slotsRoomController.getRoom().getType());
            return new CommonResult<>(Code.FAIL);
        }
    }

    /**
     * 给房间池子加钱
     *
     * @param gameData
     * @param betValue
     * @return player对象和税收
     */
    protected CommonResult<Pair<Player, BetDivideInfo>> roomMoneyToPool(T gameData, long betValue) {
        try {
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(gameData.getRoomCfgId());
            if (warehouseCfg == null) {
                return new CommonResult<>(Code.SAMPLE_ERROR);
            }
            CommonResult<Player> result = slotsPlayerService.betDeductCurrent(gameData.getPlayerId(), betValue, warehouseCfg.getTransactionItemId(), true, AddType.SLOTS_BET, String.valueOf(gameData.getRoomId()), false);

            if (!result.success()) {
                log.warn("把钱添加到房间池子失败,扣除玩家金额失败 playerId = {},betValue = {},roomId = {},code = {}", gameData.getPlayerId(), betValue, gameData.getRoomId(), result.code);
                return new CommonResult<>(result.code);
            }

            BigDecimal bet = BigDecimal.valueOf(betValue);
            BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(gameData.getRoomCfgId());
            //给标准池子加钱
            BigDecimal toBigPoolProp = BigDecimal.valueOf(baseRoomCfg.getInitBasePoolProportion()).divide(GameConstant.TEN_THOUSAND_BD, 4, RoundingMode.HALF_UP);
            long toBigPoolGold = bet.multiply(toBigPoolProp).setScale(0, RoundingMode.HALF_UP).longValue();
            if (toBigPoolGold > 0) {
                long poolCoin = roomSlotsPoolDao.addToBigPool(gameData.getRoomId(), toBigPoolGold);
                if (gameData.getRoomId() > 0) {
                    slotsRoomManager.updatePoolValue(gameData.getRoomId(), poolCoin);
                }
                log.info("给房间标准池加钱成功 playerId = {},gameType = {},roomId = {},roomCfgId = {},modelId = {},add = {},poolAfterGold = {}", gameData.getPlayerId(), gameData.getGameType(), gameData.getRoomId(), gameData.getRoomCfgId(), gameData.getLastModelId(), toBigPoolGold, poolCoin);
            }

            //扣除加入水池的钱之后，剩余的钱
            long systemIncomeBigDecimal = betValue - toBigPoolGold;

            GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(SlotsConst.GlobalConfig.ID_ROOM_INCOME_PROP);
            long roomCreatorIncome = 0;
            if (globalConfigCfg.getIntValue() > 0) {
                //房主收益
                BigDecimal roomCreatorIncomeBigDecimal = BigDecimal.valueOf(systemIncomeBigDecimal).multiply(BigDecimal.valueOf(globalConfigCfg.getIntValue())).divide(GameConstant.TEN_THOUSAND_BD, 2, RoundingMode.HALF_UP);
                roomCreatorIncome = roomCreatorIncomeBigDecimal.longValue();
            }

            CommonResult<Pair<Player, BetDivideInfo>> commonResult = new CommonResult<>(Code.SUCCESS);
            long tax = betValue - toBigPoolGold - roomCreatorIncome;

            BetDivideInfo betDivideInfo = new BetDivideInfo();
            betDivideInfo.setToBigPool(toBigPoolGold);
            betDivideInfo.setInCome(roomCreatorIncome);
            betDivideInfo.setTax(tax);

            if (tax < 1) {
                log.warn("tax 小于1， gameType = {},roomCfgId = {},roomId = {},betValue = {},toBigPoolGold = {}", gameData.getGameType(), gameData.getRoomCfgId(), gameData.getRoomId(), betValue, toBigPoolGold);
            }
            commonResult.data = new Pair<>(result.data, betDivideInfo);
            slotsRoomManager.playerBet(gameData.getRoomId(), gameData.getPlayerId(), betValue, roomCreatorIncome);
            return commonResult;
        } catch (Exception e) {
            log.error("", e);
            return new CommonResult<>(Code.EXCEPTION);
        }
    }

    /**
     * 添加更新奖池事件
     */
    protected void addUpdatePoolEvent() {
        this.gameUpdatePoolEvent = new TimerEvent<>(this, "gameUpdatePoolEvent", 20).withTimeUnit(TimeUnit.SECONDS);
        timerCenter.add(this.gameUpdatePoolEvent);
    }

    /**
     * 关闭
     */
    public void shutdown() {
        if (checkPlayerStatusTimeout != null && !checkPlayerStatusTimeout.isCancelled()) {
            checkPlayerStatusTimeout.cancel();
        }
        checkPlayerStatusTimeout = null;
        this.gameDataMap.forEach((k, v) -> {
            try {
                if (v.getOfflineEventMap() != null && !v.getOfflineEventMap().isEmpty()) {
                    for (Map.Entry<Integer, OffLineEventData> en : v.getOfflineEventMap().entrySet()) {
                        //检查该事件是否已经执行
                        if (en.getValue().isAction()) {
                            continue;
                        }
                        //开始执行
                        onAutoExitAction(v, en.getKey());
                        en.getValue().setAction(true);
                    }
                }
                offlineSaveGameData(v);
                taskManager.onExit(v.getPlayerId());
            } catch (Exception e) {
                log.error("", e);
            }
        });
    }

    protected void initConfig() {
        baseRoomConfig();
        baseLineConfig();
        specialPlayConfig();
        specialAuxiliaryConfig();

        globalConfig();
        calAllLineStake();
//        log.info("配置重新计算结束 gameType = {}", this.gameType);
    }


    /**
     * 根据水池偏差值获取 结果库配置
     *
     * @param diff
     * @return
     */
    protected SpecialResultLibCfg getLibCfgByPoolDiff(long diff) {
        for (Map.Entry<Integer, SpecialResultLibCfg> en : getGenerateManager().getSpecialResultLibCacheData().getResultLibMap().entrySet()) {
            SpecialResultLibCfg cfg = en.getValue();
            if ((diff >= cfg.getEnterLimitMin() || cfg.getEnterLimitMin() <= -999999) && (diff < cfg.getEnterLimitMax() || cfg.getEnterLimitMax() >= 999999)) {
                return cfg;
            }
        }
        return null;
    }

    /**
     * 根据模式id获取libCfg
     *
     * @param gameType
     * @param modelId
     * @return
     */
    protected SpecialResultLibCfg getLibCfgByModelId(int gameType, int modelId) {
        for (Map.Entry<Integer, SpecialResultLibCfg> en : GameDataManager.getSpecialResultLibCfgMap().entrySet()) {
            SpecialResultLibCfg cfg = en.getValue();
            if (cfg.getGameType() == gameType && cfg.getModelId() == modelId) {
                return cfg;
            }
        }
        return null;
    }

    /**
     * 获取libCfg，如果第一次玩这个游戏(按gameType计算)，那么使用默认的水池
     * 否则要计算水池偏差
     *
     * @param gameData
     * @return
     */
    public CommonResult<SpecialResultLibCfg> getLibCfg(T gameData, long poolInit) {
        CommonResult<SpecialResultLibCfg> result = new CommonResult<>(Code.SUCCESS);

        SlotsRoomController roomController = gameData.getSlotsRoomController();

        boolean flag = gameData.getHasPlaySlots().compareAndSet(false, true);
        //表示第一次玩该slots游戏
        if (flag) {
            if (roomController != null && roomController.getRoom().getType() == RoomType.SLOTS_TEAM_UP_ROOM) {
                //获取保证金(即水池)
                CommonResult<Long> poolResult = checkAndGetPredictCostGoldNum(roomController);
                if (!poolResult.success()) {
                    result.code = poolResult.code;
                    return result;
                }
            }

            SpecialResultLibCfg libCfg = getLibCfgByModelId(gameData.getGameType(), SlotsConst.Common.FIRST_GAME_GET_MODEL_ID);
            if (libCfg == null) {
                log.warn("获取结果库配置失败 playerId = {},gameType = {},roomCfgId = {},modelId = {}", gameData.getPlayerId(), gameData.getGameType(), gameData.getRoomCfgId(), SlotsConst.Common.FIRST_GAME_GET_MODEL_ID);
                result.code = Code.NOT_FOUND;
                return result;
            }
            historySlotsDao.addGameType(gameData.getPlayerId(), gameData.getGameType());
            result.data = libCfg;
            log.debug("玩家第一次玩该slots游戏，选择默认模式，playerId = {},gameType = {},modelId = {}", gameData.getPlayerId(), gameData.getGameType(), SlotsConst.Common.FIRST_GAME_GET_MODEL_ID);

        } else {
            if (roomController == null) {
                //获取水池
                Number poolValue = slotsPoolDao.getBigPoolByRoomCfgId(gameData.getGameType(), gameData.getRoomCfgId());
                if (poolValue == null) {
                    log.warn("获取水池失败 playerId = {},gameType = {},roomCfgId = {}", gameData.getPlayerId(), gameData.getGameType(), gameData.getRoomCfgId());
                    result.code = Code.NOT_FOUND;
                    return result;
                }

                //计算偏差范围
                long diff = BigDecimal.valueOf(poolValue.longValue() - poolInit).divide(BigDecimal.valueOf(poolInit), 6, RoundingMode.HALF_UP).multiply(GameConstant.TEN_THOUSAND_BD).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
                SpecialResultLibCfg libCfg = getLibCfgByPoolDiff(diff);
                if (libCfg == null) {
                    log.warn("获取结果库配置失败 playerId = {},gameType = {},roomCfgId = {},diff = {}", gameData.getPlayerId(), gameData.getGameType(), gameData.getRoomCfgId(), diff);
                    result.code = Code.NOT_FOUND;
                    return result;
                }
                result.data = libCfg;
                log.debug("根据水池偏差计算获取滚轴模式配置  playerId = {},poolValue = {},poolInit = {},diff = {},modelId = {}", gameData.getPlayerId(), poolValue, poolInit, diff, libCfg.getModelId());
            } else if (roomController.getRoom().getType() == RoomType.SLOTS_TEAM_UP_ROOM) {  //slots 好友房
                //获取保证金(即水池)
                CommonResult<Long> poolResult = checkAndGetPredictCostGoldNum(roomController);
                if (!poolResult.success()) {
                    result.code = poolResult.code;
                    return result;
                }

                //slotsRoom.pool中存的是玩家累计的保证金
                poolInit = roomController.getRoom().getPool();

                //计算偏差范围
                long diff = BigDecimal.valueOf(poolResult.data - poolInit).divide(BigDecimal.valueOf(poolInit), 6, RoundingMode.HALF_UP).multiply(GameConstant.TEN_THOUSAND_BD).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
                SpecialResultLibCfg libCfg = getLibCfgByPoolDiff(diff);
                if (libCfg == null) {
                    log.warn("好友房获取结果库配置失败 playerId = {},gameType = {},roomCfgId = {},roomId = {},diff = {}", gameData.getPlayerId(), gameData.getGameType(), gameData.getRoomCfgId(), gameData.getRoomId(), diff);
                    result.code = Code.NOT_FOUND;
                    return result;
                }
                result.data = libCfg;
                log.debug("根据水池偏差计算获取滚轴模式配置  playerId = {},roomId = {},poolValue = {},poolInit = {},diff = {},modelId = {}", gameData.getPlayerId(), gameData.getRoomId(), poolResult.data, poolInit, diff, libCfg.getModelId());
            } else {
                result.code = Code.NOT_FOUND;
                log.warn("未找到该结果库配置2 playerId = {},gameType = {},roomCfgId = {},roomType = {}", gameData.getPlayerId(), gameData.getGameType(), gameData.getRoomCfgId(), roomController.getRoom().getType());
            }
        }
        return result;
    }

    /**
     * 获取 PlayerGameData 对象
     *
     * @param playerController
     * @return
     */
    public T getPlayerGameData(PlayerController playerController) {
        T playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null) {
            return null;
        }

        if (playerGameData.getRoomCfgId() != playerController.getPlayer().getRoomCfgId()) {
            log.warn("玩家 gameData 不匹配当前 PlayerController, playerId={}, playerGameData.roomCfgId={}, playerController.roomCfgId={}", playerController.playerId(), playerGameData.getRoomCfgId(), playerController.getPlayer().getRoomCfgId());
        }
        playerGameData.setLastActiveTime(System.currentTimeMillis());
        return playerGameData;
    }

    public T getPlayerGameData(long playerId) {
        return this.gameDataMap.get(playerId);
    }

    public void removePlayerGameData(long playerId) {
        this.gameDataMap.remove(playerId);
        taskManager.onExit(playerId);
    }

    public void exitOldPlayerGameDataOnEnter(long playerId, int roomCfgId, long roomId) {
        T playerGameData = getPlayerGameData(playerId);
        if (playerGameData == null) {
            return;
        }

        if (isCurrentEnterGameData(playerGameData.getRoomCfgId(), playerGameData, roomCfgId, roomId)) {
            return;
        }

        Player player = playerGameData.getPlayer();
        log.info("slots清除老数据 playerId:{} gameType:{} roomConfigId:{} roomId:{}", playerId, playerGameData.getGameType(), playerGameData.getRoomCfgId(),
                player == null ? "null" : player.getRoomId());
        exitPlayerGameData(playerId, playerGameData);
    }

    private boolean isCurrentEnterGameData(int cacheRoomCfgId, T playerGameData, int enterRoomCfgId, long enterRoomId) {
        if (cacheRoomCfgId != enterRoomCfgId) {
            return false;
        }
        if (enterRoomId <= 0) {
            return true;
        }
        PlayerController playerController = playerGameData.getPlayerController();
        return playerController != null && playerController.getPlayer().getRoomId() == enterRoomId;
    }

    protected void exitPlayerGameData(long playerId, T playerGameData) {
        long now = System.currentTimeMillis();
        playerGameData.setOfflineTime(now);
        playerGameData.setOnline(false);
        if (playerGameData.getOfflineEventMap() != null && !playerGameData.getOfflineEventMap().isEmpty()) {
            for (Map.Entry<Integer, OffLineEventData> en : playerGameData.getOfflineEventMap().entrySet()) {
                if (en.getValue().isAction()) {
                    continue;
                }
                onAutoExitAction(playerGameData, en.getKey());
                en.getValue().setAction(true);
            }
        }
        PlayerController playerController = playerGameData.getPlayerController();
        if (playerController != null) {
            slotsRoomManager.exitRoom(playerController);
        }
        offlineSaveGameData(playerGameData);
        removePlayerGameData(playerId);
        playerAllSlotsDataDao.saveToRedis(playerGameData.getPlayerAllSlotsData());
    }

    /**
     * 创建玩家玩游戏的数据存储对象
     *
     * @param playerController
     * @param enterType        进入方式：1=赛季入口，只加载 SeasonGem 效果不加载研发技能；2=拜访入口，加载房主研发技能
     * @return
     */
    @SuppressWarnings("unchecked")
    public T createPlayerGameData(PlayerController playerController, int enterType) throws Exception {
        PlayerAllSlotsData playerAllSlotsData = playerAllSlotsDataDao.getFromAllDB(playerController.playerId());
        if (playerAllSlotsData == null) {
            playerAllSlotsData = new PlayerAllSlotsData();
            playerAllSlotsData.setPlayerId(playerController.playerId());
        }

        boolean seasonEntry = enterType == 1;
        //客座赌局使用房主研发属性，普通游戏仍使用玩家自己的技能
        SimVisitTrialSession visitSession = enterType == 2
                ? simVisitQuotaService.getTrialSession(playerController.playerId()) : null;
        boolean activeVisit = visitSession != null
                && visitSession.activeFor(playerController.playerId(), this.gameType, System.currentTimeMillis());
        long skillOwnerId = activeVisit ? visitSession.getOwnerId() : playerController.playerId();
        SimSkillsData simSkillsData = null;
        SeasonSlotsSessionData seasonSessionData = null;
        if (seasonEntry) {
            //赛季币余额和宝石效果在同一次 RPC 中获取；失败时保留赛季入口语义，但不错误回退到研发技能。
            CommonResult<SeasonSlotsSessionData> seasonResult = slotsRPCLinkManager.getSeasonSlotsSessionData(
                    playerController.playerId(), this.gameType, playerController.ipAddress());
            if (seasonResult != null && seasonResult.success() && seasonResult.data != null) {
                seasonSessionData = seasonResult.data;
            } else {
                seasonSessionData = new SeasonSlotsSessionData();
                log.warn("获取赛季 slots 进场快照失败，使用无宝石加成快照 playerId={},gameType={},code={}",
                        playerController.playerId(), this.gameType, seasonResult == null ? null : seasonResult.code);
            }
        } else {
            //普通入口走 RPC 取 sim 最新技能；sim 不可达才回退读库，避免定时落库前的脏读。
            CommonResult<SimSkillsData> skillResult = slotsRPCLinkManager.getSimSkillData(
                    skillOwnerId, this.gameType, playerController.ipAddress());
            if (skillResult != null && skillResult.success()) {
                simSkillsData = skillResult.data;
            } else {
                simSkillsData = slotsSkillService.getSkillDataByGameType(skillOwnerId, this.gameType);
            }
        }
        //获取sim节点
        ClusterClient simClusterClient = simNodeService.getSimClusterClient(playerController.playerId(), playerController.ipAddress());

        T playerGameData = getPlayerGameData(playerController);
        if (playerGameData != null) {
            playerGameData.setCreateTime(TimeHelper.nowInt());
            playerGameData.setOnline(true);
            playerGameData.setOfflineTime(0);
            playerGameData.setPlayerController(playerController);
            playerGameData.setOfflineEventMap(initOffLineEvent());
            playerGameData.setPlayerAllSlotsData(playerAllSlotsData);

            applyEntrySessionData(playerGameData, simSkillsData, seasonSessionData, seasonEntry);
            applyVisitSession(playerGameData, activeVisit ? visitSession : null);
            if (!seasonEntry) {
                playerGameData.setSeasonFreeGameCandidate(seasonFreeGameService.freeGameCandidate(
                        playerController.playerId(), this.gameType, System.currentTimeMillis()));
            }
            playerGameData.setSimClient(simClusterClient);
            playerGameData.setEnterType(enterType);
            return playerGameData;
        }

        long playerId = playerController.playerId();
        int roomCfgId = playerController.getPlayer().getRoomCfgId();
        log.debug("从db中获取的 getPlayerId = {}", playerId);
        playerGameData = (T) playerGameDataDao.getPlayerGameDataByPlayerId(playerId, roomCfgId, playerController.roomId(), playerGameDataClass);
        if (playerGameData == null) {
            Constructor<T> constructor = this.playerGameDataClass.getConstructor();
            playerGameData = constructor.newInstance();

            //是否玩过该类游戏
            boolean hasPlay = historySlotsDao.hasPlaySlots(playerId, playerGameData.getGameType());

            playerGameData.setGameType(playerController.getPlayer().getGameType());
            playerGameData.setRoomCfgId(roomCfgId);
            playerGameData.getHasPlaySlots().set(hasPlay);
            playerGameData.setCreateTime(TimeHelper.nowInt());
            playerGameData.setRoomId(playerController.roomId());
            playerGameData.setPlayerId(playerController.playerId());
            //设置默认押注
            BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(playerGameData.getRoomCfgId());
            playerGameData.setOneBetScore(baseRoomCfg.getDefaultBet().getFirst());
            playerGameData.setAllBetScore(oneLineToAllStake(playerGameData.getOneBetScore()));
        }
        playerGameData.setEnterType(enterType);
        playerGameData.setOfflineTime(0);
        playerGameData.setOnline(true);
        playerGameData.setLastActiveTime(System.currentTimeMillis());
        playerGameData.setPlayerController(playerController);
        playerGameData.setOfflineEventMap(initOffLineEvent());
        playerGameData.setPlayerAllSlotsData(playerAllSlotsData);

        applyEntrySessionData(playerGameData, simSkillsData, seasonSessionData, seasonEntry);
        applyVisitSession(playerGameData, activeVisit ? visitSession : null);
        if (!seasonEntry) {
            playerGameData.setSeasonFreeGameCandidate(seasonFreeGameService.freeGameCandidate(
                    playerId, this.gameType, System.currentTimeMillis()));
        }
        playerGameData.setSimClient(simClusterClient);

        //保存到缓存中
        this.gameDataMap.put(playerId, playerGameData);
        return playerGameData;
    }

    /**
     * 将入口类型对应的效果快照写入仅运行时的玩家数据，并清理上一次会话可能遗留的下注解锁缓存。
     */
    private void applyEntrySessionData(T playerGameData, SimSkillsData skillsData,
                                       SeasonSlotsSessionData seasonSessionData, boolean seasonEntry) {
        int exhaustedDailyKey = playerGameData.getSeasonSlotsSessionData().getSeasonFreeExhaustedDailyKey();
        SeasonSlotsSessionData runtimeData = seasonSessionData == null
                ? new SeasonSlotsSessionData() : seasonSessionData;
        // 普通入口也可能是赛季每日免费局候选，因此必须用显式标记区分是否启用赛季币和宝石效果。
        runtimeData.setSeasonEnter(seasonEntry);
        // 保留当日已耗尽状态，防止同一天重新进机台后再次发起无意义的跨节点免费次数申请。
        runtimeData.setSeasonFreeExhaustedDailyKey(exhaustedDailyKey);
        playerGameData.setSkillsMap(!seasonEntry && skillsData != null ? skillsData.getSkillsMap() : null);
        playerGameData.setSeasonSlotsSessionData(runtimeData);
        playerGameData.setTmpUnlockedStakeSet(null);
    }

    private void applyVisitSession(T playerGameData, SimVisitTrialSession session) {
        if (session == null) {
            playerGameData.setVisitOwnerId(0);
            playerGameData.setVisitCasinoId(0);
            return;
        }
        playerGameData.setVisitOwnerId(session.getOwnerId());
        playerGameData.setVisitCasinoId(session.getCasinoId());
    }

    /**
     * 获取 specialResultLib 中的type
     *
     * @param playerGameData
     * @param modelId
     * @return
     */
    protected CommonResult<Integer> getResultLibType(T playerGameData, int modelId, RoomType roomType) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        int gameType = playerGameData.getGameType();
        PropInfo propInfo;
        if (roomType == null) {
            propInfo = getGenerateManager().getSpecialResultLibCacheData().getResultLibTypePropInfoMap().get(modelId);
        } else if (roomType == RoomType.SLOTS_TEAM_UP_ROOM) {
            propInfo = getGenerateManager().getSpecialResultLibCacheData().getNoJackpotResultLibTypePropInfoMap().get(modelId);
        } else {
            log.warn("获取 resultLibType 是，不支持该roomType, gameType = {},modelId = {},roomType = {}", gameType, modelId, roomType);
            result.code = Code.FAIL;
            return result;
        }

        if (propInfo == null) {
            log.warn("未找到 specialResultLib 中 typeProp相关的权重信息11 modelId = {},gameType = {}", modelId, gameType);
            result.code = Code.NOT_FOUND;
            return result;
        }

        //赛季入口使用 SeasonGem，其他入口继续使用 ResearchSkills；两类配置在此处互斥。
        if (playerGameData.isSeason()) {
            propInfo = slotsSkillService.useSeasonGemLibTypeBonus(
                    playerGameData.getSeasonSlotsSessionData(), propInfo);
        } else {
            propInfo = slotsSkillService.useLibTypeSkill(this.gameType, playerGameData.getSkillsMap(), propInfo);
        }

        Integer type = propInfo.getRandKey();
        if (type == null) {
            log.warn("specialResultLib 中 typeProp随机失败 modelId = {},gameType = {}", modelId, gameType);
            result.code = Code.FAIL;
            return result;
        }
        result.data = type;
        return result;
    }

    /**
     * 获取 specialResultLib 中的倍数区间
     *
     * @param modelId
     * @param libType  specialResultLib 中的type
     * @param betValue 下注金额
     * @return
     */
    protected CommonResult<Integer> getResultLibSection(T playerGameData, int modelId, int libType, long betValue, int slotsVipType, int allBetCount, int prizelessCount) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        Map<Integer, PropInfo> tempPropMap;
        if (betValue < 1) {
            tempPropMap = getGenerateManager().getSpecialResultLibCacheData().getResultLibSectionPropMap().get(modelId);
        } else {
            tempPropMap = getGenerateManager().getSpecialResultLibCacheData().getPropMap(modelId, betValue, slotsVipType, allBetCount, prizelessCount);
        }
        if (tempPropMap == null) {
            log.warn("未找到 specialResultLib 中 section 相关的权重信息1 modelId = {},gameType = {},libType = {},betValue = {},slotsVipType = {},allBetCount = {},prizelessCount = {}", modelId, this.gameType, libType, betValue, slotsVipType, allBetCount, prizelessCount);
            result.code = Code.NOT_FOUND;
            return result;
        }
        PropInfo propInfo = tempPropMap.get(libType);
        if (propInfo == null || propInfo.getSum() < 1) {
            log.warn("未找到 specialResultLib 中 section 相关的权重信息2 modelId = {},gameType = {},libType = {},betValue = {},slotsVipType = {},allBetCount = {},prizelessCount = {}", modelId, this.gameType, libType, betValue, slotsVipType, allBetCount, prizelessCount);
            result.code = Code.NOT_FOUND;
            return result;
        }

        //赛季入口使用 SeasonGem，其他入口继续使用 ResearchSkills；两类配置在此处互斥。
        if (playerGameData.isSeason()) {
            propInfo = slotsSkillService.useSeasonGemSectionBonus(
                    playerGameData.getSeasonSlotsSessionData(), propInfo, libType);
        } else {
            propInfo = slotsSkillService.useSectionSkill(
                    this.gameType, playerGameData.getSkillsMap(), propInfo, libType);
        }

        Integer index = propInfo.getRandKey();
        if (index == null) {
            log.warn("未找到 specialResultLib 中 section 相关的权重信息3 modelId = {},gameType = {},libType = {},index = {},betValue = {},slotsVipType = {},allBetCount = {},prizelessCount = {}", modelId, this.gameType, libType, index, betValue, slotsVipType, allBetCount, prizelessCount);
            result.code = Code.FAIL;
            return result;
        }
        result.data = index;
        int[] section = getGenerateManager().getSpecialResultLibCacheData().getResultLibSectionMap().get(libType).get(index);
        log.debug("成功获取区间 modelId = {},gameType = {},libType = {},intdex = {},sectionBegin = {},sectionEnd = {},betValue = {},slotsVipType = {},allBetCount = {},prizelessCount = {}", modelId, this.gameType, libType, index, section[0], section[1], betValue, slotsVipType, allBetCount, prizelessCount);
        return result;
    }

    /**
     * 获取奖池信息
     *
     * @param playerController
     * @param stake
     * @return
     */
    public G getPoolValue(PlayerController playerController, long stake) {
        try {
            G gameRunInfo = createGameRunInfo(playerController.playerId(), Code.SUCCESS);

            BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
            if (baseInitCfg == null || baseInitCfg.getPrizePoolIdList() == null || baseInitCfg.getPrizePoolIdList().size() != 4) {
                gameRunInfo.setCode(Code.SAMPLE_ERROR);
                return gameRunInfo;
            }

            gameRunInfo.setMini(getPoolValueByPoolId(baseInitCfg.getPrizePoolIdList().get(0), stake));
            gameRunInfo.setMinor(getPoolValueByPoolId(baseInitCfg.getPrizePoolIdList().get(1), stake));
            gameRunInfo.setMajor(getPoolValueByPoolId(baseInitCfg.getPrizePoolIdList().get(2), stake));
            gameRunInfo.setGrand(getPoolValueByPoolId(baseInitCfg.getPrizePoolIdList().get(3), stake));
            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    public long getPoolValueByPoolId(int poolId, long stake) {
        PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
        return calPoolValue(stake, poolCfg.getGrowthRate(), poolCfg.getFakePoolInitTimes(), poolCfg.getFakePoolMax());
    }

    /**
     * 从奖池扣除，并给玩家加钱(普通中奖)
     *
     * @param gameRunInfo
     * @param playerGameData
     */
    protected G rewardFromBigPool(G gameRunInfo, T playerGameData) {
        if (gameRunInfo.getBigPoolTimes() < 1) {
            return gameRunInfo;
        }

        long addGold = playerGameData.getOneBetScore() * gameRunInfo.getBigPoolTimes();
        return rewardFromBigPool(gameRunInfo, playerGameData, addGold, AddType.SLOTS_BET_REWARD);
    }

    /**
     * 从奖池扣除，并给玩家加钱(普通中奖)
     *
     * @param gameRunInfo
     * @param playerGameData
     */
    protected G rewardFromBigPool(G gameRunInfo, T playerGameData, long addGold, AddType addType) {
        if (addGold < 1) {
            return gameRunInfo;
        }

        if (playerGameData.getRoomType() == null) {
            if (playerGameData.isSeason()) {
                //从赛季进入(credit-first): 先给玩家发赛季币(幂等RPC), 成功后再扣奖池(货币无关); 失败则不扣池、不算中奖
                if (!seasonReward(playerGameData, addGold)) {
                    gameRunInfo.setCode(Code.FAIL);
                    return gameRunInfo;
                }
                slotsPoolDao.addToBigPool(this.gameType, playerGameData.getRoomCfgId(), -addGold);
                gameRunInfo.setAllWinGold(addGold);
                return gameRunInfo;
            }
            CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.getPlayerId(), this.gameType, playerGameData.getRoomCfgId(), addGold, addType);
            if (!result.success()) {
                log.warn("给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                gameRunInfo.setCode(result.code);
                return gameRunInfo;
            }
            gameRunInfo.setAllWinGold(addGold);
            playerGameData.setPlayer(result.data);
        } else if (playerGameData.getRoomType() == RoomType.SLOTS_TEAM_UP_ROOM) {
            int roomCfgId = playerGameData.getRoomCfgId();
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
            if (warehouseCfg == null) {
                log.warn("给玩家添加金币失败warehouseCfg is null gameType = {},addValue = {}", this.gameType, addGold);
                gameRunInfo.setCode(Code.SAMPLE_ERROR);
                return gameRunInfo;
            }
            CommonResult<Pair<Player, Long>> result = roomSlotsPoolDao.rewardFromBigPool(playerGameData.getPlayerId(), playerGameData.getRoomId(), addGold, warehouseCfg.getTransactionItemId(), addType);
            if (!result.success()) {
                if (result.code == Code.AMOUNT_OF_RESERVES_IS_NOT_ENOUGHT) {
                    sendRoomAmountNotEnough(playerGameData.getPlayerId());
                    if (gameRunInfo.getStatus() > 0) {
                        log.warn("房间准备金不足以赔付 roomId = {},gameType = {},addValue = {}", playerGameData.getRoomId(), this.gameType, addGold);
                        gameRunInfo.setCode(Code.SUCCESS);
                        gameRunInfo.setAllWinGold(addGold);
                        return gameRunInfo;
                    }
                    return handleEmptyPrizePool(gameRunInfo, playerGameData);
                }
                log.warn("给玩家添加金币失败 roomId = {},gameType = {},addValue = {}", playerGameData.getRoomId(), this.gameType, addGold);
                gameRunInfo.setCode(result.code);
                return gameRunInfo;
            }
            slotsRoomManager.updatePoolValue(playerGameData.getRoomId(), result.data.getSecond());
            gameRunInfo.setAllWinGold(addGold);
            playerGameData.setPlayer(result.data.getFirst());
        } else {
            log.warn("无法识别玩家的roomType，加钱失败 playerId = {},roomType = {}", playerGameData.getPlayerId(), playerGameData.getRoomType());
            gameRunInfo.setCode(Code.FAIL);
        }
        return gameRunInfo;
    }

    public void sendRoomAmountNotEnough(long playerId) {
        NotifySlotsStatus slotsStatus = new NotifySlotsStatus();
        slotsStatus.pauseType = 2;
        clusterSystem.sendToPlayer(slotsStatus, playerId);
    }

    /**
     * 从奖池扣除钱(jackpot用)
     * 4个奖池专用
     *
     * @param gameRunInfo
     * @param playerGameData
     */
    protected void rewardFromSmallPool(GameRunInfo gameRunInfo, T playerGameData, List<Integer> jackpotIds) {
        if (jackpotIds == null || jackpotIds.isEmpty()) {
            return;
        }

        RoomType roomType = playerGameData.getRoomType();
        if (roomType != null) {
            return;
        }

        for (int poolId : jackpotIds) {
            //下注金额总金额，不是单线金额
            long poolValue = getPoolValueByPoolId(poolId, playerGameData.getAllBetScore());
            if (poolValue < 1) {
                continue;
            }

            if (playerGameData.isSeason()) {
                //从赛季进入(credit-first): 先发赛季币, 成功后再扣小池(货币无关); 失败则不扣池、不算中奖
                if (!seasonReward(playerGameData, poolValue)) {
                    return;
                }
                slotsPoolDao.addToSmallPool(this.gameType, playerGameData.getRoomCfgId(), -poolValue);
                slotsPoolDao.updatePoolCD(poolId);
                playerGameData.addSmallPoolReward(poolValue);
                gameRunInfo.addSmallPoolGold(poolValue);
                recordJackpotStat(gameRunInfo, poolId, poolValue);
                continue;
            }

            //给玩家加钱
            CommonResult<Player> result = slotsPoolDao.rewardFromSmallPool(playerGameData.getPlayerId(), this.gameType, playerGameData.getRoomCfgId(), poolValue, poolId, AddType.SLOTS_TRAIN, poolId + "");
            if (!result.success()) {
                log.warn("从小池子扣除，并给玩家加钱失败 code = {}", result.code);
                return;
            }
            playerGameData.addSmallPoolReward(poolValue);
            gameRunInfo.addSmallPoolGold(poolValue);
            recordJackpotStat(gameRunInfo, poolId, poolValue);
            playerGameData.setPlayer(result.data);

            log.info("玩家奖池中奖 playerId = {},gameType = {},roomCfgId = {},poolId = {},poolValue = {}", playerGameData.getPlayerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), poolId, poolValue);
        }
    }

    /**
     * 从奖池扣除钱(jackpot用)
     * 单奖池专用
     *
     * @param gameRunInfo
     * @param playerGameData
     */
    protected void rewardFromSmallPool2(GameRunInfo gameRunInfo, T playerGameData, List<Integer> jackpotIds) {
        if (jackpotIds == null || jackpotIds.isEmpty()) {
            return;
        }

        RoomType roomType = playerGameData.getRoomType();
        if (roomType != null) {
            return;
        }

        for (int poolId : jackpotIds) {
            PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
            if (poolCfg == null) {
                continue;
            }

            //检查是否中大奖 (赛季币/金币统一收口, 内部按 seasonCurrency 分流)
            CommonResult<Long> result = rewardByRatioSmallPoolCurrency(playerGameData, poolCfg.getTruePool(), poolCfg.getId(), AddType.SLOTS_JACKPOT_REWARD);
            if (!result.success()) {
                log.warn("从小池子扣除，并给玩家加钱失败2 code = {}", result.code);
                return;
            }

            playerGameData.addSmallPoolReward(result.data);
            gameRunInfo.addSmallPoolGold(result.data);
            recordJackpotStat(gameRunInfo, poolId, result.data);

            log.info("玩家从单奖池中奖 playerId = {},gameType = {},roomCfgId = {},poolId = {},poolValue = {}", playerGameData.getPlayerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), poolId, result.data);
        }
    }

    /**
     * 奖池中奖后按档位记入本次旋转结果 (mini/minor/major/grand 的金额与触发次数),
     * 供 sim 经营信息统计和任务条件 12204 使用。
     * <p>
     * 档位 = poolId 在 BaseInit.PrizePoolIdList 中的下标 (0~3); 不在列表中的奖池不计。
     * 只配了单个特殊奖池 (全屏奖/夺宝分散奖等) 的游戏不属于四档奖池, 整体不计入档位统计。
     */
    protected void recordJackpotStat(GameRunInfo<?> gameRunInfo, int poolId, long value) {
        if (gameRunInfo == null || value < 1) {
            return;
        }
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        if (baseInitCfg == null || baseInitCfg.getPrizePoolIdList() == null
                || baseInitCfg.getPrizePoolIdList().size() != JACKPOT_TIER_COUNT) {
            return;
        }
        int index = baseInitCfg.getPrizePoolIdList().indexOf(poolId);
        switch (index) {
            case 0 -> gameRunInfo.setMini(gameRunInfo.getMini() + value);
            case 1 -> gameRunInfo.setMinor(gameRunInfo.getMinor() + value);
            case 2 -> gameRunInfo.setMajor(gameRunInfo.getMajor() + value);
            case 3 -> gameRunInfo.setGrand(gameRunInfo.getGrand() + value);
            default -> {
                return;
            }
        }
        gameRunInfo.addJackpotCount(index + 1);
    }

    /**
     * 计算奖池金额
     *
     * @param stake
     * @param growthRate
     * @return
     */
    public long calPoolValue(long stake, List<Integer> growthRate, int initTimes, int maxTimes) {
        return calPoolValue(stake, growthRate, initTimes, maxTimes, 0);
    }

    /**
     * 计算奖池金额
     *
     * @param stake
     * @param growthRate
     * @param delayTime  延迟时间
     * @return
     */
    public long calPoolValue(long stake, List<Integer> growthRate, int initTimes, int maxTimes, int delayTime) {
        //押注
        BigDecimal stakeBigDecimal = BigDecimal.valueOf(stake);
        //奖池初始金额
        BigDecimal initPoolBigDecimal = stakeBigDecimal.multiply(BigDecimal.valueOf(initTimes));
        //奖池上限金额
        BigDecimal maxPoolBigDecimal = stakeBigDecimal.multiply(BigDecimal.valueOf(maxTimes));

        //间隔时间
        int timeValue = growthRate.get(0);
        BigDecimal intervalTime = BigDecimal.valueOf(timeValue);

        //增加万分比
        int propValue = growthRate.get(1);
        BigDecimal prop = BigDecimal.valueOf(propValue).divide(GameConstant.TEN_THOUSAND_BD, 4, RoundingMode.HALF_UP);

        //循环时间
        BigDecimal circulTimeBigDecimal = BigDecimal.ONE.divide(prop, 4, RoundingMode.HALF_UP).multiply(intervalTime);

        //余数y
        int y = (TimeHelper.getNowDaySeconds() + delayTime) % circulTimeBigDecimal.intValue();

        //实际金额
        BigDecimal step1 = BigDecimal.valueOf(y).divide(intervalTime, 4, RoundingMode.HALF_UP);
        BigDecimal step2 = step1.multiply(prop);
        BigDecimal step3 = step2.multiply(maxPoolBigDecimal.subtract(initPoolBigDecimal));
        return initPoolBigDecimal.add(step3).longValue();
    }


    @Override
    public void onTimer(TimerEvent e) {
        if (this.gameUpdatePoolEvent == e) {
            gameUpdatePool();
        } else if (this.clearAllLibEvent == e) {
            getResultLibDao().clearOldRedisLib(this.gameType);
            this.clearAllLibEvent = null;
            getResultLibDao().removeGenerateLock(this.gameType);
        }
    }

    /**
     * 检查玩家状态(离线和活跃)
     */
    private void checkPlayerStatus() {
        long timeMillis = System.currentTimeMillis();
        for (Map.Entry<Long, T> gameData : this.gameDataMap.entrySet()) {
            T gameDataValue = gameData.getValue();
            //是否需要回存
            if (gameDataValue.isOnline()) {
                //检查slots房间中，在线玩家是否活跃
                checkActive(gameDataValue, timeMillis);
                continue;
            }

            if (gameDataValue.getOfflineTime() + getOfflineDeleteMills() <= timeMillis) {  //离线多少秒执行数据删除
                offlineDelete(gameDataValue, gameDataValue.getPlayerController(), timeMillis);
            } else if (gameDataValue.getOfflineEventMap() != null && !gameDataValue.getOfflineEventMap().isEmpty()) {  //离线多少秒执行特殊处理
                for (Map.Entry<Integer, OffLineEventData> en : gameDataValue.getOfflineEventMap().entrySet()) {
                    //检查该事件是否已经执行
                    if (en.getValue().isAction()) {
                        continue;
                    }
                    //检查时间
                    if (en.getValue().getActionMills() > timeMillis) {
                        continue;
                    }
                    //开始执行
                    offlineImplement(gameDataValue, en.getValue());
                }
            }
        }
    }

    private void checkActive(T playerGameData, long now) {
        if (getRoomType() == null) {
            return;
        }

        //分发到对应的线程
        PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(playerGameData.getPlayerId(), 0, new BaseHandler<String>() {
            @Override
            public void action() {
                long diff = now - playerGameData.getLastActiveTime();
                if (diff > playerRoomIldeIimeMills) {
                    //玩家长时间未操作
                    slotsRoomManager.playerRoomIdle(playerGameData.getRoomId(), playerGameData.getPlayerId());
                }
            }
        }.setHandlerParamWithSelf("slots checkActive"));
    }

    /**
     * 离线执行
     *
     * @param playerGameData   玩家数据
     * @param offLineEventData 事件
     */
    private void offlineImplement(T playerGameData, OffLineEventData offLineEventData) {
        //分发到对应的线程
        PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(playerGameData.getPlayerId(), 0, new BaseHandler<String>() {
            @Override
            public void action() {
                if (playerGameData.isOnline() || offLineEventData.isAction()) {
                    return;
                }
                onAutoExitAction(playerGameData, offLineEventData.getId());
                //标记该事件已执行
                offLineEventData.setAction(true);
            }
        }.setHandlerParamWithSelf("slots offlineImplement"));
    }

    /**
     * 离线删除
     *
     * @param playerGameData   玩家数据
     * @param playerController 玩家数据
     */
    protected void offlineDelete(T playerGameData, PlayerController playerController, long now) {
        //分发到对应的线程
        PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(playerGameData.getPlayerId(), 0, new BaseHandler<String>() {
            @Override
            public void action() {
                T playerGameData = getPlayerGameData(playerController);
                if (playerGameData == null) {
                    return;
                }
                if (playerGameData.isOnline()) {
                    return;
                }
                slotsRoomManager.exitRoom(playerController);
                offlineSaveGameData(playerGameData);
                removePlayerGameData(playerGameData.getPlayerId());
                log.debug("保存离线玩家数据 playerId = {}", playerController.playerId());
            }
        }.setHandlerParamWithSelf("slots offlineDelete"));
    }

    /**
     * 玩家离线后删除缓存保存数据的时间
     *
     * @return
     */
    protected int getOfflineDeleteMills() {
        return SlotsConst.Common.MAX_OFFLINE_TIME;
    }

    /**
     * 离线自动踢出玩家
     *
     * @param gameData 玩家数据
     */
    protected void onAutoExitAction(T gameData, int eventId) {
    }


    protected abstract <D extends AbstractResultLibDao> D getResultLibDao();

    protected abstract <D extends AbstractSlotsGenerateManager> D getGenerateManager();

    /**
     * 更新奖池
     */
    protected void gameUpdatePool() {
        Map<Integer, Long> tmpPoolValueMap = new HashMap<>();
        Map<Object, Object> smallPool = slotsPoolDao.getSmallPoolByRoomCfgId(this.gameType);
        Map<Object, Object> fakeSmallPool = slotsPoolDao.getFakeSmallPoolByRoomCfgId(this.gameType);

        for (Map.Entry<Object, Object> en : smallPool.entrySet()) {
            int roomCfgId = Integer.parseInt(en.getKey().toString());
            long smallPoolValue = Long.parseLong(en.getValue().toString());

            Object o = fakeSmallPool.get(roomCfgId);
            if (o != null) {
                long fakeSmallPoolValue = Long.parseLong(o.toString());
                if (fakeSmallPoolValue > smallPoolValue) {
                    smallPoolValue = fakeSmallPoolValue;
                }
            }

            tmpPoolValueMap.put(roomCfgId, smallPoolValue);
        }
//        log.debug("更新奖池 gameType = {},map = {}", this.gameType, tmpPoolValueMap);
        this.poolValueMap = tmpPoolValueMap;
    }

    /**
     * 玩家离线保存gameData
     */
    protected void offlineSaveGameData(T gameData) {
        playerGameDataDao.savePlayerGameData(gameData);
    }

/*****************************************************************************************************************************/

    /**
     * room配置
     */
    protected void baseRoomConfig() {
        Map<Integer, BaseRoomCfg> tempRoomCfgMap = new HashMap<>();
        for (Map.Entry<Integer, BaseRoomCfg> en : GameDataManager.getBaseRoomCfgMap().entrySet()) {
            BaseRoomCfg cfg = en.getValue();
            if (cfg.getGameType() == this.gameType) {
                tempRoomCfgMap.put(cfg.getId(), cfg);
            }
        }

        if (tempRoomCfgMap.isEmpty()) {
            throw new IllegalArgumentException("该游戏baseRoomCfg 为空,初始化失败 gameType = " + gameType);
        }
        this.roomCfgMap = tempRoomCfgMap;
    }

    /**
     * baseline配置
     */
    protected void baseLineConfig() {
        //gameMode -> lineId -> cfg
        Map<Integer, Map<Integer, BaseLineCfg>> tmpBaseLineCfgMap = new HashMap<>();

        for (Map.Entry<Integer, BaseLineCfg> en : GameDataManager.getBaseLineCfgMap().entrySet()) {
            BaseLineCfg cfg = en.getValue();
            if (cfg.getGameType() == this.gameType) {
                Map<Integer, BaseLineCfg> tmpMap = tmpBaseLineCfgMap.computeIfAbsent(cfg.getGameMode(), k -> new HashMap<>());
                tmpMap.put(cfg.getLineId(), cfg);
            }
        }
        this.baseLineCfgMap = tmpBaseLineCfgMap;
    }


    /**
     * 特殊玩法
     */
    protected void specialPlayConfig() {

    }

    /**
     * 小游戏配置
     */
    protected void specialAuxiliaryConfig() {

    }

    protected void globalConfig() {
        Map<Integer, int[]> tmpBigWinShowMap = new HashMap<>();

        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_SWEET, SlotsConst.BigWinShow.SWEET, tmpBigWinShowMap);
        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_BIG, SlotsConst.BigWinShow.BIG, tmpBigWinShowMap);
        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_MEGA, SlotsConst.BigWinShow.MEGA, tmpBigWinShowMap);
        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_EPIC, SlotsConst.BigWinShow.EPIC, tmpBigWinShowMap);
        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_LEGENDARY, SlotsConst.BigWinShow.LEGENDARY, tmpBigWinShowMap);
        this.playerRoomIldeIimeMills = GameDataManager.getGlobalConfigCfg(SlotsConst.GlobalConfig.ID_ROOM_PLAYER_ILDE_TIME_MILLS).getIntValue();
        this.bigWinShowMap = tmpBigWinShowMap;
    }

    protected void calGlobalBigWinShow(int id, int pbShowId, Map<Integer, int[]> map) {
        GlobalConfigCfg cfg = GameDataManager.getGlobalConfigCfg(id);
        String[] arr = cfg.getValue().trim().split(",");
        map.put(pbShowId, new int[]{Integer.parseInt(arr[0]), Integer.parseInt(arr[1])});
    }

    public abstract int getGameType();

    public Map<Integer, BaseRoomCfg> getRoomCfgMap() {
        return roomCfgMap;
    }


    /**
     * 根据线id获取这条线上的icon坐标
     *
     * @param lineId
     * @return
     */
    public List<Integer> getIconIndexsByLineId(int lineId) {
        return getIconIndexsByLineId(lineId, false);
    }

    /**
     * 根据线id获取这条线上的icon坐标
     *
     * @param lineId
     * @return
     */
    public List<Integer> getIconIndexsByLineId(int lineId, boolean freeModel) {
        BaseLineCfg baseLineCfg = getBaseLineCfg(lineId, freeModel);
        if (baseLineCfg == null) {
            return null;
        }
        return baseLineCfg.getPosLocation();
    }

    protected BaseLineCfg getBaseLineCfg(int lineId, boolean freeModel) {
        Map<Integer, BaseLineCfg> lineCfgMap = this.baseLineCfgMap.get(0);
        if (lineCfgMap == null || lineCfgMap.isEmpty()) {
            if (freeModel) {
                lineCfgMap = this.baseLineCfgMap.get(2);
            } else {
                lineCfgMap = this.baseLineCfgMap.get(1);
            }
        }

        BaseLineCfg baseLineCfg = lineCfgMap.get(lineId);
        if (baseLineCfg == null) {
            return null;
        }
        return baseLineCfg;
    }

    @Override
    public void changeSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(BaseRoomCfg.EXCEL_NAME, () -> {
            baseRoomConfig();
            calAllLineStake();
        }).addChangeSampleFileObserveWithCallBack(BaseLineCfg.EXCEL_NAME, () -> baseLineConfig())
                .addChangeSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, () -> globalConfig())
                .addChangeSampleFileObserveWithCallBack(SpecialPlayCfg.EXCEL_NAME, () -> specialPlayConfig())
                .addChangeSampleFileObserveWithCallBack(SpecialAuxiliaryCfg.EXCEL_NAME, () -> specialAuxiliaryConfig());

    }

    /**
     * 处理玩家退出游戏事件
     *
     * @param playerController
     * @param exitType
     * @return
     */
    public T exit(PlayerController playerController, ExitType exitType) {
        T playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            return null;
        }

        if (exitType != ExitType.DROPPED) {
            exitPlayerGameData(playerController.playerId(), playerGameData);
        } else {
            long now = System.currentTimeMillis();
            playerGameData.setOfflineTime(now);
            playerGameData.setOnline(false);
            //修改执行离线任务的时间
            if (playerGameData.getOfflineEventMap() != null && !playerGameData.getOfflineEventMap().isEmpty()) {
                for (Map.Entry<Integer, OffLineEventData> en : playerGameData.getOfflineEventMap().entrySet()) {
                    OffLineEventData data = en.getValue();
                    data.setActionMills(now + data.getActionMills() + data.getDelayMills());
                }
            }
            //playerAllSlotsData 只要退出就要落库
            playerAllSlotsDataDao.saveToRedis(playerGameData.getPlayerAllSlotsData());
        }

        taskManager.onExit(playerController.playerId());
        return playerGameData;
    }

    protected int getBigShowIdByTimes(int times) {
        if (times < 1) {
            return 0;
        }
        Map.Entry<Integer, int[]> e = this.bigWinShowMap.entrySet().stream().filter(en -> times >= en.getValue()[0] && times < en.getValue()[1]).findFirst().orElse(null);
        return e == null ? 0 : e.getKey();
    }

    /**
     * 将单线押分转化为总押分
     */
    public void calAllLineStake() {
        Map<Integer, List<long[]>> tmpAllStakeMap = new HashMap<>();

        for (Map.Entry<Integer, BaseRoomCfg> en : this.roomCfgMap.entrySet()) {
            BaseRoomCfg cfg = en.getValue();
            for (long stake : cfg.getLineBetScore()) {
                long allStake = oneLineToAllStake(stake);
                long[] arr = {stake, allStake};
                tmpAllStakeMap.computeIfAbsent(cfg.getId(), k -> new ArrayList<>()).add(arr);
            }
        }

        this.allStakeMap = tmpAllStakeMap;
    }

    /**
     * 单线押分转化为总押分
     *
     * @param stake
     * @return
     */
    public long oneLineToAllStake(long stake) {
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        int lineCount = baseInitCfg.getMaxLine();

        return lineCount * stake * baseInitCfg.getBetMultiple().getFirst() * baseInitCfg.getLineMultiple().getFirst();
    }

    /**
     * 总押分转化为单线押分
     *
     * @param stake
     * @return
     */
    public long allStakeToOneLine(long stake) {
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        int lineCount = baseInitCfg.getMaxLine();

        return stake / lineCount / baseInitCfg.getBetMultiple().getFirst() / baseInitCfg.getLineMultiple().getFirst();
    }

    /**
     * 检查中奖金额是否要发送跑马灯
     *
     * @param data
     * @param win
     */
    protected void checkMarquee(T data, long win) {
        BaseRoomCfg baseRoomCfg = this.roomCfgMap.get(data.getRoomCfgId());
        if (baseRoomCfg == null || win < baseRoomCfg.getMarqueeTrigger().get(0)) {
            return;
        }

        marqueeManager.playerWinMarquee(data.getPlayerController().getPlayer().getNickName(), baseRoomCfg.getMarqueeTrigger().get(1).intValue(), baseRoomCfg.getNameid(), win, false);
    }

    /**
     * 通知其他节点，结果库变更
     */
    protected void noticeNodeLibChange(int changeType, List<SpecialResultLibCfg> cfgList) {
        try {
            List<ClusterClient> nodes = ClusterSystem.system.getNodesByTypeExcludeSelf(NodeType.GAME, CoreConst.GameMajorType.SLOTS);
            if (nodes.isEmpty()) {
                return;
            }

            List<String> tmpList = new ArrayList<>();
            cfgList.forEach(cfg -> {
                tmpList.add(JSON.toJSONString(cfg));
            });

            NoticeSlotsLibChange notice = new NoticeSlotsLibChange();
            notice.gameType = this.gameType;
            notice.changeType = changeType;
            notice.libCfgList = tmpList;
            PFMessage pfMessage = MessageUtil.getPFMessage(notice);
            ClusterMessage msg = new ClusterMessage(pfMessage);

            for (ClusterClient node : nodes) {
                node.write(msg);
            }

            log.info("通知其他节点，结果库变更 changeType : {}", changeType);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public void notifySpecialResultLibCacheData(List<SpecialResultLibCfg> cfgList) {
        if (cfgList != null && !cfgList.isEmpty()) {
            SpecialResultLibCacheData data = getGenerateManager().calSpecialResultLibCacheData(cfgList);
            getGenerateManager().setSpecialResultLibCacheData(data);
        }
        getResultLibDao().reloadLib();
    }

    /**
     * 添加测试的libtype
     *
     * @param playerController
     * @param libType
     * @return
     */
    public TestLibData addTestIconDataLibType(PlayerController playerController, int libType) {
        T playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            return null;
        }

        try {
            Set<Integer> set = libTypeSet(playerController.getPlayer().getGameType());
            if (!set.contains(libType)) {
                log.debug("libType不合法 playerId = {},libType = {}", playerController.playerId(), libType);
                return null;
            }

            TestLibData testLibData = new TestLibData();
            testLibData.setLibType(libType);
            playerGameData.addTestIconsData(testLibData);
            log.info("添加测试libType成功 playerId = {},libType = {}", playerController.playerId(), testLibData.getLibType());
            return testLibData;
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    /**
     * 添加测试icons
     *
     * @param playerController
     */
    public boolean addTestIconDataIcons(PlayerController playerController, String icons) {
        T playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            return false;
        }

        try {
            String[] splitArr = icons.split(";");
            String[] arr2 = splitArr[0].split(",");
            int[] initArr;
            if (splitArr.length > 1 && splitArr[1].contains("*")) {
                String[] rowAndCol = splitArr[1].split("\\*");
                initArr = new int[Integer.parseInt(rowAndCol[0]) * Integer.parseInt(rowAndCol[1]) + 1];
            } else {
                BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
                initArr = new int[baseInitCfg.getRows() * baseInitCfg.getCols() + 1];
            }

            for (int i = 1; i < initArr.length; i++) {
                initArr[i] = Integer.parseInt(arr2[i - 1]);
            }

            Constructor<L> constructor = this.libClass.getConstructor();
            L lib = constructor.newInstance();
            if (CollectionUtil.isEmpty(playerGameData.getTestLibDataList())) {
                lib.addLibType(1);
            } else {
                lib.addLibType(playerGameData.pollTestLibData().getLibType());
            }
            lib.setId(RandomUtils.getUUid());


            TestLibData testLibData = new TestLibData();
            SlotsResultLib resultLib = getGenerateManager().checkAward(initArr, lib);
            testLibData.setData(resultLib);
            playerGameData.addTestIconsData(testLibData);
            log.info("添加测试icons成功 playerId = {},icons = {}", playerController.playerId(), icons);
            return true;
        } catch (Exception e) {
            log.error("", e);
        }
        return false;
    }

    public static boolean isInteger(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        return str.matches("-?\\d+");
    }

    /**
     * 添加测试libs
     *
     * @param playerController
     */
    public boolean addTestLibs(PlayerController playerController, String gm) {
        T playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            return false;
        }

        try {
            String[] arr = gm.split(";");
            for (String s : arr) {
                byte[] data = strToByteArray(s);
                L lib = (L) getResultLibDao().deserializeResultLib(data, this.libClass);
                TestLibData testLibData = new TestLibData();
                testLibData.setData(lib);
                playerGameData.addTestIconsData(testLibData);
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return true;
    }

    private byte[] strToByteArray(String str) {
        String[] split = str.split(",");
        byte[] data = new byte[split.length];

        for (int i = 0; i < split.length; i++) {
            data[i] = Byte.parseByte(split[i]);
        }
        return data;
    }

    /**
     * 根据倍场id获取奖池
     *
     * @param roomCfgId
     * @return
     */
    public long getPoolValueByRoomCfgId(int roomCfgId) {
        if (this.poolValueMap == null) {
            return 0;
        }
        Long v = this.poolValueMap.get(roomCfgId);
        if (v == null) {
            return 0;
        }
        return v;
    }

    protected Set<Integer> libTypeSet(int gameType) {
        Set<Integer> set = new HashSet<>();
        for (Map.Entry<Integer, SpecialModeCfg> en : GameDataManager.getSpecialModeCfgMap().entrySet()) {
            SpecialModeCfg cfg = en.getValue();
            if (cfg.getGameType() == gameType) {
                set.add(en.getValue().getType());
            }
        }
        return set;
    }

    /**
     * 触发实际赢钱的task
     *
     * @param player 玩家数据
     */
    protected void triggerWinTask(Player player, G gameRunInfo, T playerGameData, int moneyItemId) {
        long winValue = gameRunInfo.getAllWinGold() - playerGameData.getAllBetScore();
        if (moneyItemId == ItemUtils.getGoldItemId()) {
            wealthRouletteController.addProgress(player, gameType, winValue);
        }
        int gameType = getGameType();
        //修改连续未中奖次数
        playerGameData.getPlayerAllSlotsData().updatePrizelessCount(gameRunInfo.getAllWinGold());
        //触发任务
        taskManager.trigger(player.getId(), TaskConstant.ConditionType.PLAY_GAME_WIN_MONEY, () -> {
            TaskConditionParam10003 param = new TaskConditionParam10003();
            param.setGameId(gameType);
            param.setBetAmount(playerGameData.getAllBetScore());
            param.setAddValue(winValue);
            param.setCoinId(moneyItemId);
            return param;
        }, false);
    }

    /**
     * 计算实际赢钱倍数
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    protected int calWinTimes(GameRunInfo<T> gameRunInfo, T playerGameData) {
        if (playerGameData.getAllBetScore() < 1) {
            return 0;
        }
        int times = (int) (gameRunInfo.getAllWinGold() / playerGameData.getAllBetScore());
        gameRunInfo.setAllWinTimes(times);
        return times;
    }

    public boolean canExit(SlotsPlayerGameData playerGameData) {
        return true;
    }


    public CommonResult<Long> checkAndGetPredictCostGoldNum(SlotsRoomController slotsRoomController) {
        CommonResult<Long> result = new CommonResult<>(Code.SUCCESS);
        BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(slotsRoomController.getRoom().getRoomCfgId());
        if (baseRoomCfg == null) {
            log.warn("检查房间保证金时，获取BaseRoomCfg为空 roomCfgId = {}", slotsRoomController.getRoom().getRoomCfgId());
            result.code = Code.NOT_FOUND;
            return result;
        }

        //从redis获取房间奖池是否足够
        Number value = roomSlotsPoolDao.getBigPoolByRoomId(slotsRoomController.getRoom().getId());
        if (value == null) {
            log.warn("获取房间池子失败 roomId = {}", slotsRoomController.getRoom().getId());
            result.code = Code.NOT_FOUND;
            return result;
        }

        int cfgMinValue = baseRoomCfg.getMinBankerAmount().get(1);
        long poolValue = value.longValue();
        if (poolValue < cfgMinValue) {
            //当房间中保证金不足时，开始自动续费
            boolean renewal = slotsRoomManager.autoRenewal(slotsRoomController);
            if (renewal) {
                //自动续费后再次判断金额是否足够
                value = roomSlotsPoolDao.getBigPoolByRoomId(slotsRoomController.getRoom().getId());
                poolValue = value.longValue();
                if (poolValue < cfgMinValue) {
                    result.code = Code.AMOUNT_OF_RESERVES_IS_NOT_ENOUGHT;
                    log.warn("房间保证金不足，无法开始游戏1 roomId = {},roomCfgId = {},poolValue = {},cfgValue = {}", slotsRoomController.getRoom().getId(), slotsRoomController.getRoom().getRoomCfgId(), poolValue, baseRoomCfg.getMinBankerAmount().get(1));
                    return result;
                }
            } else {
                result.code = Code.AMOUNT_OF_RESERVES_IS_NOT_ENOUGHT;
                log.warn("房间保证金不足，无法开始游戏2 roomId = {},roomCfgId = {},poolValue = {},cfgValue = {}", slotsRoomController.getRoom().getId(), slotsRoomController.getRoom().getRoomCfgId(), poolValue, baseRoomCfg.getMinBankerAmount().get(1));
                return result;
            }
        }
        result.data = poolValue;
        return result;
    }

    public RoomType getRoomType() {
        return null;
    }

    protected boolean isRoomGame() {
        return getRoomType() != null;
    }

    public long getMoneyByItemId(WarehouseCfg warehouseCfg, Player player) {
        //从赛季进入的 slots: 展示赛季币余额, 而非金币/钻石 (allGold/beforeGold/afterGold 复用同一字段)
        T gameData = getPlayerGameData(player.getId());
        if (gameData != null && gameData.isSeason()) {
            return gameData.getSeasonCoinBalance();
        }
        if (warehouseCfg.getTransactionItemId() == ItemUtils.getDiamondItemId()) {
            return player.getDiamond();
        }
        if (warehouseCfg.getTransactionItemId() == ItemUtils.getShellItemId()) {
            return player.getShell();
        }
        return player.getGold();
    }

    /**
     * 初始化离线事件
     *
     * @return
     */
    protected Map<Integer, OffLineEventData> initOffLineEvent() {
        Map<Integer, OffLineEventData> map = new HashMap<>();
        //离线30秒后执行
        OffLineEventData offLineEventData = new OffLineEventData(1);
        offLineEventData.setDelayMills(30 * TimeHelper.ONE_SECOND_OF_MILLIS);
        map.put(offLineEventData.getId(), offLineEventData);
        return map;
    }

    /**
     * 从数据库获取结果库
     *
     * @param playerGameData
     * @param libType
     * @return
     */
    protected CommonResult<L> getLibFromDB(T playerGameData, int libType) {
        CommonResult<L> result = new CommonResult<>(Code.SUCCESS);
        //先去获取测试数据
        TestLibData testLibData = playerGameData.pollTestLibData();

        boolean gmLibType = false;
        L resultLib = null;
        if (testLibData != null) {
            libType = testLibData.getLibType();
            if (libType > 0) {
                gmLibType = true;
                log.debug("获取到测试数据 playerId = {},libType = {}", playerGameData.getPlayerId(), libType);
            } else if (testLibData.getData() != null) {
                resultLib = (L) testLibData.getData();
                log.debug("获取到测试数据 playerId = {},lib = {}", playerGameData.getPlayerId(), JSON.toJSONString(resultLib));
            }
        }

        if (resultLib == null) {
            //获取倍场配置
            BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(playerGameData.getRoomCfgId());
            if (baseRoomCfg == null) {
                log.warn("获取倍场配置失败 playerId = {},gameType = {},roomCfgId = {}", playerGameData.getPlayerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId());
                result.code = Code.NOT_FOUND;
                return result;
            }

            //获取结果库配置
            CommonResult<SpecialResultLibCfg> libCfgResult = getLibCfg(playerGameData, baseRoomCfg.getInitBasePool());
            if (!libCfgResult.success()) {
                if (libCfgResult.code == Code.AMOUNT_OF_RESERVES_IS_NOT_ENOUGHT) {
                    sendRoomAmountNotEnough(playerGameData.getPlayerId());
                }
                result.code = libCfgResult.code;
                return result;
            }

            //如果gm中没有设置 libType，则需要根据配置获取 libType
            if (!gmLibType && libType <= 1) {
                //获取 specialResultLib 中的type
                CommonResult<Integer> resultLibTypeResult = getResultLibType(playerGameData, libCfgResult.data.getModelId(), playerGameData.getRoomType());
                if (!resultLibTypeResult.success()) {
                    result.code = resultLibTypeResult.code;
                    return result;
                }
                libType = resultLibTypeResult.data;
                log.debug("获取到结果库类型 playerId = {},libType = {}", playerGameData.getPlayerId(), libType);
            }

            //如果获取结果库失败，会重试，所以用循环
            for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
                //根据倍数区间从结果库里面随机获取一条
                resultLib = getLib(libCfgResult.data, libType, playerGameData);
                if (resultLib != null) {
                    //检查该lib是否中奖jackpot
                    if (resultLib.getJackpotIds() != null && !resultLib.getJackpotIds().isEmpty()) {
                        List<Integer> rewardPoolIds = checkLibPool(resultLib, playerGameData);
                        if (rewardPoolIds.isEmpty()) {  //如果发现lib可以中奖，但是不够资格
                            resultLib = afterForbidPoolLib(libCfgResult.data, resultLib, playerGameData);
                        }
                    }
                }

                if (resultLib == null) {
                    log.warn("获取结果库失败 gameType = {},modelId = {},libType = {},retry = {}", this.gameType, libCfgResult.data.getModelId(), libType, i);
                    continue;
                }
//                sectionIndex = resultLibSectionResult.data;
                log.info("成功获取结果库  playerId = {}", playerGameData.getPlayerId());
                result.code = Code.SUCCESS;
                playerGameData.setLastModelId(libCfgResult.data.getModelId());
                break;
            }
        }

        //如果前面没有获取到lib，则获取一个无奖励的结果
        if (resultLib == null) {
            resultLib = (L) getResultLibDao().getNoWinLib(this.libClass);
            if (resultLib == null) {
                log.warn("前面没有获取到lib失败，获取不中奖的结果也失败 playerId = {}", playerGameData.getPlayerId());
                result.code = Code.FAIL;
                return result;
            }
        }
        result.data = resultLib;
        return result;
    }

    /**
     * 检查该resultLib是否中jackpot
     * 如果是则判断是否有资格中奖
     * 如果否则重新获取
     *
     * @param resultLib
     * @param playerGameData
     * @return
     */
    protected List<Integer> checkLibPool(L resultLib, T playerGameData) {
        //获取玩家累计贡献金额
        long contribt = playerGameData.getAllContribtPoolGold();
        if (contribt < 1) {
            return Collections.emptyList();
        }

        //检查奖池是否已经冷却
        List<Integer> canRewardPoolIds = new ArrayList<>();
        for (Object obj : resultLib.getJackpotIds()) {
            int jackpotId = (int) obj;
            if (!slotsPoolDao.checkPoolCD(jackpotId)) {
                return Collections.emptyList();
            }
            canRewardPoolIds.add(jackpotId);
        }

        if (canRewardPoolIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("玩家累计贡献金额 playerId = {},contribtGold = {},poolId = {}", playerGameData.getPlayerId(), contribt, resultLib.getJackpotIds());

        //真奖池
        Number smallPoolNumber = slotsPoolDao.getSmallPoolByRoomCfgId(playerGameData.getGameType(), playerGameData.getRoomCfgId());
        if (smallPoolNumber == null) {
            log.debug("获取小池子金额为空 playerId = {},roomCfgId = {},poolId = {}", playerGameData.getPlayerId(), playerGameData.getRoomCfgId(), resultLib.getJackpotIds());
            return Collections.emptyList();
        }
        //假奖池
        Number fakeSmallPoolNumber = slotsPoolDao.getFakeSmallPoolByRoomCfgId(playerGameData.getGameType(), playerGameData.getRoomCfgId());
        if (fakeSmallPoolNumber == null) {
            log.debug("获取(假)小池子金额为空 playerId = {},roomCfgId = {},poolId = {}", playerGameData.getPlayerId(), playerGameData.getRoomCfgId(), resultLib.getJackpotIds());
            return Collections.emptyList();
        }

        //当真奖池 >= 假奖池时，才允许中奖
        long smallPool = smallPoolNumber.longValue();
        long fakeSmallPool = fakeSmallPoolNumber.longValue();

        if (smallPool < fakeSmallPool) {
            log.debug("真奖池小于假奖池，不允许中奖 playerId = {},roomCfgId = {},smallPool = {},fakeSmallPoolNumber = {}", playerGameData.getPlayerId(), playerGameData.getRoomCfgId(), smallPool, fakeSmallPool);
            return Collections.emptyList();
        }

        log.info("真奖池大于假奖池，允许中奖 playerId = {},roomCfgId = {},smallPool = {},fakeSmallPoolNumber = {}", playerGameData.getPlayerId(), playerGameData.getRoomCfgId(), smallPool, fakeSmallPool);
        BigDecimal pool = BigDecimal.valueOf(smallPoolNumber.longValue());

        List<Integer> jackpotIds = new ArrayList<>();
        for (int jackpotId : canRewardPoolIds) {
            PoolCfg poolCfg = GameDataManager.getPoolCfg(jackpotId);
            if (poolCfg == null) {
                log.debug("获取的池子配置为空 poolId = {}", jackpotId);
                continue;
            }

            //中奖概率,这里保留了8位
            // 按需求将原始概率放大 10000 倍，命中上限后再由奖池值和冷却控制实际放奖频率。
            int propV = BigDecimal.valueOf(contribt).divide(pool, 8, BigDecimal.ROUND_HALF_UP).divide(BigDecimal.valueOf(poolCfg.getPoolProp()), 8, BigDecimal.ROUND_HALF_UP).multiply(oneHundredMillionBigDecimal).intValue();
            int rand = RandomUtils.randomInt(GameConstant.TEN_THOUSAND);
            if (rand >= propV) {
                log.debug("随机概率，未中奖 rand = {},propV = {}", rand, propV);
            } else {
                jackpotIds.add(jackpotId);
            }
        }
        return jackpotIds;
    }

    /**
     * 如果系统判断玩家不能中奖池后的处理办法，获取一个普通的结果
     *
     * @param specialResultLibCfg
     * @param resultLib
     * @return
     */
    protected L afterForbidPoolLib(SpecialResultLibCfg specialResultLibCfg, L resultLib, T playerGameData) {
        return getLib(specialResultLibCfg, 1, playerGameData);
    }

    protected L getLib(SpecialResultLibCfg specialResultLibCfg, int libType, T playerGameData) {
        //获取倍数区间
        CommonResult<Integer> resultLibSectionResult = getResultLibSection(playerGameData, specialResultLibCfg.getModelId(), libType, playerGameData.getAllBetScore(),
                playerGameData.getPlayer().getSvip(), playerGameData.getPlayerAllSlotsData().getAllBetCount(), playerGameData.getPlayerAllSlotsData().getPrizelessCount());
        if (!resultLibSectionResult.success()) {
            log.warn("获取区间失败 modelId = {},libtype = {}", specialResultLibCfg.getModelId(), libType);
            return null;
        }

        //根据倍数区间从结果库里面随机获取一条
        return (L) getResultLibDao().getLibBySectionIndex(libType, resultLibSectionResult.data, this.libClass);
    }

    /**
     * 处理奖池为空时的逻辑(暂时只有房间slots才会调用)
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    protected G handleEmptyPrizePool(G gameRunInfo, T playerGameData) {
        //首先获取一个不中奖的结果
        L lib = (L) getResultLibDao().getNoWinLib(this.libClass);
        if (lib == null) {
            log.warn("获取不中奖的结果失败 playerId = {}", playerGameData.getPlayerId());
            gameRunInfo.setCode(Code.AMOUNT_OF_RESERVES_IS_NOT_ENOUGHT);
            return gameRunInfo;
        }
        gameRunInfo.setBigPoolTimes(0);
        gameRunInfo.setSmallPoolGold(0);
        gameRunInfo.setResultLib(null);
        return normal(gameRunInfo, playerGameData, playerGameData.getAllBetScore(), lib);
    }

    /**
     * 获取游戏状态
     *
     * @return
     */
    public NotifySlotsStatus gameStatus(PlayerController playerController) {
        NotifySlotsStatus res = new NotifySlotsStatus();
        if (getRoomType() != RoomType.SLOTS_TEAM_UP_ROOM) {
            log.warn("获取游戏状态错误 roomType = {}", getRoomType());
            return res;
        }
        //获取保证金(即水池)
        SlotsRoomController roomController = (SlotsRoomController) playerController.getScene();
        res.pauseType = roomStatus(roomController);
        return res;
    }

    public int roomStatus(SlotsRoomController slotsRoomController) {
        if (slotsRoomController == null || slotsRoomController.getRoom() == null) {
            return 0;
        }
        int status = 0;
        SlotsFriendRoom friendRoom = slotsRoomController.getRoom();
        if (friendRoom.getStatus() == 2) {
            status = 1;
        }
        if (friendRoom.getOverdueTime() < System.currentTimeMillis()) {
            status = 3;
        }
        CommonResult<Long> result = checkAndGetPredictCostGoldNum(slotsRoomController);
        if (result.code == Code.AMOUNT_OF_RESERVES_IS_NOT_ENOUGHT) {
            status = 2;
        }
        if (friendRoom.getStatus() == 3) {
            status = 4;
        }
        return status;
    }

    protected G createGameRunInfo(long playerId, int code) throws Exception {
        Constructor<G> constructor = this.gameRunInfoClass.getConstructor(int.class, long.class);
        return constructor.newInstance(code, playerId);
    }

    /**
     * 清除游戏状态
     *
     * @param playerId
     * @param roomCfgId
     */
    @SuppressWarnings("unchecked")
    public void cleanStatus(long playerId, int roomCfgId) {
        T playerGameData = getPlayerGameData(playerId);
        if (playerGameData != null) {
            playerGameData.setStatus(0);
            playerGameData.getRemainFreeCount().set(0);
            playerGameData.getFreeIndex().set(0);
            playerGameData.setFreeLib(null);
            playerGameData.setTestLibDataList(null);
        } else {
            SlotsPlayerGameData data = playerGameDataDao.getPlayerGameDataByPlayerId(playerId, roomCfgId, 0, playerGameDataClass);
            if (data != null) {
                data.setStatus(0);
                data.setFreeAllWin(0);
                playerGameDataDao.savePlayerGameData(data);
            }
        }
    }

    /**
     * gm修改奖池
     *
     * @param playerController
     * @param type             0.标准池  1.小奖池
     * @param value
     */
    public boolean gmChangePool(PlayerController playerController, int type, long value) {
        if (getRoomType() != null) {
            log.warn("gm修改奖池失败，暂不支持房间修改 playerId = {},type = {},value = {}", playerController.playerId(), type, value);
            return false;
        }

        if (value < 0) {
            log.warn("gm修改奖池失败 playerId = {},type = {},value = {}", playerController.playerId(), type, value);
            return false;
        }
        T playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.warn("gm修改奖池失败 playerId = {}", playerController.playerId());
            return false;
        }

        if (type == 0) {
            slotsPoolDao.setBigPool(playerGameData.getGameType(), playerGameData.getRoomCfgId(), value);
            log.debug("玩家gm修改标准池 playerId = {},roomCfgId = {},value = {}", playerController.playerId(), playerGameData.getRoomCfgId(), value);
        } else if (type == 1) {
            slotsPoolDao.setSmallPool(playerGameData.getGameType(), playerGameData.getRoomCfgId(), value);
            log.debug("玩家gm修改小奖池 playerId = {},roomCfgId = {},value = {}", playerController.playerId(), playerGameData.getRoomCfgId(), value);
        } else if (type == 2) {
            slotsPoolDao.setFakeSmallPool(playerGameData.getGameType(), playerGameData.getRoomCfgId(), value);
            log.debug("玩家gm修改假奖池 playerId = {},roomCfgId = {},value = {}", playerController.playerId(), playerGameData.getRoomCfgId(), value);
        } else {
            log.warn("gm修改奖池失败,不支持的类型 playerId = {},type = {}", playerController.playerId(), type);
            return false;
        }
        return true;
    }

    /**
     * gm修改玩家贡献金额
     *
     * @param playerController
     * @param value
     */
    public boolean gmChangeContribtGold(PlayerController playerController, long value) {
        if (getRoomType() != null) {
            log.warn("gm修改贡献值失败，暂不支持房间修改 playerId = {},value = {}", playerController.playerId(), value);
            return false;
        }
        if (value < 0) {
            log.warn("gm修改贡献值失败 playerId = {},value = {}", playerController.playerId(), value);
            return false;
        }
        T playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.warn("gm修改贡献值失败 playerId = {}", playerController.playerId());
            return false;
        }
        playerGameData.setRewardPoolGold(0);
        playerGameData.setContribtPoolGold(value);
        log.debug("玩家修改贡献金额 playerId = {},value = {}", playerController.playerId(), value);
        return true;
    }

    public boolean gmChangeAllData(PlayerController playerController, int allBetCount, int prizelessCount) {
        T playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.warn("gm修改allData失败 playerId = {}", playerController.playerId());
            return false;
        }

        if (allBetCount >= 0) {
            playerGameData.getPlayerAllSlotsData().setAllBetCount(allBetCount);
        }

        if (prizelessCount >= 0) {
            playerGameData.getPlayerAllSlotsData().setPrizelessCount(prizelessCount);
        }
        return true;
    }

    public AtomicBoolean getOpen() {
        return open;
    }

    /**
     * 获取玩家的筹码列表
     *
     * @param playerGameData
     * @return
     */
    public List<Long> stakeList(T playerGameData) {
        //首先获取配置的筹码列表
        List<long[]> cfgStakeList = this.allStakeMap.get(playerGameData.getRoomCfgId());

        List<Long> list = new ArrayList<>();
        for (long[] arr : cfgStakeList) {
            list.add(arr[1]);
        }

        playerGameData.setTmpUnlockedStakeSet(null);
        if (playerGameData.isSeason()) {
            //赛季入口只追加 SeasonGem.bet，不能混入 ResearchSkills.bet。
            List<Long> seasonBet = playerGameData.getSeasonSlotsSessionData().getBet();
            if (seasonBet != null && !seasonBet.isEmpty()) {
                list.addAll(seasonBet);
                playerGameData.setTmpUnlockedStakeSet(new HashSet<>(seasonBet));
            }
        } else if (playerGameData.getSkillsMap() != null && !playerGameData.getSkillsMap().isEmpty()) {
            //其他入口保持原有研发技能下注解锁逻辑。
            for (Map.Entry<Integer, Integer> en : playerGameData.getSkillsMap().entrySet()) {
                ResearchSkillsCfg cfg = this.slotsSkillService.getResearchSkillsCfg(this.gameType, en.getKey(), en.getValue());
                if (cfg == null) {
                    continue;
                }

                if (cfg.getBet() == null || cfg.getBet().isEmpty()) {
                    continue;
                }
                list.addAll(cfg.getBet());
                playerGameData.setTmpUnlockedStakeSet(new HashSet<>(cfg.getBet()));
                break;
            }
        }
        return list;
    }

    /**
     * 获取押分值
     *
     * @param playerGameData
     * @param betValue
     * @return
     */
    public long[] getBetScore(T playerGameData, long betValue) {
        long[] betScoreArr = this.allStakeMap.get(playerGameData.getRoomCfgId()).stream().filter(arr -> arr[1] == betValue).findFirst().orElse(null);
        if (betScoreArr != null) {
            return betScoreArr;
        }

        if (playerGameData.getTmpUnlockedStakeSet() == null || !playerGameData.getTmpUnlockedStakeSet().contains(betValue)) {
            return betScoreArr;
        }

        betScoreArr = new long[2];
        betScoreArr[0] = allStakeToOneLine(betValue);
        betScoreArr[1] = betValue;
        return betScoreArr;
    }

    /**
     * 返回本局正在生效的技能快照：普通入口是自己的，客座赌局是进场时加载的房主技能。
     */
    public Map<Integer, Integer> getSkills(PlayerController playerController) {
        T playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.info("获取技能失败，playerId={},gameType = {}", playerController.playerId(), this.gameType);
            return null;
        }
        return playerGameData.getSkillsMap();
    }
}
