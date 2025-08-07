package com.jjg.game.slots.game.dollarexpress.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.data.GirdUpdateConfig;
import com.jjg.game.slots.data.PropInfo;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.DollarExpressLogger;
import com.jjg.game.slots.game.dollarexpress.dao.DollarExpressGameDataDao;
import com.jjg.game.slots.game.dollarexpress.data.*;
import com.jjg.game.slots.game.dollarexpress.dao.DollarExpressResultLibDao;
import com.jjg.game.slots.game.dollarexpress.pb.DollarsInfo;
import com.jjg.game.slots.game.dollarexpress.pb.ResultLineInfo;
import com.jjg.game.slots.game.dollarexpress.pb.TrainInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import com.jjg.game.slots.sample.GameDataManager;
import com.jjg.game.slots.sample.bean.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 美元快递游戏逻辑处理器
 *
 * @author 11
 * @date 2025/6/11 16:48
 */
@Component
public class DollarExpressGameManager extends AbstractSlotsGameManager<DollarExpressPlayerGameData> {

    @Autowired
    private DollarExpressResultLibDao libDao;
    @Autowired
    private DollarExpressGenerateManager generateManager;
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private DollarExpressLogger logger;
    @Autowired
    private DollarExpressGameDataDao gameDataDao;

    private BigDecimal hundred = BigDecimal.valueOf(100);

    private Map<Integer, GirdUpdateConfig> girdUpdateConfigMap;
    //在替换格子时限制while最大循环次数
    private final int updateGirdWhildMaxCount = 30;

    //倍率放大倍数
    private int timesScale = 100;
    private BigDecimal timesScaleBigDecimal = BigDecimal.valueOf(timesScale);


    public DollarExpressGameManager() {
        super(DollarExpressPlayerGameData.class);
    }

    @Override
    public void init() {
        log.info("启动美元快递游戏管理器...");
        this.gameType = CoreConst.GameType.DOLLAR_EXPRESS;
        this.libDao.init(this.gameType);

        this.generateManager.init(this.gameType);
        //计算配置后缓存
        initConfig();
    }

    /**
     * 开始游戏
     *
     * @param playerController
     * @param betValue
     * @return
     */
    public DollarExpressGameRunInfo playerStartGame(PlayerController playerController, long betValue) {
        //获取玩家游戏数据
        DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new DollarExpressGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerGameData, betValue, true);
    }

    /**
     * 自动玩游戏
     *
     * @param betValue
     * @return
     */
    public DollarExpressGameRunInfo autoStartGame(DollarExpressPlayerGameData playerGameData, long betValue) {
        log.debug("系统开始自动玩游戏 playerId = {}", playerGameData.playerId());

        return startGame(playerGameData, betValue, true);
    }

    /**
     * 开始游戏
     *
     * @param betValue
     * @return
     */
    public DollarExpressGameRunInfo startGame(DollarExpressPlayerGameData playerGameData, long betValue, boolean updateGird) {
        DollarExpressGameRunInfo gameRunInfo = new DollarExpressGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {

            boolean allAreaUnlock = playerGameData.getAllUnLock().compareAndSet(true, false);
            if (allAreaUnlock) {
                gameRunInfo = areaAllUnlockGoldTrain(gameRunInfo, playerGameData, updateGird);
            } else {
                //获取当前处于哪种状态
                int status = playerGameData.getStatus();
                if (status == DollarExpressConstant.Status.NORMAL) {  //正常
                    gameRunInfo = normal(gameRunInfo, playerGameData, betValue, updateGird);
                } else if (status == DollarExpressConstant.Status.NOTMAL_ALL_BOARD || status == DollarExpressConstant.Status.GOLD_ALL_BOARD) {  //二选一
                    gameRunInfo.setCode(Code.FORBID);
                    log.debug("当前正处于二选一状态，禁止开始游戏操作 playerId = {},gameType = {},roomCfgId = {}, status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                    return gameRunInfo;
                } else if (status == DollarExpressConstant.Status.ALL_BOARD_TRAIN) {  //二选一之拉火车
                    gameRunInfo = allBoardTrain(gameRunInfo, playerGameData);
                } else if (status == DollarExpressConstant.Status.ALL_BOARD_GOLD_TRAIN) {  //二选一之拉黄金火车
                    gameRunInfo = allBoardGoldTrain(gameRunInfo, playerGameData, updateGird);
                } else if (status == DollarExpressConstant.Status.ALL_BOARD_FREE) {  //二选一之免费模式
                    gameRunInfo = allBoardFree(gameRunInfo, playerGameData);
                } else {
                    gameRunInfo.setCode(Code.FAIL);
                    log.debug("开始游戏失败，检测到错误状态 playerId = {},gameType = {},roomCfgId = {},status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                    return gameRunInfo;
                }
            }

            //检查是否触发投资游戏
            gameRunInfo = checkInvers(playerGameData, gameRunInfo);

            //标准池
            if (gameRunInfo.getBigPoolTimes() > 0) {
                long addGold = calWinGold(playerGameData.getLastBet(),gameRunInfo.getBigPoolTimes());
                if (addGold > 0) {
                    CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), addGold, "SLOTS_BET_REWARD");
                    if (!result.success()) {
                        log.warn("给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                        gameRunInfo.setCode(result.code);
                        return gameRunInfo;
                    }
                    gameRunInfo.setAllWinGold(addGold);
                }
            }

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //添加美元收集进度
            if (gameRunInfo.getTotalDollars() < 1) {
                gameRunInfo.setTotalDollars(playerGameData.getTotalDollars());
            }

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            gameRunInfo.setAfterGold(player.getGold());

            //添加大奖展示id
            int times = (int) (gameRunInfo.getAllWinGold() / playerGameData.getLastBet());
            log.debug("计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            //发送日志
            logger.gameResult(player, gameRunInfo);
            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    /**
     * 玩家二选一
     *
     * @param playerController
     * @param chooseStatus
     * @return
     */
    public DollarExpressGameRunInfo playerChooseFreeGameType(PlayerController playerController, int chooseStatus) {
        DollarExpressGameRunInfo gameRunInfo = new DollarExpressGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            //获取玩家游戏数据
            DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerController);
            if (playerGameData == null) {
                log.debug("获取玩家游戏数据失败，二选一失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            //设置活跃时间
            playerGameData.setLastActiveTime(TimeHelper.nowInt());

            int code = chooseFreeGameType(playerGameData, chooseStatus);
            if (code != Code.SUCCESS) {
                gameRunInfo.setCode(code);
                return gameRunInfo;
            }
            log.info("玩家进行二选一，playerId = {},gameType = {},roomCfgId = {},chooseStatus = {}", playerController.playerId(), playerGameData.getGameType(), playerController.getPlayer().getRoomCfgId(), chooseStatus);
            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    /**
     * 投资游戏选择地区
     *
     * @param playerController
     */
    public DollarExpressGameRunInfo invest(PlayerController playerController, int areaId) {
        DollarExpressGameRunInfo gameRunInfo = new DollarExpressGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            if (areaId < 1 || areaId > 8) {
                log.debug("区域id参数错误，投资游戏失败 playerId = {},gameType = {},roomCfgId = {},areaId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), areaId);
                gameRunInfo.setCode(Code.PARAM_ERROR);
                return gameRunInfo;
            }

            DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerController);
            if (playerGameData == null) {
                log.debug("获取玩家游戏数据失败，投资游戏失败 playerId = {},gameType = {},roomCfgId = {},areaId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), areaId);
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            //设置活跃时间
            playerGameData.setLastActiveTime(TimeHelper.nowInt());

            //检查是否被选择
            boolean select = playerGameData.areaSelected(areaId);
            if (select) {
                log.debug("该地区已被选择 playerId = {},gameType = {},roomCfgId = {},areaId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), areaId);
                gameRunInfo.setCode(Code.FORBID);
                return gameRunInfo;
            }

            boolean flag = playerGameData.getInvers().compareAndSet(true, false);
            if (!flag) {
                log.debug("当前不处于投资游戏 playerId = {},gameType = {},roomCfgId = {},areaId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), areaId);
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }
            playerGameData.addSelectedArea(areaId);

            List<Integer> rewardIdList = generateManager.getRewardList(generateManager.getPropAndAwardInfo(generateManager.getDollarExpressCollectDollarConfig().getAuxiliaryId()), playerGameData.getLastModelId());
            //处理奖励逻辑
            InversGoldTrainRewardData data = generateManager.handInversGoldTrainReward(rewardIdList);

            //3个小地图奖励的金币
            boolean goldTrain = true;
            List<Long> rewardGoldList = new ArrayList<>();
            long allAddGold = 0;

            long avg = playerGameData.getAddDollarsTotalStake() / playerGameData.getAddDollarsCount();
            for (int times : data.getRewardsTimes()) {
                long gold = avg * times;
                rewardGoldList.add(gold);
                allAddGold += gold;
                if (goldTrain && times < 1) {
                    goldTrain = false;
                }
            }

            Player player = null;
            //3次中奖金币
            if (allAddGold > 0) {
                CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), allAddGold, "SLOTS_INVEST_REWARD");
                if (!result.success()) {
                    gameRunInfo.setCode(result.code);
                    return gameRunInfo;
                }
                gameRunInfo.addAllWinGold(allAddGold);
                gameRunInfo.setInvestRewardGoldList(rewardGoldList);
                player = result.data;
                log.debug("投资游戏玩家添加金币 gameType = {},roomCfgId = {},addGold = {}", this.gameType, playerGameData.getRoomCfgId(), allAddGold);
            }

            //3次全部中奖的金火车
            if (goldTrain && data.getGoldTrain() > 0) {
                long addGold = allAddGold * data.getGoldTrain();
                CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), addGold, "SLOTS_INVEST_REWARD");
                if (!result.success()) {
                    log.warn("投资游戏金火车给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                    gameRunInfo.setCode(result.code);
                    return gameRunInfo;
                }
                gameRunInfo.addAllWinGold(addGold);

                gameRunInfo.setInvestRewardGoldTrainCount(data.getGoldTrain());
                gameRunInfo.setInvestRewardGold(allAddGold);
                player = result.data;
                log.debug("小地图3次都中奖，添加金火车的金币 gameType = {},roomCfgId = {},addGold = {}", this.gameType, playerGameData.getRoomCfgId(), addGold);
            }

            //检查地图是否全部解锁
            if (playerGameData.areaAllUnlock()) {
                gameRunInfo.setAllAreaUnLock(true);
                playerGameData.getAllUnLock().compareAndSet(false, true);
            }

            if (player == null) {
                player = slotsPlayerService.get(playerController.playerId());
            }
            playerController.setPlayer(player);
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    /**
     * 获取奖池
     * @param playerController
     */
    public DollarExpressGameRunInfo getPoolValue(PlayerController playerController,long stake) {
        DollarExpressGameRunInfo gameRunInfo = new DollarExpressGameRunInfo(Code.SUCCESS, playerController.playerId());
        try{
            DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerController);
            if (playerGameData != null) {
                //设置活跃时间
                playerGameData.setLastActiveTime(TimeHelper.nowInt());
            }

            gameRunInfo.setMini(getPoolValueByPoolId(DollarExpressConstant.Common.MINI_POOL_ID,stake));
            gameRunInfo.setMinor(getPoolValueByPoolId(DollarExpressConstant.Common.MINOR_POOL_ID,stake));
            gameRunInfo.setMajor(getPoolValueByPoolId(DollarExpressConstant.Common.MAJOR_POOL_ID,stake));
            gameRunInfo.setGrand(getPoolValueByPoolId(DollarExpressConstant.Common.GRAND_POOL_ID,stake));
        }catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    public long getPoolValueByPoolId(int poolId,long stake) throws Exception {
        PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
        return calTrainPoolValue(stake,poolCfg.getGrowthRate(),poolCfg.getFakePoolInitTimes(),poolCfg.getFakePoolMax());
    }

    /**
     * 系统自动二选一
     *
     * @param playerGameData
     */
    public void autoChooseFreeModelType(DollarExpressPlayerGameData playerGameData) {
        try {
            int chooseStatus;
            if (playerGameData.getStatus() == DollarExpressConstant.Status.NOTMAL_ALL_BOARD) {
                chooseStatus = RandomUtils.randomInt(2) == 0 ? DollarExpressConstant.Status.ALL_BOARD_TRAIN : DollarExpressConstant.Status.ALL_BOARD_FREE;
            } else {
                chooseStatus = RandomUtils.randomInt(2) == 0 ? DollarExpressConstant.Status.ALL_BOARD_GOLD_TRAIN : DollarExpressConstant.Status.ALL_BOARD_FREE;
            }

            int code = chooseFreeGameType(playerGameData, chooseStatus);
            if (code != Code.SUCCESS) {
                log.debug("系统自动二选一失败 playerId = {},chooseStatus = {}", playerGameData.playerId(), chooseStatus);
                return;
            }
            log.info("系统自动进行二选一 playerId = {},chooseStatus = {}", playerGameData.playerId(), chooseStatus);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 二选一选哪个？？？
     *
     * @param chooseStatus
     * @return
     */
    private int chooseFreeGameType(DollarExpressPlayerGameData playerGameData, int chooseStatus) {
        if (playerGameData.getStatus() == DollarExpressConstant.Status.NOTMAL_ALL_BOARD) {  //普通二选一
            if (chooseStatus == DollarExpressConstant.Status.ALL_BOARD_TRAIN) {  //拉普通火车
                playerGameData.setStatus(DollarExpressConstant.Status.ALL_BOARD_TRAIN);
            } else {
                playerGameData.setStatus(DollarExpressConstant.Status.ALL_BOARD_FREE);
                playerGameData.getRemainFreeCount().set(8);
            }
        } else if (playerGameData.getStatus() == DollarExpressConstant.Status.GOLD_ALL_BOARD) {  //黄金二选一
            if (chooseStatus == DollarExpressConstant.Status.ALL_BOARD_GOLD_TRAIN) {  //拉黄金火车
                playerGameData.setStatus(DollarExpressConstant.Status.ALL_BOARD_GOLD_TRAIN);
            } else {
                playerGameData.setStatus(DollarExpressConstant.Status.ALL_BOARD_FREE);
                playerGameData.getRemainFreeCount().set(8);
            }
        } else {
            log.debug("当前不处于二选一状态，禁止二选一操作 playerId = {},gameType = {},roomCfgId = {},status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), playerGameData.getStatus());
            return Code.FORBID;
        }
        return Code.SUCCESS;
    }

    /**
     * 普通正常流程
     *
     * @param gameRunInfo
     * @param playerGameData
     * @param betValue
     * @return
     */
    private DollarExpressGameRunInfo normal(DollarExpressGameRunInfo gameRunInfo, DollarExpressPlayerGameData playerGameData, long betValue, boolean updateGird) {
        log.debug("开始正常流程 playerId = {},betValue = {},updateGird = {}", playerGameData.playerId(), betValue, updateGird);
        //获取倍场配置
        BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(playerGameData.getRoomCfgId());
        if (baseRoomCfg == null) {
            log.warn("获取倍场配置失败 playerId = {},gameType = {},roomCfgId = {},betValue = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), betValue);
            gameRunInfo.setCode(Code.NOT_FOUND);
            return gameRunInfo;
        }

        //检查押分是否合法
        boolean match = this.allStakeMap.get(playerGameData.getRoomCfgId()).stream().anyMatch(bet -> bet == betValue);
        if (!match) {
            log.warn("押分值不合法 playerId = {},gameType = {},roomCfgId = {},betValue = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), betValue);
            gameRunInfo.setCode(Code.PARAM_ERROR);
            return gameRunInfo;
        }

        Player player = slotsPlayerService.get(playerGameData.playerId());
        if (player.getGold() < betValue) {
            log.debug("玩家余额不足，无法快乐的玩游戏 playerId = {},gameType = {},roomCfgId = {},betValue = {},currentGold = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), betValue, player.getGold());
            gameRunInfo.setCode(Code.NOT_ENOUGH);
            return gameRunInfo;
        }

        CommonResult<SpecialResultLibCfg> libCfgResult = getLibCfg(playerGameData, baseRoomCfg.getInitBasePool());
        if (!libCfgResult.success()) {
            gameRunInfo.setCode(libCfgResult.code);
            return gameRunInfo;
        }

        //获取 specialResultLib 中的type
        CommonResult<Integer> resultLibTypeResult = getResultLibType(playerGameData.getGameType(), libCfgResult.data.getModelId());
        if (!resultLibTypeResult.success()) {
            gameRunInfo.setCode(libCfgResult.code);
            return gameRunInfo;
        }
        log.debug("获取到结果库类型 playerId = {},libType = {}", playerGameData.playerId(), resultLibTypeResult.data);

        int sectionIndex = -1;
        DollarExpressResultLib resultLib = null;

        //先去获取测试数据
        TestLibData testLibData = playerGameData.pollTestLibData();
        if (testLibData != null) {
            resultLib = testLibData.getResultLib();
            updateGird = testLibData.isUpdateGird();
            log.debug("获取到测试数据 playerId = {},updateGird = {},libId = {}", playerGameData.playerId(), updateGird, resultLib.getId());
        }

        if (resultLib == null) {
            for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
                //获取倍数区间
                CommonResult<Integer> resultLibSectionResult = getResultLibSection(libCfgResult.data.getModelId(), resultLibTypeResult.data);
                if (!resultLibSectionResult.success()) {
                    continue;
                }

                //根据倍数区间从结果库里面随机获取一条
                resultLib = libDao.getLibBySectionIndex(libCfgResult.data.getModelId(), resultLibTypeResult.data, resultLibSectionResult.data);
                if (resultLib == null) {
                    log.debug("获取结果库失败 gameType = {},modelId = {},libType = {},sectionIndex = {},retry = {}", this.gameType, libCfgResult.data.getModelId(), resultLibTypeResult.data, resultLibSectionResult.data, i);
                    continue;
                }
                sectionIndex = resultLibSectionResult.data;
                log.debug("成功获取结果库  playerId = {},libId = {}", playerGameData.playerId(), resultLib.getId());
                break;
            }
        }

        //如果前面没有获取到lib，则获取一个无奖励的结果
        if (resultLib == null) {
            sectionIndex = this.defaultRewardSectionIndex;
            resultLib = libDao.getLibBySectionIndex(libCfgResult.data.getModelId(), SlotsConst.SpecialResultLib.TYPE_NORMAL, this.defaultRewardSectionIndex);
            log.debug("前面获取结果库失败，所以找一个不中奖的结果返回 gameType = {},libType = {}", this.gameType, resultLibTypeResult.data);
        }

        if (resultLib == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("获取结果库失败 gameType = {},libType = {}", this.gameType, resultLibTypeResult.data);
            return gameRunInfo;
        }

        //给池子加钱
        CommonResult<Player> result = goldToPool(playerGameData, betValue, baseRoomCfg);
        if (!result.success()) {
            gameRunInfo.setCode(result.code);
            return gameRunInfo;
        }

//        long bet = BigDecimal.valueOf(betValue).divide(hundred, 0, BigDecimal.ROUND_HALF_UP).longValue();
        playerGameData.setLastStake(betValue);
        playerGameData.setLastBet(betValue);
        gameRunInfo.setBet(betValue);

        gameRunInfo.setIconArr(resultLib.getIconArr());

        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(SlotsConst.SpecialResultLib.TYPE_ALL_BOARD)) {  //是否会触发二选一
            int count = generateManager.checkAllBoadrd(resultLib.getIconArr());
            if (count > 3) {
                playerGameData.setStatus(DollarExpressConstant.Status.GOLD_ALL_BOARD);
            } else {
                playerGameData.setStatus(DollarExpressConstant.Status.NOTMAL_ALL_BOARD);
            }
            log.debug("触发二选一  playerId = {},libId = {},status = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus());
        }

        if (sectionIndex > 0) {
            playerGameData.setLastSectionIndex(sectionIndex);
        }
        playerGameData.setLastModelId(libCfgResult.data.getModelId());

        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));

        //格子修改
        CommonResult<int[]> updateResult = updateGird(libCfgResult.data.getModelId(), SlotsConst.BaseElementReward.ROTATESTATE_NORMAL, resultLibTypeResult.data, gameRunInfo.getIconArr(), resultLib.getAwardLineInfoList(), updateGird);
        if (updateResult.success()) {
            gameRunInfo.setIconArr(updateResult.data);
        }
        //检查与美元相关的逻辑
        gameRunInfo = checkDorllar(gameRunInfo, playerGameData, resultLib.getGoldTrainCount(), resultLib.getGoldTrainAllTimes());

        //计算火车奖励
        gameRunInfo = calTrainReward(playerGameData, resultLib.getTrainList(), gameRunInfo);
        //添加火车信息
//        gameRunInfo = transTrainPbInfo(gameRunInfo, resultLib.getTrainList(), gameRunInfo.getBet());

        //添加中奖线信息
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(resultLib.getAwardLineInfoList(), gameRunInfo.getBet()));

        gameRunInfo.addBigPoolTimes(resultLib.getTimes());

        gameRunInfo.setStatus(playerGameData.getStatus());
        return gameRunInfo;
    }

    /**
     * 返回免费模式选择火车
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    private DollarExpressGameRunInfo allBoardTrain(DollarExpressGameRunInfo gameRunInfo, DollarExpressPlayerGameData playerGameData) {
        log.debug("进入二选一之拉火车流程 playerId = {}", playerGameData.playerId());
        DollarExpressResultLib trainLib = null;
        for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
            //获取一个倍数区间
            CommonResult<Integer> result = getResultLibSection(playerGameData.getLastModelId(), SlotsConst.SpecialResultLib.TYPE_AGAIN_TRAIN);
            if (!result.success()) {
                continue;
            }
            //获取结果库
            trainLib = libDao.getLibBySectionIndex(playerGameData.getLastModelId(), SlotsConst.SpecialResultLib.TYPE_AGAIN_TRAIN, result.data);
            if (trainLib == null) {
                continue;
            }
            break;
        }

        if (trainLib == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("未在该条结果库中找到重转信息1 gameType = {},modelId = {}", this.gameType, playerGameData.getLastModelId());
            return gameRunInfo;
        }

        log.debug("获取到拉火车的结果库 playerId = {},libId = {}", playerGameData.playerId(), trainLib.getId());

        gameRunInfo.setBet(playerGameData.getLastBet());
        gameRunInfo.setStatus(playerGameData.getStatus());

        playerGameData.setStatus(DollarExpressConstant.Status.NORMAL);

        gameRunInfo.setIconArr(trainLib.getIconArr());

        //计算火车奖励
        gameRunInfo = calTrainReward(playerGameData, trainLib.getTrainList(), gameRunInfo);
        //添加火车信息
//        gameRunInfo = transTrainPbInfo(gameRunInfo, trainLib.getTrainList(), gameRunInfo.getBet());

        gameRunInfo.setBigPoolTimes(trainLib.getTimes());

        log.debug("libId = {},train = {}", trainLib.getId(), JSON.toJSONString(trainLib));
        return gameRunInfo;

    }

    /**
     * 返回免费模式选择黄金火车
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    private DollarExpressGameRunInfo allBoardGoldTrain(DollarExpressGameRunInfo gameRunInfo, DollarExpressPlayerGameData playerGameData, boolean updateGird) {
        log.debug("进入二选一之拉黄金火车流程 playerId = {}", playerGameData.playerId());
        DollarExpressResultLib goldTrainLib = null;
        for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
            //获取一个倍数区间
            CommonResult<Integer> result = getResultLibSection(playerGameData.getLastModelId(), SlotsConst.SpecialResultLib.TYPE_AGAIN_GOLD_TRAIN);
            if (!result.success()) {
                continue;
            }
            //获取结果库
            goldTrainLib = libDao.getLibBySectionIndex(playerGameData.getLastModelId(), SlotsConst.SpecialResultLib.TYPE_AGAIN_GOLD_TRAIN, result.data);
            if (goldTrainLib == null) {
                continue;
            }
            break;
        }

        if (goldTrainLib == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("未在该条结果库中找到重转信息 gameType = {},modelId = {}", this.gameType, playerGameData.getLastModelId());
            return gameRunInfo;
        }

        log.debug("成功获取黄金列车结果库 playerId = {},libId = {}", playerGameData.playerId(), goldTrainLib.getId());

        gameRunInfo.setStatus(playerGameData.getStatus());

        playerGameData.setStatus(DollarExpressConstant.Status.NORMAL);
        gameRunInfo.setIconArr(goldTrainLib.getIconArr());

        //格子修改
        CommonResult<int[]> updateResult = updateGird(playerGameData.getLastModelId(), SlotsConst.BaseElementReward.ROTATESTATE_AGAIN, SlotsConst.SpecialResultLib.TYPE_AGAIN_GOLD_TRAIN, goldTrainLib.getIconArr(), goldTrainLib.getAwardLineInfoList(), updateGird);
        if (updateResult.success()) {
            gameRunInfo.setIconArr(updateResult.data);
        }
        gameRunInfo = checkDorllar(gameRunInfo, playerGameData, goldTrainLib.getGoldTrainCount(), goldTrainLib.getGoldTrainAllTimes());

        gameRunInfo.addBigPoolTimes(goldTrainLib.getTimes());

        return gameRunInfo;
    }

    /**
     * 返回免费模式结果
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    private DollarExpressGameRunInfo allBoardFree(DollarExpressGameRunInfo gameRunInfo, DollarExpressPlayerGameData playerGameData) {
        DollarExpressResultLib freeLib = null;
        for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
            //获取一个倍数区间
            CommonResult<Integer> result = getResultLibSection(playerGameData.getLastModelId(), SlotsConst.SpecialResultLib.TYPE_ALL_BOARD_FREE);
            if (!result.success()) {
                continue;
            }
            //获取结果库
            freeLib = libDao.getLibBySectionIndex(playerGameData.getLastModelId(), SlotsConst.SpecialResultLib.TYPE_ALL_BOARD_FREE, result.data);
            if (freeLib == null) {
                continue;
            }
            break;
        }

        if (freeLib == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("未在该条结果库中找到免费转信息 gameType = {},modelId = {}", this.gameType, playerGameData.getLastModelId());
            return gameRunInfo;
        }
        Map<Integer, DollarExpressFreeGame> freeGameMap = freeLib.getFreeGameMap();
        if (freeGameMap == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("未在该条结果库中找到免费转信息1 gameType = {},libId = {}", this.gameType, freeLib.getId());
            return gameRunInfo;
        }
//        System.out.println(playerGameData.getLastAgainGameIndex());
        DollarExpressFreeGame freeGame = freeGameMap.get(8 - playerGameData.getRemainFreeCount().get());
        if (freeGame == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("未在该条结果库中找到免费转信息2 gameType = {},libId = {}", this.gameType, freeLib.getId());
            return gameRunInfo;
        }
        gameRunInfo.setStatus(playerGameData.getStatus());

        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);
        if (afterCount < 1) {
            playerGameData.setStatus(DollarExpressConstant.Status.NORMAL);
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setBet(playerGameData.getLastBet());

        //计算火车奖励
        gameRunInfo = calTrainReward(playerGameData, freeGame.getTrainList(), gameRunInfo);
        //添加火车信息
//        gameRunInfo = transTrainPbInfo(gameRunInfo, freeGame.getTrainList(), gameRunInfo.getBet());
        //添加中奖线信息
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(freeGame.getAwardLineInfoList(), gameRunInfo.getBet()));
        gameRunInfo.setBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setRemainFreeCount(afterCount);
        return gameRunInfo;
    }

    /**
     * 地图全部解锁，奖励黄金火车
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    private DollarExpressGameRunInfo areaAllUnlockGoldTrain(DollarExpressGameRunInfo gameRunInfo, DollarExpressPlayerGameData playerGameData, boolean updateGird) {
        log.debug("进入地图全部解锁，奖励黄金火车流程 playerId = {}", playerGameData.playerId());
        DollarExpressResultLib goldTrainLib = null;
        for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
            //获取一个倍数区间
            CommonResult<Integer> result = getResultLibSection(playerGameData.getLastModelId(), SlotsConst.SpecialResultLib.TYPE_AGAIN_GOLD_TRAIN);
            if (!result.success()) {
                continue;
            }
            //获取结果库
            goldTrainLib = libDao.getLibBySectionIndex(playerGameData.getLastModelId(), SlotsConst.SpecialResultLib.TYPE_AGAIN_GOLD_TRAIN, result.data);
            if (goldTrainLib == null) {
                continue;
            }
            break;
        }

        if (goldTrainLib == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("地图全部解锁,未在该条结果库中找到重转信息 gameType = {},modelId = {}", this.gameType, playerGameData.getLastModelId());
            return gameRunInfo;
        }

        playerGameData.setStatus(DollarExpressConstant.Status.NORMAL);
        gameRunInfo.setIconArr(goldTrainLib.getIconArr());

        //格子修改
        CommonResult<int[]> updateResult = updateGird(playerGameData.getLastModelId(), SlotsConst.BaseElementReward.ROTATESTATE_AGAIN, SlotsConst.SpecialResultLib.TYPE_AGAIN_GOLD_TRAIN, goldTrainLib.getIconArr(), goldTrainLib.getAwardLineInfoList(), updateGird);
        if (updateResult.success()) {
            gameRunInfo.setIconArr(updateResult.data);
        }
        gameRunInfo = checkDorllar(gameRunInfo, playerGameData, goldTrainLib.getGoldTrainCount(), goldTrainLib.getGoldTrainAllTimes());

        gameRunInfo.setBigPoolTimes(goldTrainLib.getTimes());
        playerGameData.setSelectedAreaSet(null);
        return gameRunInfo;
    }


    /**
     * 给池子加钱
     *
     * @param gameData
     * @param betValue
     * @return
     */
    private CommonResult<Player> goldToPool(DollarExpressPlayerGameData gameData, long betValue, BaseRoomCfg baseRoomCfg) {
        CommonResult<Player> result = slotsPlayerService.addGold(gameData.playerId(), -betValue, "SLOTS_BET");
        if (!result.success()) {
            log.debug("把钱添加到池子失败,扣除玩家金额失败 playerId = {},betValue = {},code = {}", gameData.playerId(), betValue, result.code);
            return result;
        }

        BigDecimal bet = BigDecimal.valueOf(betValue);
        log.debug("玩家扣除金币成功 playerId = {},reduceGold = {},afterGold = {}", gameData.playerId(), betValue, result.data.getGold());

        //给标准池子加钱
        BigDecimal toBigPoolProp = BigDecimal.valueOf(baseRoomCfg.getInitBasePoolProportion()).divide(tenThousandBigDecimal, 4, BigDecimal.ROUND_HALF_UP);
        long toBigPoolGold = bet.multiply(toBigPoolProp).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
        if (toBigPoolGold > 0) {
            long poolCoin = slotsPoolDao.addToBigPool(this.gameType, gameData.getRoomCfgId(), toBigPoolGold);
            log.debug("给标准池加钱成功 gameType = {},roomCfgId = {},add = {},afterGold = {}", gameData.getGameType(), gameData.getRoomCfgId(), toBigPoolGold, poolCoin);
        }

        //给小池子加钱
        BigDecimal toSmallPoolProp = BigDecimal.valueOf(baseRoomCfg.getCommissionProp()).divide(tenThousandBigDecimal, 4, BigDecimal.ROUND_HALF_UP);
        long toSmallPoolGold = bet.multiply(toSmallPoolProp).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
        if (toSmallPoolGold > 0) {
            long poolCoin = slotsPoolDao.addToSmallPool(this.gameType, gameData.getRoomCfgId(), toSmallPoolGold);
            gameData.addAllBet(poolCoin);
            long contribtGold = gameData.addContribtPoolGold(poolCoin);
            log.debug("给小池子加钱成功 gameType = {},roomCfgId = {},add = {},afterGold = {},contribtGold={}", gameData.getGameType(), gameData.getRoomCfgId(), toSmallPoolGold, poolCoin, contribtGold);
        }
        return result;
    }


    /**
     * 修改格子
     *
     * @param modelId
     * @param spinStatus
     * @param arr
     * @param update     在测试时从正向执行有时候不需要修改格子，但是从结果库取出的结果必须按照要求修改格子(总之，是为了测试方便)
     */
    public CommonResult<int[]> updateGird(int modelId, int spinStatus, int libType, int[] arr, List<DollarExpressAwardLineInfo> awardLineInfoList, boolean update) {
        CommonResult<int[]> result = new CommonResult<>(Code.FAIL);
        if (this.specialGirdCfgMap == null || this.specialGirdCfgMap.isEmpty()) {
            return result;
        }

        log.debug("开始修改格子 modelId = {},spinStaus = {},libType = {},update = {}", modelId, spinStatus, libType, update);
        int[] newArr = null;
        if (update) {
            //中奖线里面的图标id
            Set<Integer> lineInfoIconSet = null;

            for (Map.Entry<Integer, SpecialGirdCfg> en : this.specialGirdCfgMap.entrySet()) {
                SpecialGirdCfg cfg = en.getValue();
                //模式id
                if (cfg.getModelId() != 0 && cfg.getModelId() != modelId) {
                    continue;
                }
                //旋转状态
                if (cfg.getSpinStatus() != spinStatus) {
                    continue;
                }
                //格式修改算法
                if (!checkUpdateType(cfg.getGirdUpdateType(), libType)) {
                    continue;
                }
                GirdUpdateConfig config = this.girdUpdateConfigMap.get(cfg.getId());
                //随机次数
                int randCount = config.getRandCountPropInfo().getRandKey();
                if (randCount < 1) {
                    continue;
                }

                //添加中奖线里面的图标id
                if (lineInfoIconSet == null) {
                    lineInfoIconSet = new HashSet<>();
                    if (awardLineInfoList != null && !awardLineInfoList.isEmpty()) {
                        for (DollarExpressAwardLineInfo info : awardLineInfoList) {
                            lineInfoIconSet.add(info.getIconId());
                        }
                    }
                }

                //原盘面中剩余的可被替换的图标
                Map<Integer, Integer> canReplaceMap = new HashMap<>();

                //去掉不替换的元素
                for (int i = 1; i < arr.length; i++) {
                    int icon = arr[i];
                    //如果该图标时中奖图标，也不能被替换
                    if (lineInfoIconSet.contains(icon)) {
                        continue;
                    }

                    boolean match = cfg.getNotReplaceEle().stream().anyMatch(e -> icon == e);
                    if (!match) {
                        canReplaceMap.put(i, icon);
                    }
                }

                if (canReplaceMap.isEmpty()) {
                    continue;
                }

                if (newArr == null) {
                    newArr = Arrays.copyOf(arr, arr.length);
                }

                PropInfo cloumnPropInfo = config.getSpecialIconPropInfo().clone();
                int whileCount = 0;

                outWhile:
                while (randCount > 0) {
                    whileCount++;
                    if (whileCount >= this.updateGirdWhildMaxCount) {
                        break;
                    }

                    if (cloumnPropInfo.getSum() < 1) {
                        break;
                    }
                    //先随机一列
                    int columnId = cloumnPropInfo.getRandKey();
                    int index = (columnId - 1) * generateManager.getBaseInitCfg().getRows() + 1;
                    //计算这一列的坐标
                    for (int j = 0; j < generateManager.getBaseInitCfg().getRows(); j++) {
                        if (randCount < 1) {
                            break outWhile;
                        }
                        int tempIndex = index + j;
                        //检查该坐标是否在可被替换中
                        Integer canReplaceIcon = canReplaceMap.get(tempIndex);
                        if (canReplaceIcon == null) {
                            continue;
                        }

                        //可替换
                        newArr[tempIndex] = config.getSpecialIconId();
                        randCount--;
                    }

                    if (randCount > 0) {
                        cloumnPropInfo.removeKeyAndRecalculate(columnId);
                    } else {
                        break outWhile;
                    }
                }
            }
        } else {
            newArr = arr;
        }

        if (newArr != null) {
            result.code = Code.SUCCESS;
            result.data = newArr;
            log.debug("修改格子 modelId = {},spinStatus = {},updateGird = {}", modelId, spinStatus, update);
        }
        return result;
    }

    /**
     * 设置美元倍数等信息
     *
     * @param gameRunInfo
     * @return
     */
    public DollarExpressGameRunInfo checkDorllar(DollarExpressGameRunInfo gameRunInfo, DollarExpressPlayerGameData gameData, int goldTrainCount, int dollarAllTimes) {
        DollarsInfo dollarsInfo = new DollarsInfo();
        dollarsInfo.dollarIndexIds = null;
        dollarsInfo.dollarValueList = null;

        log.debug("设置美元倍数信息 playerId = {},goldTrainCount = {},dollarAllTimes = {},bet = {}", gameData.playerId(), goldTrainCount, dollarAllTimes, gameData.getLastBet());
        //检查美金和保险箱
        int safeBoxIndex = 0;
        int goldTrainIndex = 0;
        for (int i = 1; i < gameRunInfo.getIconArr().length; i++) {
            int icon = gameRunInfo.getIconArr()[i];
            if (icon == DollarExpressConstant.BaseElement.ID_DOLLAR) {
                if (dollarsInfo.dollarIndexIds == null) {
                    dollarsInfo.dollarIndexIds = new ArrayList<>();
                }
                dollarsInfo.dollarIndexIds.add(i);

                if (dollarAllTimes < 1) {
                    int times = generateManager.randDollarTimes();
                    if (dollarsInfo.dollarValueList == null) {
                        dollarsInfo.dollarValueList = new ArrayList<>();
                    }
                    //这里的倍数是总押分的正常倍数，不用放大缩小100倍
                    dollarsInfo.dollarValueList.add(times * gameData.getLastBet());
                    gameRunInfo.addDollarsGoldTimes(times);
                }
            } else if (icon == DollarExpressConstant.BaseElement.ID_SAFE_BOX) {
                safeBoxIndex = i;
            } else if (icon == DollarExpressConstant.BaseElement.ID_GOLD_TRAIN) {
                goldTrainIndex = i;
            }
        }

        //给美钞设值
        if (dollarAllTimes > 0 && dollarsInfo.dollarIndexIds != null) {
            int times = dollarAllTimes / dollarsInfo.dollarIndexIds.size();
            long value = gameData.getLastBet() * times;
            if (dollarsInfo.dollarValueList == null) {
                dollarsInfo.dollarValueList = new ArrayList<>();
            }
            for (int i = 0; i < dollarsInfo.dollarIndexIds.size(); i++) {
                dollarsInfo.dollarValueList.add(value);
                gameRunInfo.addDollarsGoldTimes(times);
            }
        }

        log.debug("本局出现美金数量 count = {},values = {},allDollarTimes = {}", dollarsInfo.dollarIndexIds == null ? 0 : dollarsInfo.dollarIndexIds.size(), dollarsInfo.dollarValueList, gameRunInfo.getDollarsGoldTimes());

        //如果盘面中出现美金,且有保险箱，则会触发现金奖励
        if (dollarsInfo.dollarIndexIds != null && !dollarsInfo.dollarIndexIds.isEmpty()) {
            dollarsInfo.coinIndexId = safeBoxIndex;
        }

        //检查是否收集美元
        if (gameData.getLastBet() >= generateManager.getDollarExpressCollectDollarConfig().getStakeMin() && dollarsInfo.dollarIndexIds != null && dollarsInfo.dollarValueList != null) {
            dollarsInfo.collectDollarIndexIds = new ArrayList<>();
            boolean collect = false;
            for (int i = 0; i < dollarsInfo.dollarIndexIds.size(); i++) {
                int index = dollarsInfo.dollarIndexIds.get(i);
                int rand = RandomUtils.randomMinMax(0, 10000);
                if (rand < generateManager.getDollarExpressCollectDollarConfig().getProp()) {
                    collect = true;
                    dollarsInfo.collectDollarIndexIds.add(index);

                    gameData.addDollasCount(1);
//                    gameRunInfo.addTotalDollars(1);
                    log.debug("收集美元 playerId = {},currentCollectCount = {},totalCount = {}", gameData.playerId(), dollarsInfo.dollarValueList.size(), gameData.getTotalDollars());
                }
            }

            if (collect) {
                gameData.addDollarsTotalStake(gameData.getLastBet());
                log.debug("count = {},avg = {}", gameData.getAddDollarsCount(), gameData.getAddDollarsTotalStake() / gameData.getAddDollarsCount());
            }
        }

        //设置黄金列车倍数
        if (goldTrainIndex > 0 && dollarsInfo.dollarIndexIds != null && !dollarsInfo.dollarIndexIds.isEmpty()) {
            if (goldTrainCount < 1) {
                goldTrainCount = generateManager.getGoldTrainCount();
                if (goldTrainCount < 1) {
                    log.warn("获取的 goldTrainCount 小于1");
                }
            }

            int times = goldTrainCount * gameRunInfo.getDollarsGoldTimes() * timesScale;
            gameRunInfo.addBigPoolTimes(times);

            TrainInfo goldTrainInfo = goldTrainPbInfo(goldTrainCount, gameRunInfo.getDollarsGoldTimes() * gameData.getLastBet());
            gameRunInfo.addTrainInfo(goldTrainInfo);
            log.debug("添加黄金列车倍数 playerId = {},times = {},lastBet = {},train = {}", gameData.playerId(), times, gameData.getLastBet(), JSON.toJSONString(goldTrainInfo));
        }

        //设置保险箱倍数
        if (dollarsInfo.coinIndexId > 0 && gameRunInfo.getDollarsGoldTimes() > 0) {
            int addTimes = gameRunInfo.getDollarsGoldTimes() * timesScale;
            gameRunInfo.addBigPoolTimes(addTimes);
            log.debug("添加保险箱倍数 playerId = {},times = {}", gameData.playerId(), addTimes);
        }

        gameRunInfo.setDollarsInfo(dollarsInfo);
        return gameRunInfo;
    }

    @Override
    protected DollarExpressResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected DollarExpressGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected DollarExpressGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    protected DollarExpressPlayerGameData setGameDataValues(DollarExpressPlayerGameData playerGameData,DollarExpressPlayerGameDataDTO dto) {
        playerGameData.setRemainFreeCount(new AtomicInteger(dto.getRemainFreeCount()));
        playerGameData.setInvers(new AtomicBoolean(dto.isInvers()));
        playerGameData.setAllUnLock(new AtomicBoolean(dto.isAllUnLock()));
        return playerGameData;
    }

    @Override
    protected void specialGirdConfig(int gameType) {
        Map<Integer, SpecialGirdCfg> tempSpecialGirdCfgMap = new HashMap<>();
        Map<Integer, GirdUpdateConfig> tempGirdUpdateConfigMap = new HashMap<>();

        for (Map.Entry<Integer, SpecialGirdCfg> en : GameDataManager.getSpecialGirdCfgMap().entrySet()) {
            SpecialGirdCfg cfg = en.getValue();
            if (cfg.getGameType() != gameType) {
                continue;
            }
            if (cfg.getGirdUpdateType() != SlotsConst.SpecialGird.GIRD_UPDATE_TYPE_APPOINT && cfg.getGirdUpdateType() != SlotsConst.SpecialGird.GIRD_UPDATE_TYPE_APPOINT_2) {
                continue;
            }
            GirdUpdateConfig config = new GirdUpdateConfig();
            config.setId(cfg.getId());

            PropInfo propInfo = new PropInfo();
            int begin = 0;
            int end = 0;

            //元素
            for (Map.Entry<Integer, Integer> elementEn : cfg.getElement().entrySet()) {
                begin = end;

                int iconId = elementEn.getKey();
                config.setSpecialIconId(elementEn.getKey());
                end += elementEn.getValue();
                propInfo.addProp(iconId, begin, end);
            }
            propInfo.setSum(end);
            config.setOtherIconPropInfo(propInfo);


            propInfo = new PropInfo();
            begin = 0;
            end = 0;

            //巨型块大小
            for (List<String> list : cfg.getBigBlockSize()) {
                begin = end;
                end += Integer.parseInt(list.get(1));
                propInfo.addProp(Integer.parseInt(list.get(0)), begin, end);
            }
            propInfo.setSum(end);
            config.setSpecialIconPropInfo(propInfo);

            propInfo = new PropInfo();
            begin = 0;
            end = 0;
            //影响格子
            for (List<Integer> list : cfg.getAffectGird()) {
                int girdId = list.get(0);
                int prop = list.get(1);
                int maxShowLimit = list.get(2);

                begin = end;

                end += prop;
                propInfo.addProp(girdId, begin, end, maxShowLimit);
            }
            propInfo.setSum(end);
            config.setOtherIconAffectGirdPropInfo(propInfo);


            propInfo = new PropInfo();
            begin = 0;
            end = 0;
            //随机次数
            for (Map.Entry<Integer, Integer> randCountEn : cfg.getRandCount().entrySet()) {
                begin = end;
                end += randCountEn.getValue();
                propInfo.addProp(randCountEn.getKey(), begin, end);
            }
            propInfo.setSum(end);
            config.setRandCountPropInfo(propInfo);

            tempGirdUpdateConfigMap.put(cfg.getId(), config);
            tempSpecialGirdCfgMap.put(cfg.getId(), cfg);
        }

        if (!tempSpecialGirdCfgMap.isEmpty()) {
            this.specialGirdCfgMap = tempSpecialGirdCfgMap;
        }
        if (!tempGirdUpdateConfigMap.isEmpty()) {
            this.girdUpdateConfigMap = tempGirdUpdateConfigMap;
        }
    }

    /**
     * 计算火车是否中奖池
     *
     * @param playerGameData
     * @param trainList
     * @param gameRunInfo
     * @return
     */
    private DollarExpressGameRunInfo calTrainReward(DollarExpressPlayerGameData playerGameData, List<Train> trainList, DollarExpressGameRunInfo gameRunInfo) {
        if (trainList == null || trainList.isEmpty()) {
            return gameRunInfo;
        }

        //先转化程消息结构体
        for (Train train : trainList) {
            TrainInfo trainInfo = transTrainPbInfo(train, gameRunInfo.getBet());
            gameRunInfo.addTrainInfo(trainInfo);
        }

        for (TrainInfo trainInfo : gameRunInfo.getTrainList()) {
            //获取玩家累计贡献金额
            long contribt = playerGameData.getAllContribtPoolGold();
            if (contribt < 1) {
                break;
            }
            log.debug("玩家累计贡献金额 playerId = {},contribtGold = {},trainCoinId = {}", playerGameData.playerId(), contribt, trainInfo.type);

            //真奖池
            Number smallPoolNumber = slotsPoolDao.getSmallPoolByRoomCfgId(playerGameData.getGameType(), playerGameData.getRoomCfgId());
            if (smallPoolNumber == null) {
                log.debug("获取小池子金额为空 playerId = {},roomCfgId = {},trainCoinId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId(), trainInfo.type);
                break;
            }
            //假奖池
            Number fakeSmallPoolNumber = slotsPoolDao.getFakeSmallPoolByRoomCfgId(playerGameData.getGameType(), playerGameData.getRoomCfgId());
            if (fakeSmallPoolNumber == null) {
                log.debug("获取(假)小池子金额为空 playerId = {},roomCfgId = {},trainCoinId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId(), trainInfo.type);
                break;
            }

            //当真奖池 >= 假奖池时，才允许中奖
            long smallPool = smallPoolNumber.longValue();
            long fakeSmallPool = fakeSmallPoolNumber.longValue();

            if (smallPool < fakeSmallPool) {
                log.debug("真奖池小于假奖池，不允许中奖 playerId = {},roomCfgId = {},smallPool = {},fakeSmallPoolNumber = {}", playerGameData.playerId(), playerGameData.getRoomCfgId(), smallPool, fakeSmallPool);
                break;
            }

            log.debug("真奖池大于假奖池，允许中奖 playerId = {},roomCfgId = {},smallPool = {},fakeSmallPoolNumber = {}", playerGameData.playerId(), playerGameData.getRoomCfgId(), smallPool, fakeSmallPool);
            BigDecimal pool = BigDecimal.valueOf(smallPoolNumber.longValue());

            int poolId = getPoolIdByTrain(trainInfo.type);
            if (poolId < 1) {
                log.debug("获取的池子id小于1 trainCoinId = {},poolId = {}", trainInfo.type, poolId);
                continue;
            }
            PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
            if (poolCfg == null) {
                log.debug("获取的池子配置为空 trainCoinId = {},poolId = {}", trainInfo.type, poolId);
                continue;
            }
            //中奖概率,这里保留了8位，所以最后可以取int值
            int propV = BigDecimal.valueOf(contribt).divide(pool, 8, BigDecimal.ROUND_HALF_UP).divide(BigDecimal.valueOf(poolCfg.getPoolProp()), 8, BigDecimal.ROUND_HALF_UP).multiply(oneHundredMillionBigDecimal).intValue();
            int rand = RandomUtils.randomInt(oneHundredMillion);
//            rand = 1;
            if (rand >= propV) {
                log.debug("随机概率，未中奖 rand = {},propV = {}", rand, propV);
                continue;
            }
            //计算奖池金额
            //车厢节数+1，是因为要加上最后一个奖池车厢
            long addGold = calTrainPoolValue(playerGameData.getLastBet(),poolCfg.getGrowthRate(),poolCfg.getFakePoolInitTimes(),poolCfg.getFakePoolMax(),trainInfo.goldList.size()+1,poolCfg.getDelayTime());

            log.debug("概率计算可以中小奖池 playerId = {},rand = {},propV = {},addGold = {}", playerGameData.playerId(), rand, propV, addGold);

            //给玩家加钱
            CommonResult<Player> result = slotsPoolDao.rewardFromSmallPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), addGold, "SLOTS_TRAIN_" + pool);
            if (!result.success()) {
                log.warn("从小池子扣除，并给玩家加钱失败 code = {}", result.code);
                break;
            }

            //缓存中奖金额,以便计算玩家贡献金额
            playerGameData.addSmallPoolReward(addGold);
            gameRunInfo.addSmallPoolGold(addGold);

            trainInfo.goldList.add(addGold);
            trainInfo.poolId = poolId;
            log.debug("该火车中奖，并且加钱成功 playerId = {},addGold = {}", playerGameData.playerId(), addGold);
        }
        return gameRunInfo;
    }

    /**
     * 添加测试icons
     *
     * @param playerController
     * @param testLibData
     */
    public void addTestIconData(PlayerController playerController, TestLibData testLibData, boolean icons) {
        DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            return;
        }
        try {
            DollarExpressResultLib lib = new DollarExpressResultLib();
            lib.setRollerMode(playerGameData.getLastModelId());
            lib.setRollerId(1);

            if (icons) {
                lib = generateManager.checkAward(testLibData.getIcons(), lib).get(0);
            } else {
                int[] arr = new int[21];
                int index = 1;
                for (int i = 1; i <= 5; i++) {
                    int indexBegin = (i - 1) * 4;
                    int[] tmpArr = new int[4];
                    System.arraycopy(testLibData.getIcons(), indexBegin, tmpArr, 0, 4);

                    int tmpIndex = RandomUtils.nextInt(0, 4);
                    for (int j = 0; j < 4; j++) {
                        arr[index] = tmpArr[tmpIndex];
                        tmpIndex++;
                        if (tmpIndex >= 4) {
                            tmpIndex = 0;
                        }
                        index++;
                    }
                }
                testLibData.setIcons(arr);
            }
            testLibData.setResultLib(lib);
            playerGameData.addTestIconsData(testLibData);
            log.info("添加测试icons成功 playerId = {},libId = {}", playerController.playerId(), lib.getId());
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 测试使用，选择所有地区，只剩一个可选
     *
     * @param playerController
     */
    public void selectAllArea(PlayerController playerController) {
        DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            return;
        }
        for (int i = 1; i < 8; i++) {
            playerGameData.addSelectedArea(i);
        }
    }

    /**
     * 检查是否触发投资游戏
     *
     * @param playerGameData
     */
    private DollarExpressGameRunInfo checkInvers(DollarExpressPlayerGameData playerGameData, DollarExpressGameRunInfo gameRunInfo) {
        if (playerGameData.getTotalDollars() < generateManager.getDollarExpressCollectDollarConfig().getMax()) {
            return gameRunInfo;
        }

        Set<Integer> set = playerGameData.getSelectedAreaSet();
        if (set == null || set.isEmpty()) {
            gameRunInfo.setChoosableAreas(List.of(1, 2, 3, 4, 5, 6, 7, 8));
        } else {
            List<Integer> tmpList = new ArrayList<>();
            for (int i = 1; i <= 8; i++) {
                if (!set.contains(i)) {
                    tmpList.add(i);
                }
            }
            gameRunInfo.setChoosableAreas(tmpList);
        }

        boolean flag = playerGameData.getInvers().compareAndSet(false, true);
        if (flag) {
            gameRunInfo.setTotalDollars(playerGameData.getTotalDollars());
            playerGameData.setTotalDollars(0);
            log.debug("美金累计到 {} 个，触发条件 {} 个，触发投资小游戏后清零 playerId = {}", gameRunInfo.getTotalDollars(),generateManager.getDollarExpressCollectDollarConfig().getMax(), playerGameData.playerId());
        }
        return gameRunInfo;
    }

    /**
     * 退出游戏
     *
     * @param playerController
     */
    @Override
    public boolean exit(PlayerController playerController) {
        DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            return true;
        }

        if (playerGameData.getStatus() == DollarExpressConstant.Status.NOTMAL_ALL_BOARD || playerGameData.getStatus() == DollarExpressConstant.Status.GOLD_ALL_BOARD) {
            autoChooseFreeModelType(playerGameData);
        }

        DollarExpressPlayerGameDataDTO dto = playerGameData.converToDto();
        dto.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        dto.setInvers(playerGameData.getInvers().get());
        dto.setAllUnLock(playerGameData.getAllUnLock().get());
        gameDataDao.saveGameData(dto);
        return true;
    }

    @Override
    public void onTimer(TimerEvent e) {
        super.onTimer(e);
    }

    /**
     * 将库里面的中将线信息转化为消息
     *
     * @param infoList
     * @param bet
     * @return
     */
    private List<ResultLineInfo> transAwardLinePbInfo(List<DollarExpressAwardLineInfo> infoList, long bet) {
        if (infoList == null || infoList.isEmpty()) {
            return null;
        }

        List<ResultLineInfo> list = new ArrayList<>(infoList.size());
        for (DollarExpressAwardLineInfo lineInfo : infoList) {
            ResultLineInfo resultLineInfo = new ResultLineInfo();
            resultLineInfo.id = lineInfo.getId();
            resultLineInfo.iconIndexs = getIconIndexsByLineId(lineInfo.getId()).subList(0, lineInfo.getSameCount());
            resultLineInfo.times = lineInfo.getBaseTimes();
            resultLineInfo.winGold = bet * lineInfo.getBaseTimes();
            list.add(resultLineInfo);
        }
        return list;
    }

    /**
     * 计算火车奖池金额
     * @param stake
     * @param growthRate
     * @return
     */
    public long calTrainPoolValue(long stake, List<Integer> growthRate,int initTimes,int maxTimes) {
        return calTrainPoolValue(stake,growthRate,initTimes,maxTimes,0,0);
    }

    /**
     * 计算火车奖池金额
     * @param stake
     * @param growthRate
     * @return
     */
    public long calTrainPoolValue(long stake, List<Integer> growthRate,int initTimes,int maxTimes,int coach,int delayTime) {
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

        //总的延迟时间
        int allDelayTime = (coach * delayTime) / 1000;
        //余数y
        int y = (TimeHelper.getNowDaySeconds() + allDelayTime) % circulTimeBigDecimal.intValue();

        //实际金额
        BigDecimal step1 = BigDecimal.valueOf(y).divide(intervalTime, 4, RoundingMode.HALF_UP);
        BigDecimal step2 = step1.multiply(prop);
        BigDecimal step3 = step2.multiply(maxPoolBigDecimal.subtract(initPoolBigDecimal));
        long addGold = initPoolBigDecimal.add(step3).longValue();

//        log.debug("概率计算可以中小奖池 playerId = {},rand = {},propV = {},intervalTime = {},y = {},addGold = {}", playerGameData.playerId(), rand, propV, intervalTime, y, addGold);
//        log.debug("计算火车奖池金额 stake = {},timeValue = {},propValue = {},y = {},addGold = {}", stake, timeValue, propValue, y, addGold);
        return addGold;
    }

    /**
     * 将库里面的火车信息转化为消息
     *
     * @param train
     * @param bet
     * @return
     */
    private TrainInfo transTrainPbInfo(Train train, long bet) {
        TrainInfo trainInfo = new TrainInfo();
        trainInfo.type = train.getTrainIconId();
        if (train.getCoachs() != null && !train.getCoachs().isEmpty()) {
            trainInfo.goldList = new ArrayList<>(train.getCoachs().size());
            for (int[] arr : train.getCoachs()) {
//                trainInfo.goldList.add(bet * arr[1]);
                trainInfo.goldList.add(calWinGold(bet,arr[1]));
            }
        }
        return trainInfo;
    }

    private TrainInfo goldTrainPbInfo(int goldTrainCount, long gold) {
        TrainInfo goldTrainInfo = new TrainInfo();
        goldTrainInfo.type = DollarExpressConstant.BaseElement.ID_GOLD_TRAIN;
        goldTrainInfo.goldList = new ArrayList<>(goldTrainCount);
        for (int i = 0; i < goldTrainCount; i++) {
            goldTrainInfo.goldList.add(gold);
        }
        return goldTrainInfo;
    }

    private boolean checkUpdateType(int updateType, int libType) {
        if (libType == SlotsConst.SpecialResultLib.TYPE_GOLD_TRAIN) {
            return updateType == SlotsConst.SpecialGird.GIRD_UPDATE_TYPE_APPOINT_2;
        }

        if (libType == SlotsConst.SpecialResultLib.TYPE_AGAIN_GOLD_TRAIN) {
            return updateType == SlotsConst.SpecialGird.GIRD_UPDATE_TYPE_APPOINT;
        }
        return false;
    }

    /**
     * 火车与奖池映射
     * @param train
     * @return
     */
    private int getPoolIdByTrain(int train) {
        if (train == DollarExpressConstant.BaseElement.ID_GREEN_TRAIN) {
            return DollarExpressConstant.Common.MINI_POOL_ID;
        }
        if (train == DollarExpressConstant.BaseElement.ID_BLUE_TRAIN) {
            return DollarExpressConstant.Common.MINOR_POOL_ID;
        }
        if (train == DollarExpressConstant.BaseElement.ID_PURPLE_TRAIN) {
            return DollarExpressConstant.Common.MAJOR_POOL_ID;
        }
        if (train == DollarExpressConstant.BaseElement.ID_RED_TRAIN) {
            return DollarExpressConstant.Common.GRAND_POOL_ID;
        }
        return 0;
    }

    @Override
    public void shutdown() {
        log.info("正在关闭美元快递游戏管理器");
    }

    private long calWinGold(long bet,long times){
        BigDecimal timesBigDecimal = BigDecimal.valueOf(times).divide(timesScaleBigDecimal, 2, RoundingMode.DOWN);
        return BigDecimal.valueOf(bet).multiply(timesBigDecimal).longValue();
    }
}
