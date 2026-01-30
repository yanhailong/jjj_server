package com.jjg.game.slots.game.wealthbank.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryAwardInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;
import com.jjg.game.slots.game.wealthbank.WealthBankConstant;
import com.jjg.game.slots.game.wealthbank.dao.WealthBankGameDataDao;
import com.jjg.game.slots.game.wealthbank.dao.WealthBankResultLibDao;
import com.jjg.game.slots.game.wealthbank.data.*;
import com.jjg.game.slots.game.wealthbank.pb.WealthBankDollarsInfo;
import com.jjg.game.slots.game.wealthbank.pb.WealthBankResultLineInfo;
import com.jjg.game.slots.game.wealthbank.pb.WealthBankTrainInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public abstract class AbstractWealthBankGameManager extends AbstractSlotsGameManager<WealthBankPlayerGameData, WealthBankResultLib, WealthBankGameRunInfo> {
    @Autowired
    protected WealthBankResultLibDao libDao;
    @Autowired
    protected WealthBankGenerateManager generateManager;
    @Autowired
    protected WealthBankGameDataDao gameDataDao;

    protected WealthBankCollectDollarConfig wealthBankCollectDollarConfig;

    public AbstractWealthBankGameManager() {
        super(WealthBankPlayerGameData.class, WealthBankResultLib.class, WealthBankGameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("[Wealth Bank] 启动财富银行游戏管理器...");
        super.init();
    }

    @Override
    public WealthBankGameRunInfo enterGame(PlayerController playerController) {
        WealthBankGameRunInfo gameRunInfo = new WealthBankGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            WealthBankPlayerGameData playerGameData = getPlayerGameData(playerController);
            if (playerGameData == null) {
                log.debug("[Wealth Bank] 进入游戏时，playerGameData为空 playerId = {}", playerController.playerId());
                gameRunInfo.setCode(Code.FAIL);
                return gameRunInfo;
            }

            //执行自动二选一
            if (playerGameData.getStatus() == WealthBankConstant.Status.NOTMAL_ALL_BOARD || playerGameData.getStatus() == WealthBankConstant.Status.GOLD_ALL_BOARD) {
                autoChooseFreeModelType(playerGameData);
            }

            //自动投资游戏
            if (playerGameData.getInvers().get()) {
                autoInvest(playerGameData);
            }

            //检查当前是否处于特殊模式
            if (playerGameData.getStatus() == WealthBankConstant.Status.ALL_BOARD_FREE) {
                int forCount = playerGameData.getRemainFreeCount().get();
                for (int i = 0; i < forCount; i++) {
                    autoStartGame(playerGameData, playerGameData.getAllBetScore());
                }
            } else if (playerGameData.getStatus() == WealthBankConstant.Status.ALL_BOARD_TRAIN || playerGameData.getStatus() == WealthBankConstant.Status.ALL_BOARD_GOLD_TRAIN) {
                autoStartGame(playerGameData, playerGameData.getAllBetScore());
            }
            gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
            gameRunInfo.setTotalDollars(playerGameData.getTotalDollars());
        } catch (Exception e) {
            log.error("[Wealth Bank] ", e);
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
    public WealthBankGameRunInfo playerChooseFreeGameType(PlayerController playerController, int chooseStatus) {
        WealthBankGameRunInfo gameRunInfo = new WealthBankGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            //获取玩家游戏数据
            WealthBankPlayerGameData playerGameData = getPlayerGameData(playerController);
            if (playerGameData == null) {
                log.debug("[Wealth Bank] 获取玩家游戏数据失败，二选一失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            int code = chooseFreeGameType(playerGameData, chooseStatus);
            if (code != Code.SUCCESS) {
                gameRunInfo.setCode(code);
                return gameRunInfo;
            }
            gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
            gameRunInfo.setStatus(playerGameData.getStatus());
            log.info("[Wealth Bank] 玩家进行二选一，playerId = {},gameType = {},roomCfgId = {},chooseStatus = {}", playerController.playerId(), playerGameData.getGameType(), playerController.getPlayer().getRoomCfgId(), chooseStatus);
            return gameRunInfo;
        } catch (Exception e) {
            log.error("[Wealth Bank] ", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    /**
     * 投资游戏选择地区
     */
    public WealthBankGameRunInfo invest(PlayerController playerController, WealthBankPlayerGameData playerGameData, int areaId) {
        WealthBankGameRunInfo gameRunInfo = new WealthBankGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            if (!this.wealthBankCollectDollarConfig.getTriggerTarMap().containsKey(areaId)) {
                log.debug("[Wealth Bank] 区域id参数错误，投资游戏失败 playerId = {},gameType = {},roomCfgId = {},areaId = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), areaId);
                gameRunInfo.setCode(Code.PARAM_ERROR);
                return gameRunInfo;
            }

            //检查是否被选择
            boolean select = playerGameData.areaSelected(areaId);
            if (select) {
                log.debug("[Wealth Bank] 该地区已被选择 playerId = {},gameType = {},roomCfgId = {},areaId = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), areaId);
                gameRunInfo.setCode(Code.FORBID);
                return gameRunInfo;
            }

            boolean flag = playerGameData.getInvers().compareAndSet(true, false);
            if (!flag) {
                log.debug("[Wealth Bank] 当前不处于投资游戏 playerId = {},gameType = {},roomCfgId = {},areaId = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), areaId);
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            //获取触发的小游戏id
            SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(this.wealthBankCollectDollarConfig.getAuxiliaryId());
            List<Integer> timesList = generateManager.inversTimes(specialAuxiliaryCfg);
            if (timesList == null || timesList.isEmpty()) {
                log.debug("[Wealth Bank] 获取投资游戏奖励倍数失败 playerId = {},gameType = {},roomCfgId = {},areaId = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), areaId);
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
            } else if (scoreType == SlotsConst.Common.SCORE_TYPE_ONE_BET) {
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
                log.debug("[Wealth Bank] 投资游戏玩家添加金币 gameType = {},roomCfgId = {},addGold = {}", this.gameType, playerGameData.getRoomCfgId(), allAddGold);
            }

            //3次全部中奖的金火车
            if (winCount >= this.wealthBankCollectDollarConfig.getAllWinCount()) {
                int rand = RandomUtils.randomMinMax(0, 10000);
                if (rand < this.wealthBankCollectDollarConfig.getAllWinCountProp()) {
                    SpecialAuxiliaryCfg allWinSpecialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(this.wealthBankCollectDollarConfig.getAllWinCountAuxiliaryId());
                    int goldTrainCount = generateManager.inversAllWinGoldTrainCount(allWinSpecialAuxiliaryCfg);
                    if (goldTrainCount > 0) {
                        long addGold = allAddGold * goldTrainCount;
                        CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), addGold, AddType.SLOTS_INVEST_REWARD);
                        if (!result.success()) {
                            log.warn("[Wealth Bank] 投资游戏金火车给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                            gameRunInfo.setCode(result.code);
                            return gameRunInfo;
                        }
                        gameRunInfo.addAllWinGold(addGold);

                        gameRunInfo.setInvestRewardGoldTrainCount(goldTrainCount);
                        gameRunInfo.setInvestRewardGold(allAddGold);
                        player = result.data;
                        log.debug("[Wealth Bank] 小地图3次都中奖，添加金火车的金币 gameType = {},roomCfgId = {},addGold = {}", this.gameType, playerGameData.getRoomCfgId(), addGold);
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
            log.error("[Wealth Bank] ", e);
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
    public WealthBankGameRunInfo playerInvest(PlayerController playerController, int areaId) {
        //获取玩家游戏数据
        WealthBankPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("[Wealth Bank] 获取玩家游戏数据失败，投资游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new WealthBankGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        if (getRoomType() != null) {
            int code = slotsRoomManager.checkCanPlay(this, playerController);
            if (code != Code.SUCCESS) {
                log.debug("该游戏无法继续 playerId = {},gameType = {},roomCfgId = {},code = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), code);
                return new WealthBankGameRunInfo(code, playerController.playerId());
            }
        }
        return invest(playerController, playerGameData, areaId);
    }

    /**
     * 系统自动二选一
     *
     * @param playerGameData
     */
    public void autoChooseFreeModelType(WealthBankPlayerGameData playerGameData) {
        try {
            int chooseStatus;
            if (playerGameData.getStatus() == WealthBankConstant.Status.NOTMAL_ALL_BOARD) {
                chooseStatus = RandomUtils.randomInt(2) == 0 ? WealthBankConstant.Status.ALL_BOARD_TRAIN : WealthBankConstant.Status.ALL_BOARD_FREE;
            } else {
                chooseStatus = RandomUtils.randomInt(2) == 0 ? WealthBankConstant.Status.ALL_BOARD_GOLD_TRAIN : WealthBankConstant.Status.ALL_BOARD_FREE;
            }

            int code = chooseFreeGameType(playerGameData, chooseStatus);
            if (code != Code.SUCCESS) {
                log.debug("[Wealth Bank] 系统自动二选一失败 playerId = {},chooseStatus = {}", playerGameData.playerId(), chooseStatus);
                return;
            }
            log.info("[Wealth Bank] 系统自动进行二选一 playerId = {},chooseStatus = {}", playerGameData.playerId(), chooseStatus);
        } catch (Exception e) {
            log.error("[Wealth Bank] ", e);
        }
    }

    /**
     * 系统选择小地区
     *
     * @param playerGameData
     */
    public void autoInvest(WealthBankPlayerGameData playerGameData) {
        try {
            List<Integer> choosableAreas = getChoosableAreas(playerGameData);
            if (choosableAreas.isEmpty()) {
                log.debug("[Wealth Bank] 系统自动投资游戏选择小地区失败，获取的可选区域为空 playerId = {}", playerGameData.playerId());
                return;
            }
            int areaId = choosableAreas.get(RandomUtils.randomInt(choosableAreas.size()));
            invest(null, playerGameData, areaId);
            log.info("[Wealth Bank] 系统自动投资游戏选择小地区结束 playerId = {},areaId = {}", playerGameData.playerId(), areaId);
        } catch (Exception e) {
            log.error("[Wealth Bank] ", e);
        }
    }

    /**
     * 二选一选哪个？？？
     *
     * @param chooseStatus
     * @return
     */
    private int chooseFreeGameType(WealthBankPlayerGameData playerGameData, int chooseStatus) {
        if (playerGameData.getStatus() == WealthBankConstant.Status.NOTMAL_ALL_BOARD) {  //普通二选一
            if (chooseStatus == WealthBankConstant.Status.ALL_BOARD_TRAIN) {  //拉普通火车
                playerGameData.setStatus(WealthBankConstant.Status.ALL_BOARD_TRAIN);
            } else {
                playerGameData.setStatus(WealthBankConstant.Status.ALL_BOARD_FREE);
                playerGameData.getRemainFreeCount().set(8);
            }
        } else if (playerGameData.getStatus() == WealthBankConstant.Status.GOLD_ALL_BOARD) {  //黄金二选一
            if (chooseStatus == WealthBankConstant.Status.ALL_BOARD_GOLD_TRAIN) {  //拉黄金火车
                playerGameData.setStatus(WealthBankConstant.Status.ALL_BOARD_GOLD_TRAIN);
            } else {
                playerGameData.setStatus(WealthBankConstant.Status.ALL_BOARD_FREE);
                playerGameData.getRemainFreeCount().set(8);
            }
        } else {
            log.debug("[Wealth Bank] 当前不处于二选一状态，禁止二选一操作 playerId = {},gameType = {},roomCfgId = {},status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), playerGameData.getStatus());
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
    public WealthBankGameRunInfo autoStartGame(WealthBankPlayerGameData playerGameData, long betValue) {
        log.debug("[Wealth Bank] 系统开始自动玩游戏 playerId = {}", playerGameData.playerId());

        return startGame(null, playerGameData, betValue, true);
    }

    /**
     * 开始游戏
     *
     * @param betValue
     * @return
     */
    @Override
    public WealthBankGameRunInfo
    startGame(PlayerController playerController, WealthBankPlayerGameData playerGameData, long betValue, boolean auto) {
        WealthBankGameRunInfo gameRunInfo = new WealthBankGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerGameData.getPlayer().getRoomCfgId());
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerGameData.setPlayer(player);

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            boolean allAreaUnlock = playerGameData.getAllUnLock().compareAndSet(true, false);
            if (allAreaUnlock) {
                gameRunInfo = areaAllUnlockGoldTrain(gameRunInfo, playerGameData);
            } else {
                //获取当前处于哪种状态
                int status = playerGameData.getStatus();
                if (status == WealthBankConstant.Status.NORMAL) {  //正常
                    gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
                } else if (status == WealthBankConstant.Status.NOTMAL_ALL_BOARD || status == WealthBankConstant.Status.GOLD_ALL_BOARD) {  //二选一
                    gameRunInfo.setCode(Code.FORBID);
                    log.debug("[Wealth Bank] 当前正处于二选一状态，禁止开始游戏操作 playerId = {},gameType = {},roomCfgId = {}, status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                    return gameRunInfo;
                } else if (status == WealthBankConstant.Status.ALL_BOARD_TRAIN) {  //二选一之拉火车
                    gameRunInfo = allBoardTrain(gameRunInfo, playerGameData);
                } else if (status == WealthBankConstant.Status.ALL_BOARD_GOLD_TRAIN) {  //二选一之拉黄金火车
                    gameRunInfo = allBoardGoldTrain(gameRunInfo, playerGameData);
                } else if (status == WealthBankConstant.Status.ALL_BOARD_FREE) {  //二选一之免费模式
                    gameRunInfo = allBoardFree(gameRunInfo, playerGameData);
                } else {
                    gameRunInfo.setCode(Code.FAIL);
                    log.debug("[Wealth Bank] 开始游戏失败，检测到错误状态 playerId = {},gameType = {},roomCfgId = {},status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                    return gameRunInfo;
                }
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            //检查是否触发投资游戏
            gameRunInfo = checkInvers(playerGameData, gameRunInfo);

            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);
            // 补丁：rewardFromBigPool中setAllWinGold没有加smallPoolGold 导致两次显示总金额不同
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());
            //触发实际赢钱的task
            triggerWinTask(playerGameData.getPlayer(), gameRunInfo.getAllWinGold(), betValue, warehouseCfg.getTransactionItemId());

            //添加美元收集进度
            if (gameRunInfo.getTotalDollars() < 1) {
                gameRunInfo.setTotalDollars(playerGameData.getTotalDollars());
            }

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerGameData.setPlayer(player);

            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData, betValue);
            log.debug("[Wealth Bank] 计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            //系统自动玩的游戏，不会走跑马灯
            if (!auto) {
                checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            }
            gameRunInfo.setData(playerGameData);
            return gameRunInfo;
        } catch (Exception e) {
            log.error("[Wealth Bank] ", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    @Override
    protected WealthBankGameRunInfo normal(WealthBankGameRunInfo gameRunInfo, WealthBankPlayerGameData playerGameData, long betValue, WealthBankResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(WealthBankConstant.SpecialMode.TYPE_TRIGGER_ALL_BOARD)) {  //是否会触发二选一
            int count = generateManager.allBoadrCount(resultLib.getIconArr());
            if (count > generateManager.getAllBoardMinCount()) {
                playerGameData.setStatus(WealthBankConstant.Status.GOLD_ALL_BOARD);
            } else {
                playerGameData.setStatus(WealthBankConstant.Status.NOTMAL_ALL_BOARD);
            }
            log.debug("[Wealth Bank] 触发二选一  playerId = {},libId = {},status = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus());
        }

        log.debug("[Wealth Bank] id = {}", resultLib.getId());

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
    protected WealthBankGameRunInfo allBoardTrain(WealthBankGameRunInfo gameRunInfo, WealthBankPlayerGameData playerGameData) {
        log.debug("[Wealth Bank] 进入二选一之拉火车流程 playerId = {}", playerGameData.playerId());

        CommonResult<WealthBankResultLib> libResult = getLibFromDB(playerGameData, DollarExpressConstant.SpecialMode.TYPE_TRIGGER_NORMAL_TRAIN);
        if (!libResult.success()) {
            return null;
        }
        WealthBankResultLib trainLib = libResult.data;

        if (trainLib == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("[Wealth Bank] 未在该条结果库中找到重转信息1 gameType = {},modelId = {}", this.gameType, playerGameData.getLastModelId());
            return gameRunInfo;
        }

        log.debug("[Wealth Bank] 获取到拉火车的结果库 playerId = {},libId = {}", playerGameData.playerId(), trainLib.getId());

        gameRunInfo.setStatus(playerGameData.getStatus());
        playerGameData.setStatus(WealthBankConstant.Status.NORMAL);

        gameRunInfo.setIconArr(trainLib.getIconArr());

        //检查与美元相关的逻辑
        gameRunInfo = checkDorllar(gameRunInfo, playerGameData, trainLib);
        //计算火车奖励
        gameRunInfo = calTrainReward(playerGameData, trainLib, gameRunInfo);

        gameRunInfo.setBigPoolTimes(trainLib.getTimes());
        gameRunInfo.setResultLib(trainLib);

        log.debug("[Wealth Bank] libId = {}", trainLib.getId());
        return gameRunInfo;

    }

    /**
     * 返回免费模式选择黄金火车
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    protected WealthBankGameRunInfo allBoardGoldTrain(WealthBankGameRunInfo gameRunInfo, WealthBankPlayerGameData playerGameData) {
        log.debug("[Wealth Bank] 进入二选一之拉黄金火车流程 playerId = {}", playerGameData.playerId());

        CommonResult<WealthBankResultLib> libResult = getLibFromDB(playerGameData, DollarExpressConstant.SpecialMode.TYPE_TRIGGER_GOLD_TRAIN);
        if (!libResult.success()) {
            return null;
        }
        WealthBankResultLib goldTrainLib = libResult.data;

        if (goldTrainLib == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("[Wealth Bank] 未在该条结果库中找到重转信息 gameType = {},modelId = {}", this.gameType, playerGameData.getLastModelId());
            return gameRunInfo;
        }

        log.debug("[Wealth Bank] 成功获取黄金列车结果库 playerId = {},libId = {}", playerGameData.playerId(), goldTrainLib.getId());

        gameRunInfo.setStatus(playerGameData.getStatus());
        playerGameData.setStatus(WealthBankConstant.Status.NORMAL);
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
    protected WealthBankGameRunInfo allBoardFree(WealthBankGameRunInfo gameRunInfo, WealthBankPlayerGameData playerGameData) {
        CommonResult<WealthBankResultLib> libResult = freeGetLib(playerGameData, WealthBankConstant.SpecialMode.TYPE_TRIGGER_FREE, WealthBankConstant.SpecialAuxiliary.TYPE_ALL_BOARD_FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        WealthBankResultLib freeGame = libResult.data;

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        gameRunInfo.setStatus(playerGameData.getStatus());

        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);
        if (afterCount < 1) {
            playerGameData.setStatus(WealthBankConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            //最后一局，通知客户端，累计免费模式的中奖金额
            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
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
    protected WealthBankGameRunInfo areaAllUnlockGoldTrain(WealthBankGameRunInfo gameRunInfo, WealthBankPlayerGameData playerGameData) {
        log.debug("[Wealth Bank] 进入地图全部解锁，奖励黄金火车流程 playerId = {}", playerGameData.playerId());

        CommonResult<WealthBankResultLib> libResult = getLibFromDB(playerGameData, DollarExpressConstant.SpecialMode.TYPE_TRIGGER_GOLD_TRAIN);
        if (!libResult.success()) {
            return null;
        }
        WealthBankResultLib goldTrainLib = libResult.data;

        if (goldTrainLib == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("[Wealth Bank] 地图全部解锁,未在该条结果库中找到重转信息 gameType = {},modelId = {}", this.gameType, playerGameData.getLastModelId());
            return gameRunInfo;
        }

        playerGameData.setStatus(WealthBankConstant.Status.NORMAL);
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
    public WealthBankGameRunInfo checkDorllar(WealthBankGameRunInfo gameRunInfo, WealthBankPlayerGameData gameData, WealthBankResultLib lib) {
        if (lib.getSpecialGirdInfoList() == null || lib.getSpecialGirdInfoList().isEmpty()) {
            return gameRunInfo;
        }

        log.debug("[Wealth Bank] 设置美元倍数信息 playerId = {},oneBetScore = {},allBetScore = {}", gameData.playerId(), gameData.getOneBetScore(), gameData.getAllBetScore());

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
            if (!specialGirdCfg.getElement().containsKey(WealthBankConstant.BaseElement.ID_DOLLAR) && !specialGirdCfg.getElement().containsKey(WealthBankConstant.BaseElement.ID_DOLLAR_2)) {
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
                log.debug("[Wealth Bank] 添加美元信息 girdId = {},value = {}", en.getKey(), value);
            }
        }

        if (dollarIndexIds == null || dollarIndexIds.isEmpty() || dollarValueList == null || dollarValueList.isEmpty()) {
            return gameRunInfo;
        }

        WealthBankDollarsInfo wealthBankDollarsInfo = new WealthBankDollarsInfo();
        wealthBankDollarsInfo.dollarIndexIds = dollarIndexIds;
        wealthBankDollarsInfo.dollarValueList = dollarValueList;

        //保险箱坐标
        int safeBoxIndex = 0;
        //黄金列车坐标
        int goldTrainIndex = 0;
        for (int i = 1; i < gameRunInfo.getIconArr().length; i++) {
            int icon = gameRunInfo.getIconArr()[i];
            if (icon == WealthBankConstant.BaseElement.ID_SAFE_BOX) {
                safeBoxIndex = i;
            } else if (icon == WealthBankConstant.BaseElement.ID_GOLD_TRAIN) {
                goldTrainIndex = i;
            }
        }

        log.debug("[Wealth Bank] 本局美金信息 dollarCount = {},values = {},safeBoxIndex = {},goldTrainIndex = {}", wealthBankDollarsInfo.dollarIndexIds == null ? 0 : wealthBankDollarsInfo.dollarIndexIds.size(), wealthBankDollarsInfo.dollarValueList, safeBoxIndex, goldTrainIndex);

        //如果盘面中出现美金,且有保险箱，则会触发现金奖励
        wealthBankDollarsInfo.coinIndexId = safeBoxIndex;

        //检查是否收集美元
        if (gameData.getOneBetScore() >= this.wealthBankCollectDollarConfig.getStakeMin()) {
            wealthBankDollarsInfo.collectDollarIndexIds = new ArrayList<>();
            boolean collect = false;
            for (int i = 0; i < wealthBankDollarsInfo.dollarIndexIds.size(); i++) {
                int index = wealthBankDollarsInfo.dollarIndexIds.get(i);
                int icon = lib.getIconArr()[index];
                if (icon != WealthBankConstant.BaseElement.ID_DOLLAR) {
                    continue;
                }

                int rand = RandomUtils.randomMinMax(0, 10000);
                if (rand < this.wealthBankCollectDollarConfig.getProp()) {
                    collect = true;
                    wealthBankDollarsInfo.collectDollarIndexIds.add(index);

                    gameData.addDollasCount(1);
                    log.debug("[Wealth Bank] 收集美元 playerId = {},girdId = {},currentCollectCount = {},totalCount = {}", gameData.playerId(), index, wealthBankDollarsInfo.dollarValueList.size(), gameData.getTotalDollars());
                }
            }

            if (collect) {
                gameData.addDollarsTotalStake(gameData.getOneBetScore());
                log.debug("[Wealth Bank] count = {},avg = {}", gameData.getAddDollarsCount(), gameData.getAddDollarsTotalStake() / gameData.getAddDollarsCount());
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

                        WealthBankTrainInfo goldWealthBankTrainInfo = goldTrainPbInfo(awardInfo.getRandCount(), gameRunInfo.getDollarsGoldTimes() * gameData.getOneBetScore());
                        gameRunInfo.addTrainInfo(goldWealthBankTrainInfo);
                        log.debug("[Wealth Bank] 触发黄金列车 playerId = {},times = {},oneBetScore = {}", gameData.playerId(), times, gameData.getOneBetScore());
                    }
                    break;
                }
            }
        }

        //设置保险箱倍数
        if (wealthBankDollarsInfo.coinIndexId > 0) {
//            gameRunInfo.addBigPoolTimes(gameRunInfo.getDollarsGoldTimes());
            log.debug("[Wealth Bank] 触发保险箱 playerId = {},times = {}", gameData.playerId(), gameRunInfo.getDollarsGoldTimes());
        }

        gameRunInfo.setDollarsInfo(wealthBankDollarsInfo);
        return gameRunInfo;
    }

    /**
     * 将库里面的中将线信息转化为消息
     *
     * @param infoList
     * @param oneBetScore 单线押分值
     * @return
     */
    private List<WealthBankResultLineInfo> transAwardLinePbInfo(List<WealthBankAwardLineInfo> infoList, long oneBetScore) {
        if (infoList == null || infoList.isEmpty()) {
            return null;
        }

        List<WealthBankResultLineInfo> list = new ArrayList<>(infoList.size());
        for (WealthBankAwardLineInfo lineInfo : infoList) {
            WealthBankResultLineInfo wealthBankResultLineInfo = new WealthBankResultLineInfo();
            wealthBankResultLineInfo.id = lineInfo.getId();
            wealthBankResultLineInfo.iconIndexs = getIconIndexsByLineId(lineInfo.getId()).subList(0, lineInfo.getSameCount());
//            wealthBankResultLineInfo.times = lineInfo.getBaseTimes();
            wealthBankResultLineInfo.winGold = oneBetScore * lineInfo.getBaseTimes();
            list.add(wealthBankResultLineInfo);
        }
        return list;
    }

    /**
     * 将库里面的火车信息转化为消息
     *
     * @param bet
     * @return
     */
    private List<WealthBankTrainInfo> transTrainPbInfo(SpecialAuxiliaryInfo specialAuxiliaryInfo, long bet) {
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
//        log.debug("[Wealth Bank] 打印火车奖励 specialAuxiliaryInfo = {}", JSON.toJSONString(specialAuxiliaryInfo));

        List<WealthBankTrainInfo> wealthBankTrainInfoList = new ArrayList<>();

        for (Object object : specialAuxiliaryInfo.getAwardInfos()) {
            SpecialAuxiliaryAwardInfo sa = (SpecialAuxiliaryAwardInfo) object;
            if (sa.getAwardCList() == null || sa.getAwardCList().isEmpty()) {
                continue;
            }

            WealthBankTrainInfo wealthBankTrainInfo = new WealthBankTrainInfo();
            wealthBankTrainInfo.type = trainType;

            wealthBankTrainInfo.goldList = new ArrayList<>();
            sa.getAwardCList().forEach(times -> wealthBankTrainInfo.goldList.add(bet * times));

            wealthBankTrainInfoList.add(wealthBankTrainInfo);
        }
        return wealthBankTrainInfoList;
    }

    private WealthBankTrainInfo goldTrainPbInfo(int goldTrainCount, long gold) {
        WealthBankTrainInfo goldWealthBankTrainInfo = new WealthBankTrainInfo();
        goldWealthBankTrainInfo.type = WealthBankConstant.BaseElement.ID_GOLD_TRAIN;
        goldWealthBankTrainInfo.goldList = new ArrayList<>(goldTrainCount);
        for (int i = 0; i < goldTrainCount; i++) {
            goldWealthBankTrainInfo.goldList.add(gold);
        }
        return goldWealthBankTrainInfo;
    }


    @Override
    public int getGameType() {
        return CoreConst.GameType.WEALTH_BANK;
    }

    @Override
    protected WealthBankResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected WealthBankGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected WealthBankGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected Class<WealthBankPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return WealthBankPlayerGameDataDTO.class;
    }

    /**
     * 计算火车是否中奖池
     *
     * @param playerGameData
     * @param lib
     * @param gameRunInfo
     * @return
     */
    private WealthBankGameRunInfo calTrainReward(WealthBankPlayerGameData playerGameData, WealthBankResultLib lib, WealthBankGameRunInfo gameRunInfo) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return gameRunInfo;
        }

        //先转化程消息结构体
        lib.getSpecialAuxiliaryInfoList().forEach(info -> {
            List<WealthBankTrainInfo> wealthBankTrainInfoList = transTrainPbInfo(info, playerGameData.getOneBetScore());
            if (!wealthBankTrainInfoList.isEmpty()) {
                gameRunInfo.addTrainInfo(wealthBankTrainInfoList);
            }
        });

        if (gameRunInfo.getTrainList() == null || gameRunInfo.getTrainList().isEmpty()) {
            return gameRunInfo;
        }

        for (WealthBankTrainInfo wealthBankTrainInfo : gameRunInfo.getTrainList()) {
            int poolId = getPoolIdByTrain(wealthBankTrainInfo.type);
            if (lib.getJackpotIds() == null || lib.getJackpotIds().isEmpty() || poolId < 1 || !lib.getJackpotIds().contains(poolId)) {
                log.debug("[Wealth Bank] 获取的池子id小于1 trainCoinId = {},poolId = {}", wealthBankTrainInfo.type, poolId);
                continue;
            }
            PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
            if (poolCfg == null) {
                continue;
            }
            //计算奖池金额
            //车厢节数+1，是因为要加上最后一个奖池车厢
            //总的延迟时间
            int allDelayTime = ((wealthBankTrainInfo.goldList.size() + 1) * poolCfg.getDelayTime()) / 1000;
            long addGold = calPoolValue(playerGameData.getOneBetScore(), poolCfg.getGrowthRate(), poolCfg.getFakePoolInitTimes(), poolCfg.getFakePoolMax(), allDelayTime);

            log.debug("[Wealth Bank] 概率计算可以中小奖池 playerId = {},addGold = {}", playerGameData.playerId(), addGold);

            //给玩家加钱
            CommonResult<Player> result = slotsPoolDao.rewardFromSmallPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), addGold, AddType.SLOTS_TRAIN, poolId + "");
            if (!result.success()) {
                log.warn("[Wealth Bank] 从小池子扣除，并给玩家加钱失败 code = {}", result.code);
                break;
            }

            //缓存中奖金额,以便计算玩家贡献金额
            playerGameData.addSmallPoolReward(addGold);
            gameRunInfo.addSmallPoolGold(addGold);
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());
            wealthBankTrainInfo.goldList.add(addGold);
            wealthBankTrainInfo.poolId = poolId;
            log.debug("[Wealth Bank] 该火车中奖，并且加钱成功 playerId = {},addGold = {}", playerGameData.playerId(), addGold);
        }
        return gameRunInfo;
    }

    /**
     * 检查是否触发投资游戏
     *
     * @param playerGameData
     */
    protected WealthBankGameRunInfo checkInvers(WealthBankPlayerGameData playerGameData, WealthBankGameRunInfo gameRunInfo) {
        if (playerGameData.getTotalDollars() < this.wealthBankCollectDollarConfig.getMax()) {
            return gameRunInfo;
        }

        gameRunInfo.setChoosableAreas(getChoosableAreas(playerGameData));
        boolean flag = playerGameData.getInvers().compareAndSet(false, true);
        if (flag) {
            gameRunInfo.setTotalDollars(playerGameData.getTotalDollars());
            playerGameData.setTotalDollars(0);
            log.debug("[Wealth Bank] 美金累计到 {} 个，触发条件 {} 个，触发投资小游戏后清零 playerId = {}", gameRunInfo.getTotalDollars(), this.wealthBankCollectDollarConfig.getMax(), playerGameData.playerId());
        }
        return gameRunInfo;
    }

    /**
     * 获取投资游戏可选择的区域
     *
     * @param playerGameData
     * @return
     */
    private List<Integer> getChoosableAreas(WealthBankPlayerGameData playerGameData) {
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
                filter(cfg -> cfg.getGameType() == this.gameType && cfg.getPlayType() == WealthBankConstant.SpecialPlay.TYPE_COLLECT_DOLLAR).
                findFirst().orElse(null);

        SpecialPlayCfg specialPlayCfg2 = GameDataManager.getSpecialPlayCfgList().stream().
                filter(cfg -> cfg.getGameType() == this.gameType && cfg.getPlayType() == WealthBankConstant.SpecialPlay.TYPE_INVERS_ALL_WIN).
                findFirst().orElse(null);

        String[] arr = specialPlayCfg1.getValue().split(",");

        WealthBankCollectDollarConfig config = new WealthBankCollectDollarConfig();

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

        this.wealthBankCollectDollarConfig = config;


    }

    /**
     * 火车与奖池映射
     *
     * @param train
     * @return
     */
    private int getPoolIdByTrain(int train) {
        if (train == WealthBankConstant.BaseElement.ID_GREEN_TRAIN) {
            return WealthBankConstant.Common.MINI_POOL_ID;
        }
        if (train == WealthBankConstant.BaseElement.ID_BLUE_TRAIN) {
            return WealthBankConstant.Common.MINOR_POOL_ID;
        }
        if (train == WealthBankConstant.BaseElement.ID_PURPLE_TRAIN) {
            return WealthBankConstant.Common.MAJOR_POOL_ID;
        }
        if (train == WealthBankConstant.BaseElement.ID_RED_TRAIN) {
            return WealthBankConstant.Common.GRAND_POOL_ID;
        }
        return 0;
    }


    @Override
    protected void onAutoExitAction(WealthBankPlayerGameData playerGameData, int eventId) {

        if (playerGameData.getStatus() == WealthBankConstant.Status.NOTMAL_ALL_BOARD || playerGameData.getStatus() == WealthBankConstant.Status.GOLD_ALL_BOARD) {
            log.debug("[Wealth Bank] 添加自动投资游戏事件 playerId = {}", playerGameData.playerId());
            autoChooseFreeModelType(playerGameData);
            //检查当前是否处于特殊模式
            if (playerGameData.getStatus() == DollarExpressConstant.Status.ALL_BOARD_FREE) {
                int forCount = playerGameData.getRemainFreeCount().get();
                for (int i = 0; i < forCount; i++) {
                    autoStartGame(playerGameData, playerGameData.getAllBetScore());
                }
            } else if (playerGameData.getStatus() == DollarExpressConstant.Status.ALL_BOARD_TRAIN || playerGameData.getStatus() == WealthBankConstant.Status.ALL_BOARD_GOLD_TRAIN) {
                autoStartGame(playerGameData, playerGameData.getAllBetScore());
            }
        }
        if (playerGameData.getInvers().get()) {
            autoInvest(playerGameData);
            log.debug("[Wealth Bank] 添加自动二选一事件 playerId = {}", playerGameData.playerId());
        }
    }

    public WealthBankCollectDollarConfig getDollarExpressCollectDollarConfig() {
        return wealthBankCollectDollarConfig;
    }

    @Override
    protected WealthBankResultLib afterForbidPoolLib(SpecialResultLibCfg specialResultLibCfg, WealthBankResultLib resultLib) {
        resultLib.setJackpotIds(null);
        return resultLib;
    }
}
