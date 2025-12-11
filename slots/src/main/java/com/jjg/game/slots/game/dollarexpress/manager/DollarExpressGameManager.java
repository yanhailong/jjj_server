package com.jjg.game.slots.game.dollarexpress.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.SpecialGirdCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryAwardInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.dao.DollarExpressGameDataDao;
import com.jjg.game.slots.game.dollarexpress.dao.DollarExpressResultLibDao;
import com.jjg.game.slots.game.dollarexpress.data.*;
import com.jjg.game.slots.game.dollarexpress.pb.DollarsInfo;
import com.jjg.game.slots.game.dollarexpress.pb.ResultLineInfo;
import com.jjg.game.slots.game.dollarexpress.pb.TrainInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 美元快递游戏逻辑处理器
 *
 * @author 11
 * @date 2025/6/11 16:48
 */
@Component
public class DollarExpressGameManager extends AbstractSlotsGameManager<DollarExpressPlayerGameData, DollarExpressResultLib> {

    @Autowired
    private DollarExpressResultLibDao libDao;
    @Autowired
    private DollarExpressGenerateManager generateManager;
    @Autowired
    private DollarExpressGameDataDao gameDataDao;

    private DollarExpressCollectDollarConfig dollarExpressCollectDollarConfig;

    //玩家自动二选一定时任务
    private Map<Long, TimerEvent<String>> autoChooseFreeModeEventMap = new HashMap<>();
    //玩家投资游戏定时任务
    private Map<Long, TimerEvent<String>> autoInversEventMap = new HashMap<>();


    public DollarExpressGameManager() {
        super(DollarExpressPlayerGameData.class, DollarExpressResultLib.class);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void init() {
        log.info("启动美元快递游戏管理器...");
        super.init();
    }

    @Override
    public DollarExpressGameRunInfo enterGame(PlayerController playerController) {
        DollarExpressGameRunInfo gameRunInfo = new DollarExpressGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerController);
            if (playerGameData == null) {
                log.debug("进入游戏时，playerGameData为空 playerId = {}", playerController.playerId());
                gameRunInfo.setCode(Code.FAIL);
                return gameRunInfo;
            }

            //执行自动二选一
            if (playerGameData.getStatus() == DollarExpressConstant.Status.NOTMAL_ALL_BOARD || playerGameData.getStatus() == DollarExpressConstant.Status.GOLD_ALL_BOARD) {
                autoChooseFreeModelType(playerGameData);
            }

            //自动投资游戏
            if (playerGameData.getInvers().get()) {
                autoInvest(playerGameData);
            }

            //检查当前是否处于特殊模式
            if (playerGameData.getStatus() == DollarExpressConstant.Status.ALL_BOARD_FREE) {
                int forCount = playerGameData.getRemainFreeCount().get();
                for (int i = 0; i < forCount; i++) {
                    autoStartGame(playerGameData, playerGameData.getAllBetScore());
                }
            } else if (playerGameData.getStatus() == DollarExpressConstant.Status.ALL_BOARD_TRAIN || playerGameData.getStatus() == DollarExpressConstant.Status.ALL_BOARD_GOLD_TRAIN) {
                autoStartGame(playerGameData, playerGameData.getAllBetScore());
            }

            gameRunInfo.setTotalDollars(playerGameData.getTotalDollars());
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
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
        return startGame(playerController, playerGameData, betValue, false);
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
     */
    public DollarExpressGameRunInfo invest(PlayerController playerController, DollarExpressPlayerGameData playerGameData, int areaId) {
        DollarExpressGameRunInfo gameRunInfo = new DollarExpressGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            if (!this.dollarExpressCollectDollarConfig.getTriggerTarMap().containsKey(areaId)) {
                log.debug("区域id参数错误，投资游戏失败 playerId = {},gameType = {},roomCfgId = {},areaId = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), areaId);
                gameRunInfo.setCode(Code.PARAM_ERROR);
                return gameRunInfo;
            }

            //设置活跃时间
            playerGameData.setLastActiveTime(TimeHelper.nowInt());

            //检查是否被选择
            boolean select = playerGameData.areaSelected(areaId);
            if (select) {
                log.debug("该地区已被选择 playerId = {},gameType = {},roomCfgId = {},areaId = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), areaId);
                gameRunInfo.setCode(Code.FORBID);
                return gameRunInfo;
            }

            boolean flag = playerGameData.getInvers().compareAndSet(true, false);
            if (!flag) {
                log.debug("当前不处于投资游戏 playerId = {},gameType = {},roomCfgId = {},areaId = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), areaId);
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            //获取触发的小游戏id
            SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(this.dollarExpressCollectDollarConfig.getAuxiliaryId());
            List<Integer> timesList = generateManager.inversTimes(specialAuxiliaryCfg);
            if (timesList == null || timesList.isEmpty()) {
                log.debug("获取投资游戏奖励倍数失败 playerId = {},gameType = {},roomCfgId = {},areaId = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), areaId);
                gameRunInfo.setCode(Code.FAIL);
                return gameRunInfo;
            }

            playerGameData.addSelectedArea(areaId);

            //中奖的次数
            int winCount = 0;
            List<Long> rewardGoldList = new ArrayList<>();
            long allAddGold = 0;

            //押分类型
            int scoreType = 0;
            if (specialAuxiliaryCfg.getAwardTypeC_value() != null && !specialAuxiliaryCfg.getAwardTypeC_value().isEmpty()) {
                scoreType = specialAuxiliaryCfg.getAwardTypeC_value().get(0);
            }

            long betScore = 0;
            if (scoreType == SlotsConst.Common.SCORE_TYPE_ONE_BET) {
                betScore = playerGameData.getOneBetScore();
            } else if (scoreType == SlotsConst.Common.SCORE_TYPE_ALL_BET) {
                betScore = playerGameData.getAllBetScore();
            } else {
                //默认平均单线押分
                betScore = playerGameData.getAddDollarsTotalStake() / playerGameData.getAddDollarsCount();
            }

            for (int times : timesList) {
                long gold = betScore * times;
                rewardGoldList.add(gold);
                allAddGold += gold;
                if (times > 0) {
                    winCount++;
                }
            }

            Player player = null;
            //3次中奖金币
            if (allAddGold > 0) {
                CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), allAddGold, AddType.SLOTS_INVEST_REWARD);
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
            if (winCount >= this.dollarExpressCollectDollarConfig.getAllWinCount()) {
                int rand = RandomUtils.randomMinMax(0, 10000);
                if (rand < this.dollarExpressCollectDollarConfig.getAllWinCountProp()) {
                    SpecialAuxiliaryCfg allWinSpecialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(this.dollarExpressCollectDollarConfig.getAllWinCountAuxiliaryId());
                    int goldTrainCount = generateManager.inversAllWinGoldTrainCount(allWinSpecialAuxiliaryCfg);
                    if (goldTrainCount > 0) {
                        long addGold = allAddGold * goldTrainCount;
                        CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), addGold, AddType.SLOTS_INVEST_REWARD);
                        if (!result.success()) {
                            log.warn("投资游戏金火车给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                            gameRunInfo.setCode(result.code);
                            return gameRunInfo;
                        }
                        gameRunInfo.addAllWinGold(addGold);

                        gameRunInfo.setInvestRewardGoldTrainCount(goldTrainCount);
                        gameRunInfo.setInvestRewardGold(allAddGold);
                        player = result.data;
                        log.debug("小地图3次都中奖，添加金火车的金币 gameType = {},roomCfgId = {},addGold = {}", this.gameType, playerGameData.getRoomCfgId(), addGold);
                    }
                }
            }

            //检查地图是否全部解锁
            if (playerGameData.areaAllUnlock()) {
                gameRunInfo.setAllAreaUnLock(true);
                playerGameData.getAllUnLock().compareAndSet(false, true);
            }

            if (playerController != null && player != null) {
                playerController.setPlayer(player);
            }
            playerGameData.clearInvers();
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    /**
     * 玩家投资游戏选择地区
     *
     * @param playerController
     * @param areaId
     * @return
     */
    public DollarExpressGameRunInfo playerInvest(PlayerController playerController, int areaId) {
        //获取玩家游戏数据
        DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，投资游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new DollarExpressGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        playerGameData.setLastActiveTime(TimeHelper.nowInt());

        return invest(playerController, playerGameData, areaId);
    }

    /**
     * 获取奖池
     *
     * @param playerController
     */
    public DollarExpressGameRunInfo getPoolValue(PlayerController playerController, long stake) {
        DollarExpressGameRunInfo gameRunInfo = new DollarExpressGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            gameRunInfo.setMini(getPoolValueByPoolId(DollarExpressConstant.Common.MINI_POOL_ID, stake));
            gameRunInfo.setMinor(getPoolValueByPoolId(DollarExpressConstant.Common.MINOR_POOL_ID, stake));
            gameRunInfo.setMajor(getPoolValueByPoolId(DollarExpressConstant.Common.MAJOR_POOL_ID, stake));
            gameRunInfo.setGrand(getPoolValueByPoolId(DollarExpressConstant.Common.GRAND_POOL_ID, stake));
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
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
     * 系统选择小地区
     *
     * @param playerGameData
     */
    public void autoInvest(DollarExpressPlayerGameData playerGameData) {
        try {
            List<Integer> choosableAreas = getChoosableAreas(playerGameData);
            if (choosableAreas.isEmpty()) {
                log.debug("系统自动投资游戏选择小地区失败，获取的可选区域为空 playerId = {}", playerGameData.playerId());
                return;
            }
            int areaId = choosableAreas.get(RandomUtils.randomInt(choosableAreas.size()));
            invest(null, playerGameData, areaId);
            log.info("系统自动投资游戏选择小地区结束 playerId = {},areaId = {}", playerGameData.playerId(), areaId);
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
     * 自动玩游戏
     *
     * @param betValue
     * @return
     */
    public DollarExpressGameRunInfo autoStartGame(DollarExpressPlayerGameData playerGameData, long betValue) {
        log.debug("系统开始自动玩游戏 playerId = {}", playerGameData.playerId());

        return startGame(new PlayerController(null,null), playerGameData, betValue, true);
    }

    /**
     * 开始游戏
     *
     * @param betValue
     * @return
     */
    public DollarExpressGameRunInfo
    startGame(PlayerController playerController, DollarExpressPlayerGameData playerGameData, long betValue, boolean auto) {
        DollarExpressGameRunInfo gameRunInfo = new DollarExpressGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(player.getGold());

            boolean allAreaUnlock = playerGameData.getAllUnLock().compareAndSet(true, false);
            if (allAreaUnlock) {
                gameRunInfo = areaAllUnlockGoldTrain(gameRunInfo, playerGameData);
            } else {
                //获取当前处于哪种状态
                int status = playerGameData.getStatus();
                if (status == DollarExpressConstant.Status.NORMAL) {  //正常
                    gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
                } else if (status == DollarExpressConstant.Status.NOTMAL_ALL_BOARD || status == DollarExpressConstant.Status.GOLD_ALL_BOARD) {  //二选一
                    gameRunInfo.setCode(Code.FORBID);
                    log.debug("当前正处于二选一状态，禁止开始游戏操作 playerId = {},gameType = {},roomCfgId = {}, status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                    return gameRunInfo;
                } else if (status == DollarExpressConstant.Status.ALL_BOARD_TRAIN) {  //二选一之拉火车
                    gameRunInfo = allBoardTrain(gameRunInfo, playerGameData);
                } else if (status == DollarExpressConstant.Status.ALL_BOARD_GOLD_TRAIN) {  //二选一之拉黄金火车
                    gameRunInfo = allBoardGoldTrain(gameRunInfo, playerGameData);
                } else if (status == DollarExpressConstant.Status.ALL_BOARD_FREE) {  //二选一之免费模式
                    gameRunInfo = allBoardFree(gameRunInfo, playerGameData);
                } else {
                    gameRunInfo.setCode(Code.FAIL);
                    log.debug("开始游戏失败，检测到错误状态 playerId = {},gameType = {},roomCfgId = {},status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                    return gameRunInfo;
                }
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            //检查是否触发投资游戏
            gameRunInfo = checkInvers(playerGameData, gameRunInfo);

            //标准池
            if (gameRunInfo.getBigPoolTimes() > 0) {
                long addGold = playerGameData.getOneBetScore() * gameRunInfo.getBigPoolTimes();
                if (addGold > 0) {
                    CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), addGold, AddType.SLOTS_BET_REWARD);
                    if (!result.success()) {
                        log.warn("给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                        gameRunInfo.setCode(result.code);
                        return gameRunInfo;
                    }
                    gameRunInfo.setAllWinGold(addGold);
                }
            }

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(),gameRunInfo.getAllWinGold(),betValue);

            //添加美元收集进度
            if (gameRunInfo.getTotalDollars() < 1) {
                gameRunInfo.setTotalDollars(playerGameData.getTotalDollars());
            }

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(player.getGold());

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData, betValue);
            log.debug("计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            //系统自动玩的游戏，不会走跑马灯
            if (!auto) {
                checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            }
            gameRunInfo.setData(playerGameData);
            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }


    /**
     * 普通正常流程
     *
     * @param gameRunInfo
     * @param playerGameData
     * @param betValue
     * @return
     */
    private DollarExpressGameRunInfo normal(DollarExpressGameRunInfo gameRunInfo, DollarExpressPlayerGameData playerGameData, long betValue) {
        CommonResult<Pair<DollarExpressResultLib,Long>> libResult = normalGetLib(playerGameData, betValue, DollarExpressConstant.SpecialMode.TYPE_NORMAL);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        DollarExpressResultLib resultLib = libResult.data.getFirst();
        gameRunInfo.setTax(libResult.data.getSecond());

        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(DollarExpressConstant.SpecialMode.TYPE_TRIGGER_ALL_BOARD)) {  //是否会触发二选一
            int count = generateManager.allBoadrCount(resultLib.getIconArr());
            if (count > generateManager.getAllBoardMinCount()) {
                playerGameData.setStatus(DollarExpressConstant.Status.GOLD_ALL_BOARD);
            } else {
                playerGameData.setStatus(DollarExpressConstant.Status.NOTMAL_ALL_BOARD);
            }
            log.debug("触发二选一  playerId = {},libId = {},status = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus());
        }

        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));

        gameRunInfo.setIconArr(resultLib.getIconArr());

        //检查与美元相关的逻辑
        gameRunInfo = checkDorllar(gameRunInfo, playerGameData, resultLib);
//        //计算火车奖励
        gameRunInfo = calTrainReward(playerGameData, resultLib, gameRunInfo);

        //添加中奖线信息
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(resultLib.getAwardLineInfoList(), playerGameData.getOneBetScore()));

        if (gameRunInfo.getBigPoolTimes() < 1) {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }

        gameRunInfo.setStatus(playerGameData.getStatus());
        gameRunInfo.setStake(betValue);
        gameRunInfo.setResultLib(resultLib);
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
            CommonResult<Integer> result = getResultLibSection(playerGameData.getLastModelId(), DollarExpressConstant.SpecialMode.TYPE_TRIGGER_NORMAL_TRAIN);
            if (!result.success()) {
                continue;
            }
            //获取结果库
            trainLib = libDao.getLibBySectionIndex(DollarExpressConstant.SpecialMode.TYPE_TRIGGER_NORMAL_TRAIN, result.data, this.libClass);
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

        gameRunInfo.setStatus(playerGameData.getStatus());
        playerGameData.setStatus(DollarExpressConstant.Status.NORMAL);

        gameRunInfo.setIconArr(trainLib.getIconArr());

        //检查与美元相关的逻辑
        gameRunInfo = checkDorllar(gameRunInfo, playerGameData, trainLib);
        //计算火车奖励
        gameRunInfo = calTrainReward(playerGameData, trainLib, gameRunInfo);

        gameRunInfo.setBigPoolTimes(trainLib.getTimes());
        gameRunInfo.setResultLib(trainLib);

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
    private DollarExpressGameRunInfo allBoardGoldTrain(DollarExpressGameRunInfo gameRunInfo, DollarExpressPlayerGameData playerGameData) {
        log.debug("进入二选一之拉黄金火车流程 playerId = {}", playerGameData.playerId());
        DollarExpressResultLib goldTrainLib = null;
        for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
            //获取一个倍数区间
            CommonResult<Integer> result = getResultLibSection(playerGameData.getLastModelId(), DollarExpressConstant.SpecialMode.TYPE_TRIGGER_GOLD_TRAIN);
            if (!result.success()) {
                continue;
            }
            //获取结果库
            goldTrainLib = libDao.getLibBySectionIndex(DollarExpressConstant.SpecialMode.TYPE_TRIGGER_GOLD_TRAIN, result.data, this.libClass);
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

        gameRunInfo = checkDorllar(gameRunInfo, playerGameData, goldTrainLib);

        gameRunInfo.addBigPoolTimes(goldTrainLib.getTimes());
        gameRunInfo.setResultLib(goldTrainLib);

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
        CommonResult<DollarExpressResultLib> libResult = freeGetLib(playerGameData, DollarExpressConstant.SpecialMode.TYPE_TRIGGER_FREE, DollarExpressConstant.SpecialAuxiliary.TYPE_ALL_BOARD_FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        DollarExpressResultLib freeGame = libResult.data;

        gameRunInfo.setStatus(playerGameData.getStatus());

        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);
        if (afterCount < 1) {
            playerGameData.setStatus(DollarExpressConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());

        //计算火车奖励
        gameRunInfo = calTrainReward(playerGameData, freeGame, gameRunInfo);

        //添加中奖线信息
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(freeGame.getAwardLineInfoList(), playerGameData.getOneBetScore()));
        gameRunInfo.setBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setResultLib(freeGame);

        return gameRunInfo;
    }

    /**
     * 地图全部解锁，奖励黄金火车
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    private DollarExpressGameRunInfo areaAllUnlockGoldTrain(DollarExpressGameRunInfo gameRunInfo, DollarExpressPlayerGameData playerGameData) {
        log.debug("进入地图全部解锁，奖励黄金火车流程 playerId = {}", playerGameData.playerId());
        DollarExpressResultLib goldTrainLib = null;
        for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
            //获取一个倍数区间
            CommonResult<Integer> result = getResultLibSection(playerGameData.getLastModelId(), DollarExpressConstant.SpecialMode.TYPE_TRIGGER_GOLD_TRAIN);
            if (!result.success()) {
                continue;
            }
            //获取结果库
            goldTrainLib = libDao.getLibBySectionIndex(DollarExpressConstant.SpecialMode.TYPE_TRIGGER_GOLD_TRAIN, result.data, this.libClass);
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

        gameRunInfo = checkDorllar(gameRunInfo, playerGameData, goldTrainLib);

        gameRunInfo.setBigPoolTimes(goldTrainLib.getTimes());
        playerGameData.setSelectedAreaSet(null);
        return gameRunInfo;
    }

    /**
     * 设置美元倍数等信息
     *
     * @param gameRunInfo
     * @return
     */
    public DollarExpressGameRunInfo checkDorllar(DollarExpressGameRunInfo gameRunInfo, DollarExpressPlayerGameData gameData, DollarExpressResultLib lib) {
        if (lib.getSpecialGirdInfoList() == null || lib.getSpecialGirdInfoList().isEmpty()) {
            return gameRunInfo;
        }

        log.debug("设置美元倍数信息 playerId = {},oneBetScore = {},allBetScore = {}", gameData.playerId(), gameData.getOneBetScore(), gameData.getAllBetScore());

        //美元坐标
        List<Integer> dollarIndexIds = null;
        //美元值
        List<Long> dollarValueList = null;

        for (SpecialGirdInfo specialGirdInfo : lib.getSpecialGirdInfoList()) {
            if (specialGirdInfo.getValueMap() == null || specialGirdInfo.getValueMap().isEmpty()) {
                continue;
            }
            //检查修改的图标是否有美元图标
            SpecialGirdCfg specialGirdCfg = GameDataManager.getSpecialGirdCfg(specialGirdInfo.getCfgId());
            if (!specialGirdCfg.getElement().containsKey(DollarExpressConstant.BaseElement.ID_DOLLAR) && !specialGirdCfg.getElement().containsKey(DollarExpressConstant.BaseElement.ID_DOLLAR_2)) {
                continue;
            }

            for (Map.Entry<Integer, Integer> en : specialGirdInfo.getValueMap().entrySet()) {
                if (dollarIndexIds == null) {
                    dollarIndexIds = new ArrayList<>();
                }
                if (dollarValueList == null) {
                    dollarValueList = new ArrayList<>();
                }
                dollarIndexIds.add(en.getKey());


                gameRunInfo.addDollarsGoldTimes(en.getValue());
                long value = gameData.getOneBetScore() * en.getValue();
                dollarValueList.add(value);
                log.debug("添加美元信息 girdId = {},value = {}", en.getKey(), value);
            }
        }

        if (dollarIndexIds == null || dollarIndexIds.isEmpty() || dollarValueList == null || dollarValueList.isEmpty()) {
            return gameRunInfo;
        }

        DollarsInfo dollarsInfo = new DollarsInfo();
        dollarsInfo.dollarIndexIds = dollarIndexIds;
        dollarsInfo.dollarValueList = dollarValueList;

        //保险箱坐标
        int safeBoxIndex = 0;
        //黄金列车坐标
        int goldTrainIndex = 0;
        for (int i = 1; i < gameRunInfo.getIconArr().length; i++) {
            int icon = gameRunInfo.getIconArr()[i];
            if (icon == DollarExpressConstant.BaseElement.ID_SAFE_BOX) {
                safeBoxIndex = i;
            } else if (icon == DollarExpressConstant.BaseElement.ID_GOLD_TRAIN) {
                goldTrainIndex = i;
            }
        }

        log.debug("本局美金信息 dollarCount = {},values = {},safeBoxIndex = {},goldTrainIndex = {}", dollarsInfo.dollarIndexIds == null ? 0 : dollarsInfo.dollarIndexIds.size(), dollarsInfo.dollarValueList, safeBoxIndex, goldTrainIndex);

        //如果盘面中出现美金,且有保险箱，则会触发现金奖励
        dollarsInfo.coinIndexId = safeBoxIndex;

        //检查是否收集美元
        if (gameData.getOneBetScore() >= this.dollarExpressCollectDollarConfig.getStakeMin()) {
            dollarsInfo.collectDollarIndexIds = new ArrayList<>();
            boolean collect = false;
            for (int i = 0; i < dollarsInfo.dollarIndexIds.size(); i++) {
                int index = dollarsInfo.dollarIndexIds.get(i);
                int icon = lib.getIconArr()[index];
                if (icon != DollarExpressConstant.BaseElement.ID_DOLLAR) {
                    continue;
                }

                int rand = RandomUtils.randomMinMax(0, 10000);
                if (rand < this.dollarExpressCollectDollarConfig.getProp()) {
                    collect = true;
                    dollarsInfo.collectDollarIndexIds.add(index);

                    gameData.addDollasCount(1);
                    log.debug("收集美元 playerId = {},girdId = {},currentCollectCount = {},totalCount = {}", gameData.playerId(), index, dollarsInfo.dollarValueList.size(), gameData.getTotalDollars());
                }
            }

            if (collect) {
                gameData.addDollarsTotalStake(gameData.getOneBetScore());
                log.debug("count = {},avg = {}", gameData.getAddDollarsCount(), gameData.getAddDollarsTotalStake() / gameData.getAddDollarsCount());
            }
        }

        //设置黄金列车倍数
        if (lib.getSpecialAuxiliaryInfoList() != null && !lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            for (SpecialAuxiliaryInfo info : lib.getSpecialAuxiliaryInfoList()) {
                SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(info.getCfgId());

                //黄金火车火车场景
                if (generateManager.goldTrainsTrainIconId(specialAuxiliaryCfg.getType()) > 0 && info.getAwardInfos() != null && !info.getAwardInfos().isEmpty()) {
                    for (Object object : info.getAwardInfos()) {
                        SpecialAuxiliaryAwardInfo awardInfo = (SpecialAuxiliaryAwardInfo) object;

                        long times = awardInfo.getRandCount() * gameRunInfo.getDollarsGoldTimes();

//                        gameRunInfo.addBigPoolTimes(times);

                        TrainInfo goldTrainInfo = goldTrainPbInfo(awardInfo.getRandCount(), gameRunInfo.getDollarsGoldTimes() * gameData.getOneBetScore());
                        gameRunInfo.addTrainInfo(goldTrainInfo);
                        log.debug("触发黄金列车 playerId = {},times = {},oneBetScore = {},train = {}", gameData.playerId(), times, gameData.getOneBetScore(), JSON.toJSONString(goldTrainInfo));
                    }
                    break;
                }
            }
        }

        //设置保险箱倍数
        if (dollarsInfo.coinIndexId > 0) {
//            gameRunInfo.addBigPoolTimes(gameRunInfo.getDollarsGoldTimes());
            log.debug("触发保险箱 playerId = {},times = {}", gameData.playerId(), gameRunInfo.getDollarsGoldTimes());
        }

        gameRunInfo.setDollarsInfo(dollarsInfo);
        return gameRunInfo;
    }

    @Override
    protected void offlineSaveGameDataDto(DollarExpressPlayerGameData gameData) {
        try {
            DollarExpressPlayerGameDataDTO dto = gameData.converToDto(DollarExpressPlayerGameDataDTO.class);
            gameDataDao.saveGameData(dto);
        } catch (Exception e) {
            log.error("", e);
        }

    }

    /**
     * 将库里面的中将线信息转化为消息
     *
     * @param infoList
     * @param oneBetScore 单线押分值
     * @return
     */
    private List<ResultLineInfo> transAwardLinePbInfo(List<DollarExpressAwardLineInfo> infoList, long oneBetScore) {
        if (infoList == null || infoList.isEmpty()) {
            return null;
        }

        List<ResultLineInfo> list = new ArrayList<>(infoList.size());
        for (DollarExpressAwardLineInfo lineInfo : infoList) {
            ResultLineInfo resultLineInfo = new ResultLineInfo();
            resultLineInfo.id = lineInfo.getId();
            resultLineInfo.iconIndexs = getIconIndexsByLineId(lineInfo.getId()).subList(0, lineInfo.getSameCount());
//            resultLineInfo.times = lineInfo.getBaseTimes();
            resultLineInfo.winGold = oneBetScore * lineInfo.getBaseTimes();
            list.add(resultLineInfo);
        }
        return list;
    }

    /**
     * 将库里面的火车信息转化为消息
     *
     * @param bet
     * @return
     */
    private List<TrainInfo> transTrainPbInfo(SpecialAuxiliaryInfo specialAuxiliaryInfo, long bet) {
        if (specialAuxiliaryInfo.getAwardInfos() == null || specialAuxiliaryInfo.getAwardInfos().isEmpty()) {
            return Collections.emptyList();
        }

        SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(specialAuxiliaryInfo.getCfgId());
        if (generateManager.allTrainsTrainIconId(specialAuxiliaryCfg.getType()) < 1) {
            return Collections.emptyList();
        }

        int trainType = generateManager.allTrainsTrainIconId(specialAuxiliaryCfg.getType());
        if (trainType < 1) {
            return Collections.emptyList();
        }
        log.debug("打印火车奖励 specialAuxiliaryInfo = {}", JSON.toJSONString(specialAuxiliaryInfo));

        List<TrainInfo> trainInfoList = new ArrayList<>();

        for (Object object : specialAuxiliaryInfo.getAwardInfos()) {
            SpecialAuxiliaryAwardInfo sa = (SpecialAuxiliaryAwardInfo) object;
            if (sa.getAwardCList() == null || sa.getAwardCList().isEmpty()) {
                continue;
            }

            TrainInfo trainInfo = new TrainInfo();
            trainInfo.type = trainType;

            trainInfo.goldList = new ArrayList<>();
            sa.getAwardCList().forEach(times -> trainInfo.goldList.add(bet * times));

            trainInfoList.add(trainInfo);
        }
        return trainInfoList;
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


    @Override
    public int getGameType() {
        return CoreConst.GameType.DOLLAR_EXPRESS;
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

    /**
     * 计算火车是否中奖池
     *
     * @param playerGameData
     * @param lib
     * @param gameRunInfo
     * @return
     */
    private DollarExpressGameRunInfo calTrainReward(DollarExpressPlayerGameData playerGameData, DollarExpressResultLib lib, DollarExpressGameRunInfo gameRunInfo) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return gameRunInfo;
        }

        //先转化程消息结构体
        lib.getSpecialAuxiliaryInfoList().forEach(info -> {
            List<TrainInfo> trainInfoList = transTrainPbInfo(info, playerGameData.getOneBetScore());
            if (!trainInfoList.isEmpty()) {
                gameRunInfo.addTrainInfo(trainInfoList);
            }
        });

        if (gameRunInfo.getTrainList() == null || gameRunInfo.getTrainList().isEmpty()) {
            return gameRunInfo;
        }

        for (TrainInfo trainInfo : gameRunInfo.getTrainList()) {
            int poolId = getPoolIdByTrain(trainInfo.type);
            if (poolId < 1) {
                log.debug("获取的池子id小于1 trainCoinId = {},poolId = {}", trainInfo.type, poolId);
                continue;
            }
            PoolCfg poolCfg = randWinPool(playerGameData, poolId);
            if (poolCfg == null) {
                continue;
            }
            //计算奖池金额
            //车厢节数+1，是因为要加上最后一个奖池车厢
            //总的延迟时间
            int allDelayTime = ((trainInfo.goldList.size() + 1) * poolCfg.getDelayTime()) / 1000;
            long addGold = calPoolValue(playerGameData.getOneBetScore(), poolCfg.getGrowthRate(), poolCfg.getFakePoolInitTimes(), poolCfg.getFakePoolMax(), allDelayTime);

            log.debug("概率计算可以中小奖池 playerId = {},addGold = {}", playerGameData.playerId(), addGold);

            //给玩家加钱
            CommonResult<Player> result = slotsPoolDao.rewardFromSmallPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), addGold, AddType.SLOTS_TRAIN, poolId + "");
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
     * 检查是否触发投资游戏
     *
     * @param playerGameData
     */
    private DollarExpressGameRunInfo checkInvers(DollarExpressPlayerGameData playerGameData, DollarExpressGameRunInfo gameRunInfo) {
        if (playerGameData.getTotalDollars() < this.dollarExpressCollectDollarConfig.getMax()) {
            return gameRunInfo;
        }

        gameRunInfo.setChoosableAreas(getChoosableAreas(playerGameData));
        boolean flag = playerGameData.getInvers().compareAndSet(false, true);
        if (flag) {
            gameRunInfo.setTotalDollars(playerGameData.getTotalDollars());
            playerGameData.setTotalDollars(0);
            log.debug("美金累计到 {} 个，触发条件 {} 个，触发投资小游戏后清零 playerId = {}", gameRunInfo.getTotalDollars(), this.dollarExpressCollectDollarConfig.getMax(), playerGameData.playerId());
        }
        return gameRunInfo;
    }

    /**
     * 获取投资游戏可选择的区域
     *
     * @param playerGameData
     * @return
     */
    private List<Integer> getChoosableAreas(DollarExpressPlayerGameData playerGameData) {
        Set<Integer> set = playerGameData.getSelectedAreaSet();
        if (set == null || set.isEmpty()) {
            return List.of(1, 2, 3, 4, 5, 6, 7, 8);
        } else {
            List<Integer> tmpList = new ArrayList<>();
            for (int i = 1; i <= 8; i++) {
                if (!set.contains(i)) {
                    tmpList.add(i);
                }
            }
            return tmpList;
        }
    }

    @Override
    protected void specialPlayConfig() {
        SpecialPlayCfg specialPlayCfg1 = GameDataManager.getSpecialPlayCfgList().stream().
                filter(cfg -> cfg.getGameType() == this.gameType && cfg.getPlayType() == DollarExpressConstant.SpecialPlay.TYPE_COLLECT_DOLLAR).
                findFirst().orElse(null);

        SpecialPlayCfg specialPlayCfg2 = GameDataManager.getSpecialPlayCfgList().stream().
                filter(cfg -> cfg.getGameType() == this.gameType && cfg.getPlayType() == DollarExpressConstant.SpecialPlay.TYPE_INVERS_ALL_WIN).
                findFirst().orElse(null);

        String[] arr = specialPlayCfg1.getValue().split(",");

        DollarExpressCollectDollarConfig config = new DollarExpressCollectDollarConfig();

        config.setStakeMin(Long.parseLong(arr[1]));

        config.setStakeAllBetScoreMin(oneLineToAllStake(config.getStakeMin()));

        config.setBegin(Integer.parseInt(arr[2]));
        config.setProp(Integer.parseInt(arr[3]));
        config.setMax(Integer.parseInt(arr[4]));
        config.setAuxiliaryId(Integer.parseInt(arr[5]));
        config.setTriggerTarMap(specialPlayCfg1.getTriggerTag());
        config.setTriggerType(specialPlayCfg1.getTriggerRewards().get(0));
        config.setTriggerAuxiliaryId(specialPlayCfg1.getTriggerRewards().get(1));

        String[] arr2 = specialPlayCfg2.getValue().split(",");
        config.setAllWinCount(Integer.parseInt(arr2[1]));
        config.setAllWinCountProp(Integer.parseInt(arr2[2]));
        config.setAllWinCountAuxiliaryId(Integer.parseInt(arr2[3]));

        this.dollarExpressCollectDollarConfig = config;


    }

    /**
     * 火车与奖池映射
     *
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

    /**
     * 退出游戏
     *
     * @param playerController
     * @param initiativeExit
     * @return 返回值来标记是否可以进行断线重连
     */
    @Override
    public DollarExpressPlayerGameData exit(PlayerController playerController, boolean initiativeExit) {
        DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            return null;
        }

        if (playerGameData.getStatus() == DollarExpressConstant.Status.NOTMAL_ALL_BOARD || playerGameData.getStatus() == DollarExpressConstant.Status.GOLD_ALL_BOARD) {
            TimerEvent<String> autoChooseEvent = new TimerEvent<>(this, 60, DollarExpressConstant.EventName.AUTO_CHOOSE_FREEMODEL_TYPE + "_" + playerController.playerId() + "_" + playerController.getPlayer().getRoomCfgId()).withTimeUnit(TimeUnit.SECONDS);
            this.timerCenter.add(autoChooseEvent);
            this.autoChooseFreeModeEventMap.put(playerController.playerId(), autoChooseEvent);
            log.debug("添加自动二选一事件 playerId = {}", playerController.playerId());
        }

        if (playerGameData.getInvers().get()) {
            TimerEvent<String> autoInversEvent = new TimerEvent<>(this, 60, DollarExpressConstant.EventName.AUTO_INVERS + "_" + playerController.playerId() + "_" + playerController.getPlayer().getRoomCfgId()).withTimeUnit(TimeUnit.SECONDS);
            this.timerCenter.add(autoInversEvent);
            this.autoInversEventMap.put(playerController.playerId(), autoInversEvent);
            log.debug("添加自动投资游戏事件 playerId = {}", playerController.playerId());
        }

        playerGameData.setOnline(false);
        offlineSaveGameDataDto(playerGameData);
        removePlayerGameData(playerController.playerId(),playerGameData.getRoomCfgId());
        return playerGameData;
    }

    @Override
    public void onTimer(TimerEvent e) {
        super.onTimer(e);

        String[] arr = e.getParameter().toString().split("_");
        if (DollarExpressConstant.EventName.AUTO_CHOOSE_FREEMODEL_TYPE.equals(arr[0])) {
            long playerId = Long.parseLong(arr[1]);
            int roomCfgId = Integer.parseInt(arr[2]);
            this.autoChooseFreeModeEventMap.remove(playerId);
            DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerId, roomCfgId);
            if (playerGameData == null) {
                log.debug("自动二选一事件，获取 playerGameData 为空 playerId = {},roomCfgId = {}", playerId, roomCfgId);
                return;
            }
            autoChooseFreeModelType(playerGameData);
            //检查当前是否处于特殊模式
            if (playerGameData.getStatus() == DollarExpressConstant.Status.ALL_BOARD_FREE) {
                int forCount = playerGameData.getRemainFreeCount().get();
                for (int i = 0; i < forCount; i++) {
                    autoStartGame(playerGameData, playerGameData.getAllBetScore());
                }
            } else if (playerGameData.getStatus() == DollarExpressConstant.Status.ALL_BOARD_TRAIN || playerGameData.getStatus() == DollarExpressConstant.Status.ALL_BOARD_GOLD_TRAIN) {
                autoStartGame(playerGameData, playerGameData.getAllBetScore());
            }
        } else if (DollarExpressConstant.EventName.AUTO_INVERS.equals(arr[0])) {
            long playerId = Long.parseLong(arr[1]);
            int roomCfgId = Integer.parseInt(arr[2]);
            this.autoInversEventMap.remove(playerId);
            DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerId, roomCfgId);
            if (playerGameData == null) {
                log.debug("自动投资事件，获取 playerGameData 为空 playerId = {},roomCfgId = {}", playerId, roomCfgId);
                return;
            }
            autoInvest(playerGameData);
        }
    }

    public DollarExpressCollectDollarConfig getDollarExpressCollectDollarConfig() {
        return dollarExpressCollectDollarConfig;
    }
}
