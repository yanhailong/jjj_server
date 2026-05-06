package com.jjg.game.ploy.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ExitType;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.ploy.dao.PlayerPloyGameDataDao;
import com.jjg.game.ploy.dao.PloyPoolDao;
import com.jjg.game.ploy.dao.PloyRecordDao;
import com.jjg.game.ploy.data.PlayerPloyGameData;
import com.jjg.game.ploy.data.PloyBetDivideInfo;
import com.jjg.game.ploy.data.PropInfo;
import com.jjg.game.ploy.logger.PloyLogger;
import com.jjg.game.ploy.pb.ReqPloyRecord;
import com.jjg.game.ploy.utils.PropUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PloygameRoomCfg;
import com.jjg.game.sampledata.bean.PoolResultLibCfg;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 策略游戏抽象控制器
 *
 * @author 11
 * @date 2026/3/19
 */
public abstract class AbstractPloyController<T extends PlayerPloyGameData> implements TimerListener<String>, ConfigExcelChangeListener {
    protected final Logger log;

    @Autowired
    protected TimerCenter timerCenter;
    @Autowired
    protected PloyPoolDao poolDao;
    @Autowired
    protected CorePlayerService playerService;
    @Autowired
    protected PlayerPloyGameDataDao gameDataDao;
    @Autowired
    protected PloyRecordDao recordDao;
    @Autowired
    protected PloyLogger logger;

    //roomCfgId -> playerId ->gameData
    protected Map<Integer, Map<Long, T>> gameDataMap = new ConcurrentHashMap<>();

    protected Class<T> playerGameDataClass;

    //PoolResultLibCfg 映射表  modelId -> cfg
    protected Map<Integer, PoolResultLibCfg> poolResultLibCfgMap;
    //PoolResultLibCfg 权重类型  modelId -> PropInfo
    protected Map<Integer, PropInfo> poolResultLibPropMap;

    protected int gameType;
    protected int roomCfgId;

    public AbstractPloyController(Logger log, Class<T> playerGameDataClass) {
        this.log = log;
        this.playerGameDataClass = playerGameDataClass;
    }

    /**
     * 初始化
     */
    public void init(int gameType) {
        this.gameType = gameType;
        this.roomCfgId = gameType * 10 + 4;

        loadConfig();
    }

    protected void loadConfig() {
        loadPloyGameRoomCfg();
        loadPoolResultLibCfg();
    }

    /**
     * 进入游戏
     *
     * @param playerController 玩家数据
     * @return 响应数据
     */
    public AbstractMessage enterGame(PlayerController playerController, int gameType, int roomCfgId) {
        try {
            T playerGameData = createPlayerGameData(playerController, gameType, roomCfgId);
            if (playerGameData == null) {
                log.warn("创建 playerGameData 失败，进入游戏失败 playerId = {},roomCfgId = {}", playerController.playerId(), roomCfgId);
                return buildResEnterGameMessage(Code.FAIL, gameType, roomCfgId, null);
            }

            AbstractResponse res = buildResEnterGameMessage(Code.SUCCESS, gameType, roomCfgId, playerGameData);
            if (res.code == Code.SUCCESS) {
                playerController.setSubScene(this);
            }

            log.info("进入策略游戏返回 playerId = {},gameType = {},roomCfgId = {},res = {}", playerController.playerId(), gameType, roomCfgId, JSON.toJSONString(res));
            return res;
        } catch (Exception e) {
            log.error("", e);
            return buildResEnterGameMessage(Code.EXCEPTION, gameType, roomCfgId, null);
        }
    }

    /**
     * 下注
     *
     * @param playerController
     * @param betValue
     * @return
     */
    public AbstractMessage bet(PlayerController playerController, long betValue, int value) {
        try {
            T playerGameData = getPlayerGameData(playerController.playerId(), this.roomCfgId);
            if (playerGameData == null) {
                log.warn("获取 playerGameData 失败，下注失败 playerId = {},roomCfgId = {}", playerController.playerId(), this.roomCfgId);
                return buildResBetMessage(Code.FAIL, null, 0, 0);
            }

            //检查押分值
            PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(this.roomCfgId);
            boolean match = cfg.getLineBetScore().stream().anyMatch(b -> b == betValue);
            if (!match) {
                log.warn("下注额错误，下注失败 playerId = {},roomCfgId = {},betValue = {}", playerController.playerId(), playerGameData.getRoomCfgId(), betValue);
                return buildResBetMessage(Code.PARAM_ERROR, playerGameData, 0, 0);
            }

            //前置检查
            int beforeCode = beforeMoneyToPoolCheck(playerGameData);
            if (beforeCode != Code.SUCCESS) {
                log.warn("扣钱前置检查失败，下注失败 playerId = {},roomCfgId = {},betValue = {},code = {}", playerController.playerId(), playerGameData.getRoomCfgId(), betValue, beforeCode);
                return buildResBetMessage(beforeCode, playerGameData, 0, 0);
            }

            //玩家扣除下注金额，并加入标准池
            CommonResult<PloyBetDivideInfo> moneyResult = moneyToPool(playerGameData, betValue);
            if (!moneyResult.success()) {
                return buildResBetMessage(moneyResult.code, playerGameData, 0, 0);
            }
            playerGameData.setLastBet(betValue);
            playerGameData.setLastBetTime(playerGameData.getLastActiveTime());
            playerGameData.setPloyBetDivideInfo(moneyResult.data);

            //构建返回消息
            AbstractResponse res = buildResBetMessage(Code.SUCCESS, playerGameData, betValue, value);
            if (res.code != Code.SUCCESS) {
                poolToPlayer(playerGameData, moneyResult.data.getPoolChangeValue(), betValue, AddType.FAIL_ROLLBACK);
                return res;
            }
            //更新数据
            playerGameData.setLastActiveTime(System.currentTimeMillis());
            log.info("策略游戏下注返回 playerId = {},gameType = {},res = {}", playerController.playerId(), playerGameData.getGameType(), JSON.toJSONString(res));
            return res;
        } catch (Exception e) {
            log.error("", e);
            return buildResBetMessage(Code.EXCEPTION, null, 0, 0);
        }
    }

    /**
     * 退出游戏
     *
     * @param player
     * @return
     */
    public int exitGame(Player player, ExitType exitType) {
        try {
            T playerGameData = removePlayerGameData(player.getId(), roomCfgId);
            if (playerGameData == null) {
                return Code.SUCCESS;
            }
            gameDataDao.saveGameData(playerGameData);
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("", e);
            return Code.EXCEPTION;
        }
    }

    /**
     * 在玩家扣钱之前检查
     *
     * @return
     */
    public int beforeMoneyToPoolCheck(T playerGameData) {
        return Code.SUCCESS;
    }

    /**
     * 请求记录
     *
     * @param playerController 玩家数据
     * @param req              请求
     * @return 响应数据
     */
    public abstract AbstractMessage reqPloyRecord(PlayerController playerController, ReqPloyRecord req);

    /**
     * 构建玩家进入游戏时的返回消息
     *
     * @param code
     * @param playerGameData
     * @return
     */
    protected abstract AbstractResponse buildResEnterGameMessage(int code, int gameType, int roomCfgId, T playerGameData);

    /**
     * 构建玩家下注后的返回消息
     *
     * @param code
     * @param playerGameData
     * @param betValue
     * @param value
     * @return
     */
    protected abstract AbstractResponse buildResBetMessage(int code, T playerGameData, long betValue, int value);

    /**
     * 玩家扣除下注金额，并加入标准池
     *
     * @param playerGameData
     * @param betValue
     */
    protected CommonResult<PloyBetDivideInfo> moneyToPool(T playerGameData, long betValue) {
        CommonResult<PloyBetDivideInfo> result = new CommonResult<>(Code.SUCCESS);

        CommonResult<Pair<Player, Long>> deductResult = playerService.betDeductGold(playerGameData.playerId(), betValue, AddType.PLOY_BET, true, false, String.valueOf(playerGameData.getRoomCfgId()));
        if (!deductResult.success()) {
            log.warn("把钱添加到池子失败,扣除玩家金额失败 playerId = {},betValue = {},code = {}", playerGameData.playerId(), betValue, deductResult.code);
            result.code = deductResult.code;
            return result;
        }

        PloyBetDivideInfo info = new PloyBetDivideInfo();

        BigDecimal bet = BigDecimal.valueOf(betValue);
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        //给标准池子加钱
        BigDecimal toBigPoolProp = BigDecimal.valueOf(cfg.getInitBasePoolProportion()).divide(GameConstant.TEN_THOUSAND_BD, 4, RoundingMode.HALF_UP);
        long toBigPoolGold = bet.multiply(toBigPoolProp).setScale(0, RoundingMode.HALF_UP).longValue();
        if (toBigPoolGold > 0) {
            long poolCoin = poolDao.add(playerGameData.getGameType(), playerGameData.getRoomCfgId(), toBigPoolGold);
            info.setPoolAfterValue(poolCoin);
            log.info("给标准池加钱成功 playerId = {},gameType = {},roomCfgId = {},add = {},afterGold = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), toBigPoolGold, poolCoin);
        }

        long tax = betValue - toBigPoolGold;
        if (tax < 1) {
            log.warn("tax 小于1， gameType = {},roomCfgId = {},betValue = {},toBigPoolGold = {}", playerGameData.getGameType(), playerGameData.getRoomCfgId(), betValue, toBigPoolGold);
        }

        info.setPoolChangeValue(toBigPoolGold);
        info.setToBigPool(toBigPoolGold);
        info.setTax(tax);
        info.setPlayerBeforeMoney(deductResult.data.getSecond());
        info.setPlayerAfterMoney(deductResult.data.getFirst().getGold());
        result.data = info;
        return result;
    }

    /**
     * 玩家中奖，加钱
     *
     * @param playerGameData
     * @param value
     * @return
     */
    protected CommonResult<Pair<PloyBetDivideInfo, Player>> winFromPool(T playerGameData, long value, int tax) {
        if (value < 1) {
            log.warn("从奖池扣除的钱小于1 playerId = {}", playerGameData.playerId());
            return new CommonResult<>(Code.FAIL);
        }
        long addToPlayer = value;
        if (tax > 0) {
            //计算出给玩家加的钱
            int addRate = GameConstant.TEN_THOUSAND - tax;
            if (addRate > 0) {
                //扣税
                addToPlayer = BigDecimal.valueOf(addRate).multiply(BigDecimal.valueOf(value)).setScale(0, RoundingMode.FLOOR)
                        .divide(GameConstant.TEN_THOUSAND_BD, RoundingMode.DOWN).longValue();
            }
        }
        CommonResult<Pair<Player, Long>> playerCommonResult = poolToPlayer(playerGameData, value, addToPlayer, AddType.PLOY_REWARD);
        if (!playerCommonResult.success()) {
            return new CommonResult<>(playerCommonResult.code);
        }

        PloyBetDivideInfo info = new PloyBetDivideInfo();
        info.setTax(value - addToPlayer);
        info.setPoolChangeValue(value);
        info.setPoolAfterValue(playerCommonResult.data.getSecond());
        return new CommonResult<>(Code.SUCCESS, new Pair<>(info, playerCommonResult.data.getFirst()));
    }

    /**
     * 从奖池扣钱，并添加到玩家身上
     *
     * @param playerGameData
     * @param poolChangeValue  从奖池扣除多少
     * @param addToPlayerValue 给玩家添加多少
     */
    protected CommonResult<Pair<Player, Long>> poolToPlayer(T playerGameData, long poolChangeValue, long addToPlayerValue, AddType addType) {
        Long afterPool = poolDao.reduce(playerGameData.getGameType(), playerGameData.getRoomCfgId(), -Math.abs(poolChangeValue));
        if (afterPool == null) {
            log.warn("从奖池扣除失败 playerId = {},poolChangeValue = {},addToPlayerValue = {},addType = {}", playerGameData.playerId(), poolChangeValue, addToPlayerValue, addType);
            return new CommonResult<>(Code.FAIL);
        }

        CommonResult<Player> addResult = playerService.addGold(playerGameData.playerId(), Math.abs(addToPlayerValue), addType, String.valueOf(playerGameData.getGameType()));
        if (!addResult.success()) {
            poolDao.add(playerGameData.getGameType(), playerGameData.getRoomCfgId(), Math.abs(poolChangeValue));
            return new CommonResult<>(addResult.code);
        }

        CommonResult<Pair<Player, Long>> result = new CommonResult<>(Code.SUCCESS);
        result.data = new Pair<>(addResult.data, afterPool);
        log.info("从奖池扣除，并且给玩家加钱成功 playerId = {},poolChangeValue = {},addToPlayerValue = {},addType = {}", playerGameData.playerId(), poolChangeValue, addToPlayerValue, addType);
        return result;
    }

    /**
     * 创建玩家玩游戏的数据存储对象
     *
     * @param playerController
     * @return
     */
    protected T createPlayerGameData(PlayerController playerController, int gameType, int roomCfgId) throws Exception {
        //1.从内存获取
        T playerGameData = getPlayerGameData(playerController.playerId(), roomCfgId);
        if (playerGameData != null) {
            playerGameData.setPlayerController(playerController);
            return playerGameData;
        }

        //2.从数据库获取
        playerGameData = gameDataDao.findOne(playerController.playerId(), roomCfgId, this.playerGameDataClass);
        if (playerGameData == null) {
            Constructor<T> constructor = this.playerGameDataClass.getConstructor();
            playerGameData = constructor.newInstance();
            playerGameData.setId(PlayerPloyGameData.buildId(playerController.playerId(), roomCfgId));
            playerGameData.setPlayerController(playerController);
            playerGameData.setGameType(gameType);
            playerGameData.setRoomCfgId(roomCfgId);
        }

        playerGameData.setPlayerController(playerController);
        this.gameDataMap.computeIfAbsent(roomCfgId, k -> new ConcurrentHashMap<>()).put(playerController.playerId(), playerGameData);
        return playerGameData;
    }

    public T getPlayerGameData(long playerId, int roomCfgId) {
        Map<Long, T> temMap = this.gameDataMap.get(roomCfgId);
        if (temMap == null || temMap.isEmpty()) {
            return null;
        }
        return temMap.get(playerId);
    }

    public T removePlayerGameData(long playerId, int roomCfgId) {
        Map<Long, T> temMap = this.gameDataMap.get(roomCfgId);
        if (temMap == null || temMap.isEmpty()) {
            return null;
        }
        return temMap.remove(playerId);
    }

    /**
     * 根据水池偏差值获取 结果库配置
     *
     * @param diff
     * @return
     */
    protected PoolResultLibCfg getLibCfgByPoolDiff(long diff) {
        for (Map.Entry<Integer, PoolResultLibCfg> en : this.poolResultLibCfgMap.entrySet()) {
            PoolResultLibCfg cfg = en.getValue();
            if ((diff >= cfg.getEnterLimitMin() || cfg.getEnterLimitMin() <= -999999) && (diff < cfg.getEnterLimitMax() || cfg.getEnterLimitMax() >= 999999)) {
                return cfg;
            }
        }
        return null;
    }

    //----------------------------------------------------------------------------------------

    @Override
    public void initSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(PoolResultLibCfg.EXCEL_NAME, this::loadPloyGameRoomCfg);
        addChangeSampleFileObserveWithCallBack(PoolResultLibCfg.EXCEL_NAME, this::loadPoolResultLibCfg);
    }

    protected void loadPloyGameRoomCfg() {
    }

    protected void loadPoolResultLibCfg() {
        Map<Integer, PoolResultLibCfg> tmpPoolResultLibCfgMap = new HashMap<>();
        Map<Integer, PropInfo> poolResultLibPropMap = new HashMap<>();

        for (Map.Entry<Integer, PoolResultLibCfg> en : GameDataManager.getPoolResultLibCfgMap().entrySet()) {
            PoolResultLibCfg cfg = en.getValue();
            if (this.gameType != cfg.getGameType()) {
                continue;
            }
            tmpPoolResultLibCfgMap.put(cfg.getModelId(), cfg);
            poolResultLibPropMap.put(cfg.getModelId(), PropUtils.converMapToPropInfo(cfg.getTypeProp()));
        }

        this.poolResultLibCfgMap = tmpPoolResultLibCfgMap;
        this.poolResultLibPropMap = poolResultLibPropMap;
    }


    @Override
    public void onTimer(TimerEvent<String> e) {

    }

    public void shutdown() {
        if (CollectionUtil.isEmpty(gameDataMap)) {
            return;
        }
        //保存玩家数据
        for (Map.Entry<Integer, Map<Long, T>> entry : gameDataMap.entrySet()) {
            if (CollectionUtil.isEmpty(entry.getValue())) {
                continue;
            }
            for (Map.Entry<Long, T> playerGameDataEntry : entry.getValue().entrySet()) {
                log.info("关服保存策略游戏玩家数据 playerId:{}", playerGameDataEntry.getKey());
                gameDataDao.saveGameData(playerGameDataEntry.getValue());
                log.info("关服保存策略游戏玩家数据完成 playerId:{}", playerGameDataEntry.getKey());
            }
        }
    }


}
