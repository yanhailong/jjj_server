package com.jjg.game.slots.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.activity.common.data.ActivityTargetType;
import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.dao.PlayerHistorySlotsDao;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.data.*;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.data.TestLibData;
import com.jjg.game.slots.logger.SlotsLogger;
import com.jjg.game.slots.pb.NoticeSlotsLibChange;
import com.jjg.game.slots.service.SlotsPlayerService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * slots游戏管理器抽象类
 *
 * @author 11
 * @since 2025/7/1 16:42
 */
public abstract class AbstractSlotsGameManager<T extends SlotsPlayerGameData, L extends SlotsResultLib> implements TimerListener, ConfigExcelChangeListener {
    protected Logger log = LoggerFactory.getLogger(getClass());

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
    //游戏类型
    protected int gameType;
    //在specualResultLib
    protected int defaultRewardSectionIndex = -1;

    //roomCfgId -> playerId ->gameData
    protected Map<Integer, Map<Long, T>> gameDataMap = new ConcurrentHashMap<>();


    protected BigDecimal tenThousandBigDecimal = BigDecimal.valueOf(10000);
    protected BigDecimal oneHundredMillionBigDecimal = BigDecimal.valueOf(100000000);
    protected int oneHundredMillion = 100000000;

    protected Class<T> playerGameDataClass;
    protected Class<L> libClass;

    //roomName -> cfg
    protected Map<Integer, BaseRoomCfg> roomCfgMap;
    //lineId -> cfg
    protected Map<Integer, BaseLineCfg> lineCfgMap;


    //大奖展示倍数区间
    protected Map<Integer, int[]> bigWinShowMap = null;


    public AbstractSlotsGameManager(Class<T> playerGameDataClass, Class<L> libClass) {
        this.playerGameDataClass = playerGameDataClass;
        this.libClass = libClass;
    }

    //检查离线玩家事件
    private TimerEvent<String> checkOffLineEvent;
    //总押分 roomCfgId -> [0] = 单线押分  [1] = 总押分
    protected Map<Integer, List<long[]>> allStakeMap;


    //单个类型批量生成条数
    protected int batchGenCount = 200000;
    //批量保存到mongo的条数
    protected int batchSaveCount = 100;

    //在更新结果库后，要开启清除旧结果库的定时事件
    protected TimerEvent<String> clearAllLibEvent;
    //生成结果库事件
    protected TimerEvent<Map<Integer, Integer>> generateLibEvent;
    //在更新结果库后，要开启清除旧结果库的定时事件
    protected TimerEvent<String> clearRedisLibEvent;

    /**
     * 初始化
     */
    public void init() {
        this.gameType = getGameType();

        getResultLibDao().init(this.gameType);
        getGenerateManager().init(this.gameType);
        initConfig();

        addCheckOffLineEvent();
    }

    /**
     * 添加生成结果库事件
     */
    public boolean addGenerateLibEvent(Map<Integer, Integer> libTypeCountMap) {
        if (this.generateLibEvent != null) {
            log.debug("当前有未执行的生成结果库任务，所以添加失败");
            return false;
        }

        if (libTypeCountMap == null || libTypeCountMap.isEmpty()) {
            log.debug("libTypeCountMap 为空，生成失败");
            return false;
        }

        boolean lock = getResultLibDao().getGenerateLock(this.gameType);
        if (lock) {
            log.debug("当前正在执行生成结果库任务，请勿打扰.... ");
            return false;
        }
        this.generateLibEvent = new TimerEvent<>(this, 10, libTypeCountMap).withTimeUnit(TimeUnit.SECONDS);
        this.timerCenter.add(this.generateLibEvent);
        return true;
    }

    /**
     * 生成结果集
     *
     * @param libTypeCountMap 生成条数
     */
    protected void generate(Map<Integer, Integer> libTypeCountMap) {
        generate(libTypeCountMap, true);
    }

    /**
     * 生成结果集
     *
     * @param libTypeCountMap 生成条数
     */
    public void generate(Map<Integer, Integer> libTypeCountMap, boolean saveToDB) {
        String newDocName = null;
        String redisTableName = null;
        try {
            if (!saveToDB) {
                boolean lock = getResultLibDao().addGenerateLock(this.gameType);
                if (lock) {
                    log.info("生成结果库时添加锁失败，gameType = {}", this.gameType);
                    return;
                }
            }

            log.info("开始生成结果库，libTypeCountMap = {}", libTypeCountMap);

            //计算出每个区间需要的条数
            Map<Integer, Map<Integer, Integer>> exceptGenCountMap = getGenerateManager().splitLibBySection(libTypeCountMap);
            //拷贝一份
            Map<Integer, Map<Integer, Integer>> tmpExceptGenCountMap = exceptGenCountMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> new HashMap<>(e.getValue())
                    ));

            //记录当前生成的条数
            Map<Integer, Map<Integer, Integer>> currentGenCountMap = new HashMap<>();

            //移除条数为0的
            getGenerateManager().removeCount0(tmpExceptGenCountMap);

            Map<Integer, Map<Integer, int[]>> sectionMap = getGenerateManager().getSpecialResultLibCacheData().getResultLibSectionMap();

            newDocName = getResultLibDao().getNewMongoLibName();
            //实际循环次数
            int currentForCount = 0;
            //累计保存到数据库的条数
            int saveCount = 0;

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
                            log.warn("未找到对应的倍数区间 libType = {}, times = {}", tmpLibType, times);
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
                            continue;
                        }

                        if (currentCount < exceptCount) {
                            temMap.merge(index, 1, Integer::sum);
                            libList.add(lib);
                        } else {
                            exceptGenSectionCountMap.remove(index);
                        }
                    }

                    if (libList.size() >= this.batchSaveCount) {
//                        System.out.println("保存这里的111");
                        if (saveToDB) {
                            saveCount += getResultLibDao().batchSave(libList, newDocName);
                        } else {
                            saveCount += libList.size();
                        }

                        libList = new ArrayList<>();
                    }
                }
            }

            if (saveToDB && !libList.isEmpty()) {
//                System.out.println("保存这里的222 size = " + libList.size());
                saveCount += getResultLibDao().batchSave(libList, newDocName);
            }

            if (saveToDB) {
                log.debug("生成结束，开始转移到redis, newDocName = {}", newDocName);
                //加载到redis
                redisTableName = getResultLibDao().moveToRedis(newDocName, getGenerateManager().getSpecialResultLibCacheData().getResultLibSectionMap());

                log.info("生成结果库结束，实际循环次数 = {},成功保存到数据库 {} 条,mongoName = {},redisName = {}", currentForCount, saveCount, newDocName, redisTableName);

                this.clearAllLibEvent = new TimerEvent<>(this, 1, "clearLibEvent").withTimeUnit(TimeUnit.MINUTES);
                this.timerCenter.add(this.clearAllLibEvent);

                //通知其他节点，结果库变更
                noticeNodeLibChange(SlotsConst.LibChangeType.LIB_CHANGE, Collections.EMPTY_LIST);
            } else {
                log.debug("生成结束，实际循环次数 = {},总计条数 = {}", currentForCount, saveCount);
            }
        } catch (Exception e) {
            if (StringUtils.isNotEmpty(newDocName)) {
                getResultLibDao().clearMongoLib(newDocName);
            }
            if (StringUtils.isNotEmpty(redisTableName)) {
                getResultLibDao().clearRedisLib(redisTableName);
            }
            getResultLibDao().removeGenerateLock(this.gameType);
            log.error("", e);
        }
    }

    public <G extends GameRunInfo> G enterGame(PlayerController playerController) {
        return null;
    }

    /**
     * 普通流程获取结果库
     *
     * @param playerGameData
     * @param betValue
     * @return
     */
    protected CommonResult<L> normalGetLib(T playerGameData, long betValue) {
        CommonResult<L> result = new CommonResult<>(Code.SUCCESS);
        log.debug("开始正常流程 playerId = {},betValue = {}", playerGameData.playerId(), betValue);
        //获取倍场配置
        BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(playerGameData.getRoomCfgId());
        if (baseRoomCfg == null) {
            log.warn("获取倍场配置失败 playerId = {},gameType = {},roomCfgId = {},betValue = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), betValue);
            result.code = Code.NOT_FOUND;
            return result;
        }

        //检查押分是否合法
        long[] betScoreArr = this.allStakeMap.get(playerGameData.getRoomCfgId()).stream().filter(arr -> arr[1] == betValue).findFirst().orElse(null);
        if (betScoreArr == null) {
            log.warn("押分值不合法 playerId = {},gameType = {},roomCfgId = {},betValue = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), betValue);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        Player player = slotsPlayerService.get(playerGameData.playerId());
        if (player.getGold() < betValue) {
            log.debug("玩家余额不足，无法快乐的玩游戏 playerId = {},gameType = {},roomCfgId = {},betValue = {},currentGold = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), betValue, player.getGold());
            result.code = Code.NOT_ENOUGH;
            return result;
        }

        CommonResult<SpecialResultLibCfg> libCfgResult = getLibCfg(playerGameData, baseRoomCfg.getInitBasePool());
        if (!libCfgResult.success()) {
            result.code = libCfgResult.code;
            return result;
        }

        int libType = 0;
        //先去获取测试数据
        TestLibData testLibData = playerGameData.pollTestLibData();
        if (testLibData != null) {
            libType = testLibData.getLibType();
            log.debug("获取到测试数据 playerId = {},libType = {}", playerGameData.playerId(), libType);
        }

        if (libType < 1) {
            //获取 specialResultLib 中的type
            CommonResult<Integer> resultLibTypeResult = getResultLibType(playerGameData.getGameType(), libCfgResult.data.getModelId());
            if (!resultLibTypeResult.success()) {
                result.code = libCfgResult.code;
                return result;
            }
            libType = resultLibTypeResult.data;
            log.debug("获取到结果库类型 playerId = {},libType = {}", playerGameData.playerId(), libType);
        }

        int sectionIndex = -1;
        L resultLib = null;

        for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
            //获取倍数区间
            CommonResult<Integer> resultLibSectionResult = getResultLibSection(libCfgResult.data.getModelId(), libType);
            if (!resultLibSectionResult.success()) {
                continue;
            }

            //根据倍数区间从结果库里面随机获取一条
            resultLib = (L) getResultLibDao().getLibBySectionIndex(libType, resultLibSectionResult.data);
            if (resultLib == null) {
                log.debug("获取结果库失败 gameType = {},modelId = {},libType = {},sectionIndex = {},retry = {}", this.gameType, libCfgResult.data.getModelId(), libType, resultLibSectionResult.data, i);
                continue;
            }
            sectionIndex = resultLibSectionResult.data;
            log.debug("成功获取结果库  playerId = {},libId = {}", playerGameData.playerId(), resultLib.getId());
            break;
        }

        //如果前面没有获取到lib，则获取一个无奖励的结果
        if (resultLib == null) {
            sectionIndex = this.defaultRewardSectionIndex;
            resultLib = (L) getResultLibDao().getLibBySectionIndex(DollarExpressConstant.SpecialMode.TYPE_NORMAL, this.defaultRewardSectionIndex);
            log.debug("前面获取结果库失败，所以找一个不中奖的结果返回 gameType = {},libType = {}", this.gameType, libType);
        }

        if (resultLib == null) {
            log.debug("获取结果库失败 gameType = {},libType = {}", this.gameType, libType);
            result.code = Code.FAIL;
            return result;
        }

        //给池子加钱
        CommonResult<Player> poolResult = goldToPool(playerGameData, betValue, baseRoomCfg);
        if (!result.success()) {
            result.code = poolResult.code;
            return result;
        }

        //记录押分值
        playerGameData.setOneBetScore(betScoreArr[0]);
        playerGameData.setAllBetScore(betScoreArr[1]);

        if (sectionIndex > 0) {
            playerGameData.setLastSectionIndex(sectionIndex);
        }
        playerGameData.setLastModelId(libCfgResult.data.getModelId());

        result.data = resultLib;
        return result;
    }

    /**
     * 免费模式获取结果库
     *
     * @param playerGameData
     * @return
     */
    protected CommonResult<L> freeGetLib(T playerGameData) {
        CommonResult<L> result = new CommonResult<>(Code.SUCCESS);
        log.debug("开始获取免费结果库 playerId = {}", playerGameData.playerId());

        L freeLib = (L) playerGameData.getFreeLib();
        if (freeLib == null) {
            for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
                //获取一个倍数区间
                CommonResult<Integer> sectionResult = getResultLibSection(playerGameData.getLastModelId(), DollarExpressConstant.SpecialMode.TYPE_TRIGGER_FREE);
                if (!sectionResult.success()) {
                    continue;
                }
                //获取结果库
                freeLib = (L) getResultLibDao().getLibBySectionIndex(DollarExpressConstant.SpecialMode.TYPE_TRIGGER_FREE, sectionResult.data);
                if (freeLib == null) {
                    continue;
                }
                break;
            }
        }

        if (freeLib == null) {
            log.debug("未在该条结果库中找到免费转信息 gameType = {},modelId = {}", this.gameType, playerGameData.getLastModelId());
            result.code = Code.NOT_FOUND;
            return result;
        }

        if (freeLib.getSpecialAuxiliaryInfoList() == null || freeLib.getSpecialAuxiliaryInfoList().isEmpty()) {
            log.debug("未在该条结果库中找到免费转信息1 gameType = {},libId = {}", this.gameType, freeLib.getId());
            result.code = Code.NOT_FOUND;
            return result;
        }

        log.debug("找到免费旋转的结果库 libId = {}", freeLib.getId());

        //找到结果库中免费游戏的结果
        SpecialAuxiliaryInfo specialAuxiliaryInfo = null;
        for (Object obj : freeLib.getSpecialAuxiliaryInfoList()) {
            SpecialAuxiliaryInfo tmpInfo = (SpecialAuxiliaryInfo) obj;
            SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(tmpInfo.getCfgId());
            if (specialAuxiliaryCfg.getType() != DollarExpressConstant.SpecialAuxiliary.TYPE_ALL_BOARD_FREE) {
                continue;
            }
            if (tmpInfo.getFreeGames() == null || tmpInfo.getFreeGames().isEmpty()) {
                continue;
            }
            specialAuxiliaryInfo = tmpInfo;
            break;
        }

        if (specialAuxiliaryInfo == null) {
            log.debug("未在该条结果库中找到免费转信息2 gameType = {},libId = {}", this.gameType, freeLib.getId());
            result.code = Code.NOT_FOUND;
            return result;
        }

        int size = specialAuxiliaryInfo.getFreeGames().size();

        JSONObject jsonObject = specialAuxiliaryInfo.getFreeGames().get(size - playerGameData.getRemainFreeCount().get());
        L freeGame = JSON.parseObject(jsonObject.toJSONString(), this.libClass);
//        DollarExpressResultLib freeGame = (DollarExpressResultLib) specialAuxiliaryInfo.getAwardInfos().get(size - playerGameData.getRemainFreeCount().get());

        if (freeGame == null) {
            log.debug("未在该条结果库中找到免费转信息3 gameType = {},libId = {}", this.gameType, freeLib.getId());
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
     * 给池子加钱
     */
    protected CommonResult<Player> goldToPool(T gameData, long betValue, BaseRoomCfg baseRoomCfg) {
        CommonResult<Player> result = slotsPlayerService.betDeductGold(gameData.playerId(), betValue, true, "SLOTS_BET");
        if (!result.success()) {
            log.debug("把钱添加到池子失败,扣除玩家金额失败 playerId = {},betValue = {},code = {}", gameData.playerId(), betValue, result.code);
            return result;
        }
        Thread.ofVirtual().start(() -> {
            activityManager.addActivityProgress(gameData.getPlayerController().getPlayer(),
                    ActivityTargetType.getTagetKey(ActivityTargetType.BET, ActivityTargetType.EFFECTIVE_BET), betValue);
            activityManager.addPlayerActivityProgress(gameData.getPlayerController().getPlayer(),
                    ActivityTargetType.getTagetKey(ActivityTargetType.BET, ActivityTargetType.EFFECTIVE_BET), betValue);
        });
        BigDecimal bet = BigDecimal.valueOf(betValue);
        log.debug("玩家扣除金币成功 playerId = {},reduceGold = {},afterGold = {}", gameData.playerId(), betValue, result.data.getGold());

        //给标准池子加钱
        BigDecimal toBigPoolProp = BigDecimal.valueOf(baseRoomCfg.getInitBasePoolProportion()).divide(tenThousandBigDecimal, 4, RoundingMode.HALF_UP);
        long toBigPoolGold = bet.multiply(toBigPoolProp).setScale(0, RoundingMode.HALF_UP).longValue();
        if (toBigPoolGold > 0) {
            long poolCoin = slotsPoolDao.addToBigPool(this.gameType, gameData.getRoomCfgId(), toBigPoolGold);
            log.debug("给标准池加钱成功 gameType = {},roomCfgId = {},add = {},afterGold = {}", gameData.getGameType(), gameData.getRoomCfgId(), toBigPoolGold, poolCoin);
        }

        //给小池子加钱
        BigDecimal toSmallPoolProp = BigDecimal.valueOf(baseRoomCfg.getCommissionProp()).divide(tenThousandBigDecimal, 4, RoundingMode.HALF_UP);
        long toSmallPoolGold = bet.multiply(toSmallPoolProp).setScale(0, RoundingMode.HALF_UP).longValue();
        if (toSmallPoolGold > 0) {
            long poolCoin = slotsPoolDao.addToSmallPool(this.gameType, gameData.getRoomCfgId(), toSmallPoolGold);
            gameData.addAllBet(poolCoin);
            long contribtGold = gameData.addContribtPoolGold(poolCoin);
            log.debug("给小池子加钱成功 gameType = {},roomCfgId = {},add = {},afterGold = {},contribtGold={}", gameData.getGameType(), gameData.getRoomCfgId(), toSmallPoolGold, poolCoin, contribtGold);
        }
        return result;
    }

    protected void addCheckOffLineEvent() {
        this.checkOffLineEvent = new TimerEvent<>(this, "offLineEvent", 1).withTimeUnit(TimeUnit.MINUTES);
        timerCenter.add(this.checkOffLineEvent);
    }

    /**
     * 关闭
     */
    public void shutdown() {
        this.gameDataMap.forEach((k, v) -> {
            v.forEach((k1, v1) -> {
                offlineSaveGameDataDto(v1);
            });
        });
    }

    public void clearPlayerEvent(long playerId) {

    }

    protected void initConfig() {
        baseRoomConfig();
        baseLineConfig();
        specialPlayConfig();

        globalConfig();
        calAllLineStake();
        log.info("配置重新计算结束 gameType = {}", this.gameType);
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
     */
    public CommonResult<SpecialResultLibCfg> getLibCfg(T gameData, long poolInit) {
        CommonResult<SpecialResultLibCfg> result = new CommonResult<>(Code.SUCCESS);
        boolean flag = gameData.getHasPlaySlots().compareAndSet(false, true);
        //表示第一次玩该slots游戏
        if (flag) {
            SpecialResultLibCfg libCfg = getLibCfgByModelId(gameData.getGameType(), SlotsConst.Common.FIRST_GAME_GET_MODEL_ID);
            if (libCfg == null) {
                log.warn("获取结果库配置失败 playerId = {},gameType = {},roomCfgId = {},modelId = {}", gameData.playerId(), gameData.getGameType(), gameData.getRoomCfgId(), SlotsConst.Common.FIRST_GAME_GET_MODEL_ID);
                result.code = Code.NOT_FOUND;
                return result;
            }
            historySlotsDao.addGameType(gameData.playerId(), gameData.getGameType());
            result.data = libCfg;
            log.debug("玩家第一次玩该slots游戏，选择默认模式，playerId = {},gameType = {},modelId = {}", gameData.playerId(), gameData.getGameType(), SlotsConst.Common.FIRST_GAME_GET_MODEL_ID);
        } else {
            //获取水池
            Number poolValue = slotsPoolDao.getBigPoolByRoomCfgId(gameData.getGameType(), gameData.getRoomCfgId());
            if (poolValue == null) {
                log.warn("获取水池失败 playerId = {},gameType = {},roomCfgId = {}", gameData.playerId(), gameData.getGameType(), gameData.getRoomCfgId());
                result.code = Code.NOT_FOUND;
                return result;
            }

            //计算偏差范围
            long diff = BigDecimal.valueOf(poolValue.longValue() - poolInit).divide(BigDecimal.valueOf(poolInit), 6, RoundingMode.HALF_UP).multiply(tenThousandBigDecimal).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
            SpecialResultLibCfg libCfg = getLibCfgByPoolDiff(diff);
            if (libCfg == null) {
                log.warn("获取结果库配置失败 playerId = {},gameType = {},roomCfgId = {},diff = {}", gameData.playerId(), gameData.getGameType(), gameData.getRoomCfgId(), diff);
                result.code = Code.NOT_FOUND;
                return result;
            }
            result.data = libCfg;
            log.debug("根据水池偏差计算获取滚轴模式配置  playerId = {},poolValue = {},poolInit = {},diff = {},modelId = {}", gameData.playerId(), poolValue, poolInit, diff, libCfg.getModelId());
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
        return getPlayerGameData(playerController.playerId(), playerController.getPlayer().getRoomCfgId());
    }

    public T getPlayerGameData(long playerId, int roomCfgId) {
        Map<Long, T> temMap = this.gameDataMap.get(roomCfgId);
        if (temMap == null || temMap.isEmpty()) {
            return null;
        }

        return temMap.get(playerId);
    }


    /**
     * 创建玩家玩游戏的数据存储对象
     *
     * @param playerController
     * @return
     */
    public <DT extends SlotsPlayerGameDataDTO> T createPlayerGameData(PlayerController playerController) throws Exception {
        T playerGameData = getPlayerGameData(playerController);
        if (playerGameData != null) {
            return playerGameData;
        }

        DT playerGameDataDTO = (DT) getGameDataDao().getGameDataByPlayerId(playerController.playerId(), playerController.getPlayer().getRoomCfgId());
        if (playerGameDataDTO == null) {
            Constructor<T> constructor = this.playerGameDataClass.getConstructor();
            playerGameData = constructor.newInstance();

            //是否玩过该类游戏
            boolean hasPlay = historySlotsDao.hasPlaySlots(playerController.playerId(), playerGameData.getGameType());

            playerGameData.setGameType(playerController.getPlayer().getGameType());
            playerGameData.setRoomCfgId(playerController.getPlayer().getRoomCfgId());
            playerGameData.getHasPlaySlots().set(hasPlay);

            //设置默认押注
            BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(playerGameData.getRoomCfgId());
            playerGameData.setOneBetScore(baseRoomCfg.getDefaultBet().get(0));
            playerGameData.setAllBetScore(oneLineToAllStake(playerGameData.getOneBetScore()));
        } else {
            Constructor<T> constructor = this.playerGameDataClass.getConstructor();
            playerGameData = constructor.newInstance();

            BeanUtils.copyProperties(playerGameDataDTO, playerGameData);

            playerGameData = setGameDataValues(playerGameData, playerGameDataDTO);

            playerGameData.getHasPlaySlots().set(true);

            log.debug("从db中获取的playerGameData = {}", JSON.toJSONString(playerGameData));
        }
        playerGameData.setOnline(true);
        playerGameData.setPlayerController(playerController);
        return putGameData(playerController, playerGameData);
    }

    protected T putGameData(PlayerController playerController, T gameData) {
        return this.gameDataMap.computeIfAbsent(playerController.getPlayer().getRoomCfgId(), k -> new HashMap<>()).put(playerController.playerId(), gameData);
    }

    /**
     * 获取 specialResultLib 中的type
     *
     * @param gameType
     * @param modelId
     * @return
     */
    protected CommonResult<Integer> getResultLibType(int gameType, int modelId) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        PropInfo propInfo = getGenerateManager().getSpecialResultLibCacheData().getResultLibTypePropInfoMap().get(modelId);
        if (propInfo == null) {
            log.debug("未找到 specialResultLib 中 typeProp相关的权重信息 modelId = {},gameType = {}", modelId, gameType);
            result.code = Code.NOT_FOUND;
            return result;
        }
        Integer type = propInfo.getRandKey();
        if (type == null) {
            log.debug("specialResultLib 中 typeProp随机失败 modelId = {},gameType = {}", modelId, gameType);
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
     * @param libType specialResultLib 中的type
     * @return
     */
    protected CommonResult<Integer> getResultLibSection(int modelId, int libType) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        Map<Integer, PropInfo> tempPropMap = getGenerateManager().getSpecialResultLibCacheData().getResultLibSectionPropMap().get(modelId);
        if (tempPropMap == null) {
            log.debug("未找到 specialResultLib 中 section 相关的权重信息1 modelId = {},gameType = {},libType = {}", modelId, this.gameType, libType);
            result.code = Code.NOT_FOUND;
            return result;
        }
        PropInfo propInfo = tempPropMap.get(libType);
        if (propInfo == null || propInfo.getSum() < 1) {
            log.debug("未找到 specialResultLib 中 section 相关的权重信息2 modelId = {},gameType = {},libType = {}", modelId, this.gameType, libType);
            result.code = Code.NOT_FOUND;
            return result;
        }
        Integer index = propInfo.getRandKey();
        if (index == null) {
            log.debug("未找到 specialResultLib 中 section 相关的权重信息3 modelId = {},gameType = {},libType = {},index = {}", modelId, this.gameType, libType, index);
            result.code = Code.FAIL;
            return result;
        }
        result.data = index;
        int[] section = getGenerateManager().getSpecialResultLibCacheData().getResultLibSectionMap().get(libType).get(index);
        log.debug("成功获取区间 modelId = {},gameType = {},libType = {},intdex = {},sectionBegin = {},sectionEnd = {}", modelId, this.gameType, libType, index, section[0], section[1]);
        return result;
    }

    public long getPoolValueByPoolId(int poolId, long stake) throws Exception {
        PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
        return calPoolValue(stake, poolCfg.getGrowthRate(), poolCfg.getFakePoolInitTimes(), poolCfg.getFakePoolMax());
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
        BigDecimal prop = BigDecimal.valueOf(propValue).divide(tenThousandBigDecimal, 4, RoundingMode.HALF_UP);

        //循环时间
        BigDecimal circulTimeBigDecimal = BigDecimal.ONE.divide(prop, 4, RoundingMode.HALF_UP).multiply(intervalTime);

        //余数y
        int y = (TimeHelper.getNowDaySeconds() + delayTime) % circulTimeBigDecimal.intValue();

        //实际金额
        BigDecimal step1 = BigDecimal.valueOf(y).divide(intervalTime, 4, RoundingMode.HALF_UP);
        BigDecimal step2 = step1.multiply(prop);
        BigDecimal step3 = step2.multiply(maxPoolBigDecimal.subtract(initPoolBigDecimal));
        long addGold = initPoolBigDecimal.add(step3).longValue();

//        log.debug("概率计算可以中小奖池 playerId = {},rand = {},propV = {},intervalTime = {},y = {},addGold = {}", playerGameData.playerId(), rand, propV, intervalTime, y, addGold);
//        log.debug("计算火车奖池金额 stake = {},timeValue = {},propValue = {},y = {},addGold = {}", stake, timeValue, propValue, y, addGold);
        return addGold;
    }


    @Override
    public void onTimer(TimerEvent e) {
        if (this.checkOffLineEvent == e) {
            checkOffLine();
        } else if (this.clearAllLibEvent == e) {
            getResultLibDao().clearMongoLib();
            getResultLibDao().clearRedisLib();
            this.clearAllLibEvent = null;
            getResultLibDao().removeGenerateLock(this.gameType);
        } else if (this.clearRedisLibEvent == e) {
            getResultLibDao().clearRedisLib();
            this.clearRedisLibEvent = null;
            getResultLibDao().removeGenerateLock(this.gameType);
        } else if (this.generateLibEvent == e) {
            generate(this.generateLibEvent.getParameter());
            this.generateLibEvent = null;
        }
    }

    protected abstract <D extends AbstractResultLibDao> D getResultLibDao();

    protected abstract <D extends AbstractGameDataDao> D getGameDataDao();

    protected abstract <D extends AbstractSlotsGenerateManager> D getGenerateManager();

    protected T setGameDataValues(T d, SlotsPlayerGameDataDTO dto) {
        return null;
    }

    /**
     * 检查已离线的玩家，并且要保存数据
     */
    protected void checkOffLine() {
        int now = TimeHelper.nowInt();

        this.gameDataMap.forEach((key, value) -> value.entrySet().removeIf(en2 -> {
            T gameData = en2.getValue();
            if (gameData.isOnline()) {
                return false;
            }

            int diff = now - gameData.getLastActiveTime();
            //60s = 1分钟
            if (diff < 60) {
                return false;
            }
            offlineSaveGameDataDto(gameData);
            return true;
        }));
    }

    /**
     * 玩家离线保存gameDataDto
     */
    protected abstract void offlineSaveGameDataDto(T gameData);

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
        Map<Integer, BaseLineCfg> tempLineCfgMap = new HashMap<>();
        for (Map.Entry<Integer, BaseLineCfg> en : GameDataManager.getBaseLineCfgMap().entrySet()) {
            BaseLineCfg cfg = en.getValue();
            if (cfg.getGameType() == this.gameType) {
                tempLineCfgMap.put(cfg.getLineId(), cfg);
            }
        }
        this.lineCfgMap = tempLineCfgMap;
    }


    /**
     * 特殊玩法
     */
    protected void specialPlayConfig() {

    }

    protected void globalConfig() {
        Map<Integer, int[]> tmpBigWinShowMap = new HashMap<>();

        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_SWEET, SlotsConst.BigWinShow.SWEET, tmpBigWinShowMap);
        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_BIG, SlotsConst.BigWinShow.BIG, tmpBigWinShowMap);
        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_MEGA, SlotsConst.BigWinShow.MEGA, tmpBigWinShowMap);
        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_EPIC, SlotsConst.BigWinShow.EPIC, tmpBigWinShowMap);
        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_LEGENDARY, SlotsConst.BigWinShow.LEGENDARY, tmpBigWinShowMap);
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
        BaseLineCfg baseLineCfg = this.lineCfgMap.get(lineId);
        if (baseLineCfg == null) {
            return null;
        }
        return baseLineCfg.getPosLocation();
    }


    private boolean compareMiddleMaps(Map<Integer, Map<Integer, int[]>> map1, Map<Integer, Map<Integer, int[]>> map2) {

        if (!map1.keySet().equals(map2.keySet())) {
            return false;
        }

        for (Integer key : map1.keySet()) {
            Map<Integer, int[]> innerMap1 = map1.get(key);
            Map<Integer, int[]> innerMap2 = map2.get(key);

            if (!compareInnermostMaps(innerMap1, innerMap2)) {
                return false;
            }
        }

        return true;
    }

    private boolean compareInnermostMaps(Map<Integer, int[]> map1, Map<Integer, int[]> map2) {
        if (!map1.keySet().equals(map2.keySet())) {
            return false;
        }
        for (Integer key : map1.keySet()) {
            int[] array1 = map1.get(key);
            int[] array2 = map2.get(key);

            if (!Arrays.equals(array1, array2)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void changeSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(BaseRoomCfg.EXCEL_NAME, () -> baseRoomConfig())
                .addChangeSampleFileObserveWithCallBack(BaseLineCfg.EXCEL_NAME, () -> baseLineConfig())
                .addChangeSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, () -> globalConfig())
        ;
    }


    public boolean exit(PlayerController playerController) {
        return true;
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

    public Map<Integer, List<long[]>> getAllStakeMap() {
        return allStakeMap;
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

        return lineCount * stake * baseInitCfg.getBetMultiple().get(0) * baseInitCfg.getLineMultiple().get(0);
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

        marqueeManager.playerWinMarquee(data.getPlayerController().getPlayer().getNickName(),
                baseRoomCfg.getMarqueeTrigger().get(1).intValue(), baseRoomCfg.getNameid(), win);
    }

    /**
     * 通知其他节点，结果库变更
     */
    protected void noticeNodeLibChange(int changeType, List<SpecialResultLibCfg> cfgList) {
        try {
            List<ClusterClient> nodes = ClusterSystem.system.getNodesByTypeExcludeSelf(NodeType.GAME, this.gameType);
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

            if (changeType == SlotsConst.LibChangeType.CONFIG_CHANGE) {
                this.clearRedisLibEvent = new TimerEvent<>(this, 1, "clearRedisLibEvent").withTimeUnit(TimeUnit.MINUTES);
                this.timerCenter.add(this.clearRedisLibEvent);
            }

            log.info("通知其他节点，结果库变更 changeType : {}", changeType);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 添加测试icons
     *
     * @param playerController
     * @param testLibData
     */
    public void addTestIconData(PlayerController playerController, TestLibData testLibData) {
        T playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            return;
        }

        try {
            playerGameData.addTestIconsData(testLibData);
            log.info("添加测试libType成功 playerId = {},libType = {}", playerController.playerId(), testLibData.getLibType());
        } catch (Exception e) {
            log.error("", e);
        }
    }

}
