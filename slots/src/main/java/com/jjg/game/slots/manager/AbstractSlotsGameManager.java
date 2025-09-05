package com.jjg.game.slots.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.AbstractGameRunInfo;
import com.jjg.game.core.data.CommonResult;
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
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressPlayerGameData;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressResultLib;
import com.jjg.game.slots.game.dollarexpress.pb.DollarsInfo;
import com.jjg.game.slots.game.dollarexpress.pb.TrainInfo;
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
 * @author 11
 * @date 2025/7/1 16:42
 */
public abstract class AbstractSlotsGameManager<T extends SlotsPlayerGameData> implements TimerListener, ConfigExcelChangeListener {
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

    //游戏类型
    protected int gameType;
    //在specualResultLib
    protected int defaultRewardSectionIndex = -1;

    //roomCfgId -> playerId ->gameData
    protected Map<Integer,Map<Long, T>> gameDataMap = new ConcurrentHashMap<>();


    protected BigDecimal tenThousandBigDecimal = BigDecimal.valueOf(10000);
    protected BigDecimal oneHundredMillionBigDecimal = BigDecimal.valueOf(100000000);
    protected int oneHundredMillion = 100000000;

    protected Class<T> playerGameDataClass;

    //roomName -> cfg
    protected Map<Integer, BaseRoomCfg> roomCfgMap;
    //lineId -> cfg
    Map<Integer, BaseLineCfg> lineCfgMap;


    //大奖展示倍数区间
    protected Map<Integer, int[]> bigWinShowMap = null;


    public AbstractSlotsGameManager(Class<T> playerGameDataClass) {
        this.playerGameDataClass = playerGameDataClass;
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
    protected TimerEvent<Map<Integer,Integer>> generateLibEvent;
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
     * @param libTypeCountMap
     * @return
     */
    public boolean addGenerateLibEvent(Map<Integer,Integer> libTypeCountMap) {
        if (this.generateLibEvent != null) {
            log.debug("当前有未执行的生成结果库任务，所以添加失败");
            return false;
        }

        if(libTypeCountMap == null || libTypeCountMap.isEmpty()) {
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
    protected void generate(Map<Integer,Integer> libTypeCountMap) {
        String newDocName = null;
        String redisTableName = null;
        try {
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

                        if(resEn == null){
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
                        saveCount += getResultLibDao().batchSave(libList, newDocName);
                        libList = new ArrayList<>();
                    }
                }
            }

            if(libList.size() > 0) {
//                System.out.println("保存这里的222 size = " + libList.size());
                saveCount += getResultLibDao().batchSave(libList, newDocName);
            }

            log.debug("生成结束，开始转移到redis, newDocName = {}", newDocName);
            //加载到redis
            redisTableName = getResultLibDao().moveToRedis(newDocName, getGenerateManager().getSpecialResultLibCacheData().getResultLibSectionMap());

            log.info("生成结果库结束，实际循环次数 = {},成功保存到数据库 {} 条,mongoName = {},redisName = {}", currentForCount,saveCount, newDocName, redisTableName);

            this.clearAllLibEvent = new TimerEvent<>(this, 1, "clearLibEvent").withTimeUnit(TimeUnit.MINUTES);
            this.timerCenter.add(this.clearAllLibEvent);

            //通知其他节点，结果库变更
            noticeNodeLibChange(SlotsConst.LibChangeType.LIB_CHANGE, Collections.EMPTY_LIST);
        } catch (Exception e) {
            if(StringUtils.isNotEmpty(newDocName)) {
                getResultLibDao().clearMongoLib(newDocName);
            }
            if(StringUtils.isNotEmpty(redisTableName)) {
                getResultLibDao().clearRedisLib(redisTableName);
            }
            getResultLibDao().removeGenerateLock(this.gameType);
            log.error("", e);
        }
    }

    public <G extends AbstractGameRunInfo> G enterGame(PlayerController playerController) {
        return null;
    }

    protected void addCheckOffLineEvent() {
        this.checkOffLineEvent = new TimerEvent<>(this, "offLineEvent", 1).withTimeUnit(TimeUnit.MINUTES);
        timerCenter.add(this.checkOffLineEvent);
    }

    /**
     * 关闭
     */
    public void shutdown() {
        this.gameDataMap.forEach((k,v) -> {
            v.forEach((k1,v1) -> {
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
        return getPlayerGameData(playerController.playerId(),playerController.getPlayer().getRoomCfgId());
    }

    public T getPlayerGameData(long playerId,int roomCfgId) {
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


    @Override
    public void onTimer(TimerEvent e) {
        if (this.checkOffLineEvent == e) {
            checkOffLine();
        }else if (this.clearAllLibEvent == e) {
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

    protected <D extends AbstractResultLibDao> D getResultLibDao() {
        return null;
    }

    protected <D extends AbstractGameDataDao> D getGameDataDao() {
        return null;
    }

    protected <D extends AbstractSlotsGenerateManager> D getGenerateManager() {
        return null;
    }

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

        if (tempLineCfgMap.isEmpty()) {
            throw new IllegalArgumentException("该游戏 BaseLineCfg 为空,初始化失败 gameType = " + this.gameType);
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

    /**
     * 检测是否区间变化
     *
     * @param map1
     * @param map2
     */
    private boolean compareSectionMap(Map<Integer, Map<Integer, Map<Integer, int[]>>> map1, Map<Integer, Map<Integer, Map<Integer, int[]>>> map2) {
        // 比较第一层
        if (!map1.keySet().equals(map2.keySet())) {
            return false;
        }

        // 比较每一层
        for (Integer key : map1.keySet()) {
            Map<Integer, Map<Integer, int[]>> innerMap1 = map1.get(key);
            Map<Integer, Map<Integer, int[]>> innerMap2 = map2.get(key);

            if (!compareMiddleMaps(innerMap1, innerMap2)) {
                return false;
            }
        }

        return true;
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

}
