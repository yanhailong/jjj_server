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
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.dao.PlayerHistorySlotsDao;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.data.PropInfo;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SpecialResultLibCacheData;
import com.jjg.game.slots.pb.NoticeSlotsLibChange;
import com.jjg.game.slots.sample.GameDataManager;
import com.jjg.game.slots.sample.bean.*;
import com.jjg.game.slots.service.SlotsPlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    //在更新结果库后，要开启清除旧结果库的定时事件
    protected TimerEvent<String> clearAllLibEvent;
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
    protected Map<Integer, Map<Integer,PropInfo>> resultLibSectionPropMap;
    //specialResultLib表中section字段的倍数区间  modelId -> tpyeId -> 下标id -> 倍数区间
    protected Map<Integer, Map<Integer,Map<Integer,int[]>>> resultLibSectionMap;
    //只会记录在玩游戏时需要修改格子的配置，生成结果库的修改格子配置不会缓存
    protected Map<Integer,SpecialGirdCfg> specialGirdCfgMap;

    //大奖展示倍数区间
    protected Map<Integer,int[]> bigWinShowMap = null;

    public AbstractSlotsGameManager(Class<T> playerGameDataClass) {
        this.playerGameDataClass = playerGameDataClass;
    }

    /**
     * 初始化
     */
    public abstract void init();

    /**
     * 关闭
     */
    public abstract void shutdown();

    protected void initConfig() {
        baseRoomConfig(this.gameType);
        baseLineConfig(this.gameType);
        specialResultLibConfig(this.gameType,true);
        specialGirdConfig(this.gameType);
        globalConfig(this.gameType);
        log.info("配置重新计算结束 gameType = {}", this.gameType);
    }


    /**
     * 根据水池偏差值获取 结果库配置
     *
     * @param gameType
     * @param diff
     * @return
     */
    protected SpecialResultLibCfg getLibCfgByPoolDiff(int gameType, long diff) {
        for (Map.Entry<Integer, SpecialResultLibCfg> en : GameDataManager.getSpecialResultLibCfgMap().entrySet()) {
            SpecialResultLibCfg cfg = en.getValue();
            if (cfg.getGameType() != gameType) {
                continue;
            }
            if (diff >= cfg.getEnterLimitMin() && diff < cfg.getEnterLimitMax()) {
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
    protected CommonResult<SpecialResultLibCfg> getLibCfg(T gameData, long poolInit) {
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
            log.debug("玩家第一次玩该slots游戏，选择默认模式，playerId = {},gameType = {},modelId = {}", gameData.playerId(), gameData.getGameType(),SlotsConst.Common.FIRST_GAME_GET_MODEL_ID);
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
            SpecialResultLibCfg libCfg = getLibCfgByPoolDiff(gameData.getGameType(), diff);
            if (libCfg == null) {
                log.warn("获取结果库配置失败 playerId = {},gameType = {},roomCfgId = {},diff = {}", gameData.playerId(), gameData.getGameType(), gameData.getRoomCfgId(), diff);
                result.code = Code.NOT_FOUND;
                return result;
            }
            result.data = libCfg;
            log.debug("根据水池偏差计算获取滚轴模式配置  playerId = {},poolValue = {},poolInit = {},diff = {},modelId = {}", gameData.playerId(), poolValue, poolInit, diff,libCfg.getModelId());
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
    public T createPlayerGameData(PlayerController playerController) {
        boolean hasPlay = historySlotsDao.hasPlaySlots(playerController.playerId(), playerController.getPlayer().getGameType());

        T playerGameData = gameDataMap.computeIfAbsent(playerController.playerId(), k -> {
            try {
                Constructor<T> constructor = this.playerGameDataClass.getConstructor();
                return constructor.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });


        playerGameData.setPlayerController(playerController);
        playerGameData.setGameType(playerController.getPlayer().getGameType());
        playerGameData.setRoomCfgId(playerController.getPlayer().getRoomCfgId());
        playerGameData.getHasPlaySlots().set(hasPlay);

        //设置默认押注
        BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(playerGameData.getRoomCfgId());
        playerGameData.setLastStake(baseRoomCfg.getDefaultBet().get(0));
        return playerGameData;
    }

    /**
     * 获取 specialResultLib 中的type
     * @param gameType
     * @param modelId
     * @return
     */
    protected CommonResult<Integer> getResultLibType(int gameType,int modelId) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        PropInfo propInfo = this.resultLibTypePropInfoMap.get(modelId);
        if(propInfo == null){
            log.debug("未找到 specialResultLib 中 typeProp相关的权重信息 modelId = {},gameType = {}", modelId, gameType);
            result.code = Code.NOT_FOUND;
            return result;
        }
        Integer type = propInfo.getRandKey();
        if(type == null){
            log.debug("specialResultLib 中 typeProp随机失败 modelId = {},gameType = {}", modelId, gameType);
            result.code = Code.FAIL;
            return result;
        }
        result.data = type;
        return result;
    }

    /**
     * 获取 specialResultLib 中的倍数区间
     * @param modelId
     * @param libType  specialResultLib 中的type
     * @return
     */
    protected CommonResult<Integer> getResultLibSection(int modelId,int libType) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        Map<Integer, PropInfo> tempPropMap = this.resultLibSectionPropMap.get(modelId);
        if(tempPropMap == null){
            log.debug("未找到 specialResultLib 中 section 相关的权重信息1 modelId = {},gameType = {},libType = {}", modelId, this.gameType,libType);
            result.code = Code.NOT_FOUND;
            return result;
        }
        PropInfo propInfo = tempPropMap.get(libType);
        if(propInfo == null){
            log.debug("未找到 specialResultLib 中 section 相关的权重信息2 modelId = {},gameType = {},libType = {}", modelId, this.gameType,libType);
            result.code = Code.NOT_FOUND;
            return result;
        }
        Integer index = propInfo.getRandKey();
        if(index == null){
            log.debug("未找到 specialResultLib 中 section 相关的权重信息3 modelId = {},gameType = {},libType = {},index = {}", modelId, this.gameType,libType,index);
            result.code = Code.FAIL;
            return result;
        }
        result.data = index;
        int[] section = this.resultLibSectionMap.get(modelId).get(libType).get(index);
        log.debug("成功获取区间 modelId = {},gameType = {},libType = {},intdex = {},sectionBegin = {},sectionEnd = {}", modelId, this.gameType,libType,index,section[0],section[1]);
        return result;
    }

    @Override
    public void onTimer(TimerEvent e) {
        if(this.clearAllLibEvent == e){
            getResultLibDao().clearMongoLib();
            getResultLibDao().clearRedisLib();
            this.clearAllLibEvent = null;
            getResultLibDao().removeGenerateLock(this.gameType);
        }else if(this.clearRedisLibEvent == e){
            getResultLibDao().clearRedisLib();
            this.clearRedisLibEvent = null;
            getResultLibDao().removeGenerateLock(this.gameType);
        }
    }

    protected <D extends AbstractResultLibDao> D getResultLibDao() {
        return null;
    }

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
                tempRoomCfgMap.put(cfg.getRoomName(), cfg);
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
     * @param init 是否为初始化时调用
     */
    protected void specialResultLibConfig(int gameType,boolean init) {
        List<SpecialResultLibCfg> cfgList = new ArrayList<>();
        for (Map.Entry<Integer, SpecialResultLibCfg> en : GameDataManager.getSpecialResultLibCfgMap().entrySet()) {
            SpecialResultLibCfg cfg = en.getValue();
            if (cfg.getGameType() != gameType) {
                continue;
            }
            cfgList.add(cfg);
        }

        SpecialResultLibCacheData data = calSpecialResultLibCacheData(cfgList);
        if(data == null){
            log.debug("计算分析缓存 specialResultLib 配置失败 gameType = {},init = {}", gameType,init);
            return;
        }

        //如果不是初始化，并且配置是区间变化
        //就要重新将mongodb的结果集加载到redis，并且通知其他节点
        if(!init && !compareSectionMap(data.getResultLibSectionMap(),this.resultLibSectionMap)){
            String docName = getResultLibDao().getCurrentMongoLibNameFromRedis();
            //加锁
            getResultLibDao().addGenerateLock(this.gameType);
            //将结果集重新分类，转移到redis
            getResultLibDao().moveToRedis(docName,data.getResultLibSectionMap());
            //通知其他节点，结果库变更
            noticeNodeLibChange(SlotsConst.LibChangeType.CONFIG_CHANGE,cfgList);
        }

        updateSpecialResultLibCacheData(data);
    }

    public void updateSpecialResultLibCacheData(SpecialResultLibCacheData data){
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
        if(cfgList == null || cfgList.isEmpty()){
            return null;
        }
        Map<Integer, SpecialResultLibCfg> tempLibCfgMap = new HashMap<>();
        Map<Integer, PropInfo> tempResultLibTypePropInfoMap = new HashMap<>();

        Map<Integer, Map<Integer,PropInfo>> tempResultLibSectionPropMap = new HashMap<>();
        Map<Integer, Map<Integer,Map<Integer,int[]>>> tempResultLibSectionMap = new HashMap<>();

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
                Map<Integer,Map<Integer,int[]>> typeSectionMap = tempResultLibSectionMap.computeIfAbsent(cfg.getModelId(), k -> new HashMap<>());

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
                        int[] tmpArr = new int[]{Integer.parseInt(arr2[0]),Integer.parseInt(arr2[1])};
                        sectionMap.put(i,tmpArr);
                        if(tmpArr[0] == 0){
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

        if(tmpDefaultRewardSectionIndex < 0){
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
        Map<Integer,SpecialGirdCfg> tempSpecialGirdCfgMap = new HashMap<>();

        for(Map.Entry<Integer,SpecialGirdCfg> en : GameDataManager.getSpecialGirdCfgMap().entrySet()){
            SpecialGirdCfg cfg = en.getValue();
            if (cfg.getGameType() != gameType) {
                continue;
            }
            if(cfg.getGirdUpdateType() != SlotsConst.SpecialGird.GIRD_UPDATE_TYPE_APPOINT){
                continue;
            }
            tempSpecialGirdCfgMap.put(cfg.getId(), cfg);
        }

        if(!tempSpecialGirdCfgMap.isEmpty()){
            this.specialGirdCfgMap = tempSpecialGirdCfgMap;
        }
    }

    protected void globalConfig(int gameType) {
        Map<Integer,int[]> tmpBigWinShowMap = new HashMap<>();

        calGlobalBigWinShow(SlotsConst.GlobalConfig.ID_SWEET,tmpBigWinShowMap);
        calGlobalBigWinShow(SlotsConst.GlobalConfig.ID_BIG,tmpBigWinShowMap);
        calGlobalBigWinShow(SlotsConst.GlobalConfig.ID_MEGA,tmpBigWinShowMap);
        calGlobalBigWinShow(SlotsConst.GlobalConfig.ID_EPIC,tmpBigWinShowMap);
        calGlobalBigWinShow(SlotsConst.GlobalConfig.ID_LEGENDARY,tmpBigWinShowMap);
        this.bigWinShowMap = tmpBigWinShowMap;
    }

    protected void calGlobalBigWinShow(int id,Map<Integer,int[]> map) {
        GolbalConfigCfg cfg = GameDataManager.getGolbalConfigCfg(id);
        String[] arr = cfg.getValue().trim().split(",");
        map.put(id,new int[]{Integer.parseInt(arr[0]),Integer.parseInt(arr[1])});
    }

    public int getGameType() {
        return gameType;
    }

    public Map<Integer, BaseRoomCfg> getRoomCfgMap() {
        return roomCfgMap;
    }


    /**
     * 根据线id获取这条线上的icon坐标
     * @param lineId
     * @return
     */
    public List<Integer> getIconIndexsByLineId(int lineId) {
        BaseLineCfg baseLineCfg = this.lineCfgMap.get(lineId);
        if(baseLineCfg == null){
            return null;
        }
        for(Map.Entry<Integer,List<Integer>> en : baseLineCfg.getPosLocation().entrySet()){
            return en.getValue();
        }
        return null;
    }

    /**
     * 检测是否区间变化
     * @param map1
     * @param map2
     */
    private boolean compareSectionMap(Map<Integer, Map<Integer,Map<Integer,int[]>>> map1,Map<Integer, Map<Integer,Map<Integer,int[]>>> map2){
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

    private boolean compareMiddleMaps(Map<Integer, Map<Integer, int[]>> map1,Map<Integer, Map<Integer, int[]>> map2) {

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

    private boolean compareInnermostMaps(Map<Integer, int[]> map1,Map<Integer, int[]> map2) {
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
            .addChangeSampleFileObserveWithCallBack(SpecialResultLibCfg.EXCEL_NAME, () -> specialResultLibConfig(this.gameType,false))
            .addChangeSampleFileObserveWithCallBack(BaseLineCfg.EXCEL_NAME, () -> baseLineConfig(this.gameType))
            .addChangeSampleFileObserveWithCallBack(SpecialGirdCfg.EXCEL_NAME, () -> specialGirdConfig(this.gameType))
            .addChangeSampleFileObserveWithCallBack(GolbalConfigCfg.EXCEL_NAME, () -> globalConfig(this.gameType));
    }

    /**
     * 通知其他节点，结果库变更
     */
    protected void noticeNodeLibChange(int changeType,List<SpecialResultLibCfg> cfgList){
        try{
            List<ClusterClient> nodes = ClusterSystem.system.getNodesByTypeExcludeSelf(NodeType.GAME,this.gameType);
            if(nodes.isEmpty()){
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

            for(ClusterClient node : nodes){
                node.write(msg);
            }

            if(changeType == SlotsConst.LibChangeType.CONFIG_CHANGE){
                this.clearRedisLibEvent = new TimerEvent<>(this, 1, "clearRedisLibEvent").withTimeUnit(TimeUnit.MINUTES);
                this.timerCenter.add(this.clearRedisLibEvent);
            }

            log.info("通知其他节点，结果库变更 changeType : {}" ,changeType);
        }catch (Exception e){
            log.error("",e);
        }
    }

    public boolean exit(PlayerController playerController) {
        return true;
    }

    protected int getBigShowIdByTimes(int times){
        if(times < 1){
            return 0;
        }
        Map.Entry<Integer, int[]> e = this.bigWinShowMap.entrySet().stream().filter(en -> times >= en.getValue()[0] && times < en.getValue()[1]).findFirst().orElse(null);
        return e == null ? 0 : e.getKey();
    }
}
