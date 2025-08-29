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
import com.jjg.game.slots.data.PropInfo;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.data.SpecialResultLibCacheData;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressResultLib;
import com.jjg.game.slots.logger.SlotsLogger;
import com.jjg.game.slots.pb.NoticeSlotsLibChange;

import com.jjg.game.slots.service.SlotsPlayerService;
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
    protected MarsCurator marsCurator;
    @Autowired
    protected CoreMarqueeManager marqueeManager;
    @Autowired
    protected SlotsLogger logger;

    //在更新结果库后，要开启清除旧结果库的定时事件
    protected TimerEvent<String> clearAllLibEvent;
    //生成结果库事件
    protected TimerEvent<String> generateLibEvent;
    //在更新结果库后，要开启清除旧结果库的定时事件
    protected TimerEvent<String> clearRedisLibEvent;
    //游戏类型
    protected int gameType;
    //在specualResultLib
    protected int defaultRewardSectionIndex = -1;


    protected Map<Long, T> gameDataMap = new ConcurrentHashMap<>();


    protected BigDecimal tenThousandBigDecimal = BigDecimal.valueOf(10000);
    protected BigDecimal oneHundredMillionBigDecimal = BigDecimal.valueOf(100000000);
    protected int oneHundredMillion = 100000000;

    protected Class<T> playerGameDataClass;

    //roomName -> cfg
    protected Map<Integer, BaseRoomCfg> roomCfgMap;
    //lineId -> cfg
    Map<Integer, BaseLineCfg> lineCfgMap;
    //modelId -> cfg
    protected Map<Integer, SpecialResultLibCfg> resultLibMap;
    //specialResultLib表中typeProp字段的随机权重信息 specialResultLib.modelId -> propInfo
    protected Map<Integer, PropInfo> resultLibTypePropInfoMap;
    //specialResultLib表中section字段的每个倍数的随机权重信息  modelId -> tpyeId -> PropInfo
    protected Map<Integer, Map<Integer, PropInfo>> resultLibSectionPropMap;
    //specialResultLib表中section字段的倍数区间  modelId -> tpyeId -> 下标id -> 倍数区间
    protected Map<Integer, Map<Integer, Map<Integer, int[]>>> resultLibSectionMap;
    //只会记录在玩游戏时需要修改格子的配置，生成结果库的修改格子配置不会缓存
    protected Map<Integer, SpecialGirdCfg> specialGirdCfgMap;

    //大奖展示倍数区间
    protected Map<Integer, int[]> bigWinShowMap = null;

    private List<ClientRollerCfg> clientRollerCfgList;
    private List<ClientFreeRollerCfg> clientFreeRollerCfgList;

    public AbstractSlotsGameManager(Class<T> playerGameDataClass) {
        this.playerGameDataClass = playerGameDataClass;
    }

    //总押分
    protected Map<Integer, List<Long>> allStakeMap;

    //检查离线玩家事件
    private TimerEvent<String> checkOffLineEvent;

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

    public <G extends AbstractGameRunInfo> G enterGame(long playerId) {
        return null;
    }

    protected void addCheckOffLineEvent() {
        this.checkOffLineEvent = new TimerEvent<>(this, "offLineEvent", 1).withTimeUnit(TimeUnit.MINUTES);
        timerCenter.add(this.checkOffLineEvent);
    }

    /**
     * 对外调用
     * 生成结果库
     *
     * @param count
     */
    public void generateLib(int count) {
        boolean lock = getResultLibDao().addGenerateLock(this.gameType);
        if (!lock) {
            log.debug("当前正在生成结果库，请勿打扰....");
            return;
        }

        log.info("开始生成结果库，预期生成 {} 条", count);

        String newDocName = getResultLibDao().getNewMongoLibName();
        int expectGenerateCount = count;
        int restCount = Math.min(count, 100);

        List<DollarExpressResultLib> libList = new ArrayList<>();
        int saveCount = 0;
        int i = 0;


        while (count > 0) {
            int reduceCount = 0;
            i++;
            try {
                List<DollarExpressResultLib> tempList = getGenerateManager().generateOne();
                reduceCount = tempList.size();

                libList.addAll(tempList);

                if (libList.size() >= restCount) {
                    saveCount += getResultLibDao().batchSave(libList, newDocName);
                    libList = new ArrayList<>();
                }

                if ((i % 2000) == 0) {
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                log.error("", e);
            } finally {
                count -= reduceCount;
            }
        }

        //加载到redis
        String redisTableName = getResultLibDao().moveToRedis(newDocName, this.resultLibSectionMap);
        specialResultLibConfig(this.gameType, false);

        log.info("生成结果库结束，预期 {} 条，成功保存到数据库 {} 条,mongoName = {},redisName = {}", expectGenerateCount, saveCount, newDocName, redisTableName);

        this.clearAllLibEvent = new TimerEvent<>(this, 1, "clearLibEvent").withTimeUnit(TimeUnit.MINUTES);
        this.timerCenter.add(this.clearAllLibEvent);

        //通知其他节点，结果库变更
        noticeNodeLibChange(SlotsConst.LibChangeType.LIB_CHANGE, Collections.EMPTY_LIST);
    }


    /**
     * 关闭
     */
    public void shutdown() {
        this.gameDataMap.entrySet().forEach(en -> {
            T gameData = en.getValue();
            offlineSaveGameDataDto(gameData);
        });
    }

    public void clearPlayerEvent(long playerId) {

    }

    protected void initConfig() {
        baseRoomConfig(this.gameType);
        baseLineConfig(this.gameType);
        specialResultLibConfig(this.gameType, true);
        specialGirdConfig(this.gameType);
        globalConfig(this.gameType);
        clientRollerConfig(this.gameType);
        clientFreeRollerConfig(this.gameType);
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
        for (Map.Entry<Integer, SpecialResultLibCfg> en : this.resultLibMap.entrySet()) {
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
        return this.gameDataMap.get(playerController.playerId());
    }

    /**
     * 创建玩家玩游戏的数据存储对象
     *
     * @param playerController
     * @return
     */
    public <DT extends SlotsPlayerGameDataDTO> T createPlayerGameData(PlayerController playerController) throws Exception {
        T playerGameData = gameDataMap.get(playerController.playerId());
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
            playerGameData.setLastStake(baseRoomCfg.getDefaultBet().get(0));
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
        return gameDataMap.put(playerController.playerId(), playerGameData);
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
        PropInfo propInfo = this.resultLibTypePropInfoMap.get(modelId);
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
        Map<Integer, PropInfo> tempPropMap = this.resultLibSectionPropMap.get(modelId);
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
        int[] section = this.resultLibSectionMap.get(modelId).get(libType).get(index);
        log.debug("成功获取区间 modelId = {},gameType = {},libType = {},intdex = {},sectionBegin = {},sectionEnd = {}", modelId, this.gameType, libType, index, section[0], section[1]);
        return result;
    }

    public boolean addGenerateLibEvent(int count) {
        if (this.generateLibEvent != null) {
            log.debug("当前有未执行的生成结果库任务，所以添加失败 count = {}", count);
            return false;
        }

        boolean lock = getResultLibDao().getGenerateLock(this.gameType);
        if (lock) {
            log.debug("当前正在执行生成结果库任务，请勿打扰.... count = {}", count);
            return false;
        }
        this.generateLibEvent = new TimerEvent<>(this, 10, "generateLibEvent_" + count).withTimeUnit(TimeUnit.SECONDS);
        this.timerCenter.add(this.generateLibEvent);
        return true;
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
            Integer count = Integer.parseInt(e.getParameter().toString().split("_")[1]);
            generateLib(count);
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
        this.gameDataMap.entrySet().removeIf(en -> {
            T gameData = en.getValue();
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
        });
    }

    /**
     * 玩家离线保存gameDataDto
     */
    protected abstract void offlineSaveGameDataDto(T gameData);

    /*****************************************************************************************************************************/

    /**
     * room配置
     *
     * @param gameType
     */
    protected void baseRoomConfig(int gameType) {
        Map<Integer, BaseRoomCfg> tempRoomCfgMap = new HashMap<>();
        for (Map.Entry<Integer, BaseRoomCfg> en : GameDataManager.getBaseRoomCfgMap().entrySet()) {
            BaseRoomCfg cfg = en.getValue();
            if (cfg.getGameType() == gameType) {
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
     *
     * @param gameType
     */
    protected void baseLineConfig(int gameType) {
        Map<Integer, BaseLineCfg> tempLineCfgMap = new HashMap<>();
        for (Map.Entry<Integer, BaseLineCfg> en : GameDataManager.getBaseLineCfgMap().entrySet()) {
            BaseLineCfg cfg = en.getValue();
            if (cfg.getGameType() == gameType) {
                tempLineCfgMap.put(cfg.getLineId(), cfg);
            }
        }

        if (tempLineCfgMap.isEmpty()) {
            throw new IllegalArgumentException("该游戏 BaseLineCfg 为空,初始化失败 gameType = " + gameType);
        }
        this.lineCfgMap = tempLineCfgMap;
    }

    /**
     * resultLib配置
     *
     * @param gameType
     * @param init     是否为初始化时调用
     */
    protected void specialResultLibConfig(int gameType, boolean init) {
        List<SpecialResultLibCfg> cfgList = new ArrayList<>();
        for (Map.Entry<Integer, SpecialResultLibCfg> en : GameDataManager.getSpecialResultLibCfgMap().entrySet()) {
            SpecialResultLibCfg cfg = en.getValue();
            if (cfg.getGameType() != gameType) {
                continue;
            }
            cfgList.add(cfg);
        }

        SpecialResultLibCacheData data = calSpecialResultLibCacheData(cfgList);
        if (data == null) {
            log.debug("计算分析缓存 specialResultLib 配置失败 gameType = {},init = {}", gameType, init);
            return;
        }

        //如果不是初始化，并且配置是区间变化
        //就要重新将mongodb的结果集加载到redis，并且通知其他节点
        if (!init && !compareSectionMap(data.getResultLibSectionMap(), this.resultLibSectionMap)) {
            String docName = getResultLibDao().getCurrentMongoLibNameFromRedis();
            //加锁
            getResultLibDao().addGenerateLock(this.gameType);
            //将结果集重新分类，转移到redis
            getResultLibDao().moveToRedis(docName, data.getResultLibSectionMap());
            //通知其他节点，结果库变更
            noticeNodeLibChange(SlotsConst.LibChangeType.CONFIG_CHANGE, cfgList);
        }

        updateSpecialResultLibCacheData(data);

        log.info("计算分析缓存 specialResultLib 配置成功 gameType = {}", gameType);
    }

    public void updateSpecialResultLibCacheData(SpecialResultLibCacheData data) {
        this.defaultRewardSectionIndex = data.getDefaultRewardSectionIndex();
        this.resultLibMap = data.getResultLibMap();
        this.resultLibTypePropInfoMap = data.getResultLibTypePropInfoMap();
        this.resultLibSectionPropMap = data.getResultLibSectionPropMap();
        this.resultLibSectionMap = data.getResultLibSectionMap();
    }

    /**
     * 计算分析specialResultLib表
     */
    public SpecialResultLibCacheData calSpecialResultLibCacheData(List<SpecialResultLibCfg> cfgList) {
        if (cfgList == null || cfgList.isEmpty()) {
            return null;
        }
        Map<Integer, SpecialResultLibCfg> tempLibCfgMap = new HashMap<>();
        Map<Integer, PropInfo> tempResultLibTypePropInfoMap = new HashMap<>();

        Map<Integer, Map<Integer, PropInfo>> tempResultLibSectionPropMap = new HashMap<>();
        Map<Integer, Map<Integer, Map<Integer, int[]>>> tempResultLibSectionMap = new HashMap<>();

        int tmpDefaultRewardSectionIndex = -1;

        for (SpecialResultLibCfg cfg : cfgList) {
            tempLibCfgMap.put(cfg.getModelId(), cfg);

            //计算typeProp
            if (cfg.getTypeProp() != null && !cfg.getTypeProp().isEmpty()) {
                PropInfo propInfo = new PropInfo();

                int begin = 0;
                int end = 0;
                for (Map.Entry<Integer, Integer> en2 : cfg.getTypeProp().entrySet()) {
                    begin = end;
                    end += en2.getValue();
                    propInfo.addProp(en2.getKey(), begin, end);
                }
                propInfo.setSum(end);
                tempResultLibTypePropInfoMap.put(cfg.getModelId(), propInfo);
            }

            //计算sectionProp
            if (cfg.getSectionProp() != null && !cfg.getSectionProp().isEmpty()) {
                Map<Integer, PropInfo> typeSectionPropMap = tempResultLibSectionPropMap.computeIfAbsent(cfg.getModelId(), k -> new HashMap<>());
                Map<Integer, Map<Integer, int[]>> typeSectionMap = tempResultLibSectionMap.computeIfAbsent(cfg.getModelId(), k -> new HashMap<>());

                for (Map.Entry<Integer, List<String>> en2 : cfg.getSectionProp().entrySet()) {
                    PropInfo propInfo = new PropInfo();

                    int type = en2.getKey();
                    Map<Integer, int[]> sectionMap = typeSectionMap.computeIfAbsent(type, k -> new HashMap<>());

                    List<String> propList = en2.getValue();

                    int begin = 0;
                    int end = 0;
                    for (int i = 0; i < propList.size(); i++) {

                        String prop = propList.get(i);
                        String[] arr = prop.split("-");
                        String[] arr2 = arr[0].split("&");

                        begin = end;
                        end += Integer.parseInt(arr[1]);
                        propInfo.addProp(i, begin, end);

                        //倍数区间
                        int[] tmpArr = new int[]{Integer.parseInt(arr2[0]), Integer.parseInt(arr2[1])};
                        sectionMap.put(i, tmpArr);
                        if (tmpArr[0] == 0) {
                            tmpDefaultRewardSectionIndex = i;
                        }
                    }
                    propInfo.setSum(end);
                    typeSectionPropMap.put(type, propInfo);
                }
            }
        }

        if (tempLibCfgMap.isEmpty() || tempResultLibTypePropInfoMap.isEmpty() || tempResultLibSectionPropMap.isEmpty() || tempResultLibSectionMap.isEmpty()) {
            throw new IllegalArgumentException("该游戏specialResultLib 为空,初始化失败 gameType = " + gameType);
        }

        if (tmpDefaultRewardSectionIndex < 0) {
            throw new IllegalArgumentException("该游戏specialResultLib 中没有配置0倍区间 gameType = " + gameType);
        }

        SpecialResultLibCacheData data = new SpecialResultLibCacheData();
        data.setDefaultRewardSectionIndex(tmpDefaultRewardSectionIndex);
        data.setResultLibMap(tempLibCfgMap);
        data.setResultLibTypePropInfoMap(tempResultLibTypePropInfoMap);
        data.setResultLibSectionPropMap(tempResultLibSectionPropMap);
        data.setResultLibSectionMap(tempResultLibSectionMap);
        return data;
    }

    protected void specialGirdConfig(int gameType) {
        Map<Integer, SpecialGirdCfg> tempSpecialGirdCfgMap = new HashMap<>();

        for (Map.Entry<Integer, SpecialGirdCfg> en : GameDataManager.getSpecialGirdCfgMap().entrySet()) {
            SpecialGirdCfg cfg = en.getValue();
            if (cfg.getGameType() != gameType) {
                continue;
            }
            if (cfg.getGirdUpdateType() != SlotsConst.SpecialGird.GIRD_UPDATE_TYPE_APPOINT) {
                continue;
            }
            tempSpecialGirdCfgMap.put(cfg.getId(), cfg);
        }

        if (!tempSpecialGirdCfgMap.isEmpty()) {
            this.specialGirdCfgMap = tempSpecialGirdCfgMap;
        }
    }

    protected void globalConfig(int gameType) {
        Map<Integer, int[]> tmpBigWinShowMap = new HashMap<>();

        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_SWEET, SlotsConst.BigWinShow.SWEET, tmpBigWinShowMap);
        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_BIG, SlotsConst.BigWinShow.BIG, tmpBigWinShowMap);
        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_MEGA, SlotsConst.BigWinShow.MEGA, tmpBigWinShowMap);
        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_EPIC, SlotsConst.BigWinShow.EPIC, tmpBigWinShowMap);
        calGlobalBigWinShow(GameConstant.GlobalConfig.ID_LEGENDARY, SlotsConst.BigWinShow.LEGENDARY, tmpBigWinShowMap);
        this.bigWinShowMap = tmpBigWinShowMap;
    }

    protected void clientRollerConfig(int gameType) {
        List<ClientRollerCfg> tmpClientRollerCfgList = new ArrayList<>();
        for (Map.Entry<Integer, ClientRollerCfg> en : GameDataManager.getClientRollerCfgMap().entrySet()) {
            ClientRollerCfg cfg = en.getValue();
            if (cfg.getGameType() != gameType) {
                continue;
            }
            tmpClientRollerCfgList.add(cfg);
        }
        this.clientRollerCfgList = tmpClientRollerCfgList;
    }

    protected void clientFreeRollerConfig(int gameType) {
        List<ClientFreeRollerCfg> tmpClientFreeRollerCfgList = new ArrayList<>();
        for (Map.Entry<Integer, ClientFreeRollerCfg> en : GameDataManager.getClientFreeRollerCfgMap().entrySet()) {
            ClientFreeRollerCfg cfg = en.getValue();
            if (cfg.getGameType() != gameType) {
                continue;
            }
            tmpClientFreeRollerCfgList.add(cfg);
        }
        this.clientFreeRollerCfgList = tmpClientFreeRollerCfgList;
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
        for (Map.Entry<Integer, List<Integer>> en : baseLineCfg.getPosLocation().entrySet()) {
            return en.getValue();
        }
        return null;
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
        addChangeSampleFileObserveWithCallBack(BaseRoomCfg.EXCEL_NAME, () -> baseRoomConfig(this.gameType))
                .addChangeSampleFileObserveWithCallBack(SpecialResultLibCfg.EXCEL_NAME, () -> specialResultLibConfig(this.gameType, false))
                .addChangeSampleFileObserveWithCallBack(BaseLineCfg.EXCEL_NAME, () -> baseLineConfig(this.gameType))
                .addChangeSampleFileObserveWithCallBack(SpecialGirdCfg.EXCEL_NAME, () -> specialGirdConfig(this.gameType))
                .addChangeSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, () -> globalConfig(this.gameType))
                .addChangeSampleFileObserveWithCallBack(ClientRollerCfg.EXCEL_NAME, () -> clientRollerConfig(this.gameType))
                .addChangeSampleFileObserveWithCallBack(ClientFreeRollerCfg.EXCEL_NAME, () -> clientFreeRollerConfig(this.gameType))
        ;
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

    public List<ClientRollerCfg> getClientRollerCfgList() {
        return clientRollerCfgList;
    }

    public List<ClientFreeRollerCfg> getClientFreeRollerCfgList() {
        return clientFreeRollerCfgList;
    }

    /**
     * 将单线押分转化为总押分
     */
    public void calAllLineStake() {
        Map<Integer, List<Long>> tmpAllStakeMap = new HashMap<>();

        int lineCount = getGenerateManager().getBaseInitCfg().getMaxLine();
        for (Map.Entry<Integer, BaseRoomCfg> en : this.roomCfgMap.entrySet()) {
            BaseRoomCfg cfg = en.getValue();
            for (long stake : cfg.getLineBetScore()) {
                long allStake = lineCount * stake * cfg.getBetMultiple().get(0) * cfg.getLineMultiple().get(0);
                tmpAllStakeMap.computeIfAbsent(cfg.getId(), k -> new ArrayList<>()).add(allStake);
            }
        }

        this.allStakeMap = tmpAllStakeMap;
    }

    public Map<Integer, List<Long>> getAllStakeMap() {
        return allStakeMap;
    }

    /**
     * 单线押分转化为总押分
     *
     * @param stake
     * @param roonCfgId
     * @return
     */
    public long oneLineToAllStake(int stake, int roonCfgId) {
        int lineCount = getGenerateManager().getBaseInitCfg().getMaxLine();

        BaseRoomCfg cfg = GameDataManager.getBaseRoomCfg(roonCfgId);
        return lineCount * stake * cfg.getBetMultiple().get(0) * cfg.getLineMultiple().get(0);
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
}
