package com.jjg.game.slots.game.dollarexpress.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.data.DollarInfo;
import com.jjg.game.slots.game.dollarexpress.constant.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.data.*;
import com.jjg.game.slots.game.dollarexpress.dao.DollarExpressResultLibDao;
import com.jjg.game.slots.game.dollarexpress.generate.DollarExpressGenerate;
import com.jjg.game.slots.game.dollarexpress.pb.DollarsInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import com.jjg.game.slots.sample.bean.BaseRoomCfg;
import com.jjg.game.slots.sample.bean.SpecialResultLibCfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 游戏逻辑处理器
 *
 * @author 11
 * @date 2025/6/11 16:48
 */
@Component
public class DollarExpressGameManager extends AbstractSlotsGameManager<DollarExpressPlayerGameData> {

    @Autowired
    private DollarExpressResultLibDao libDao;

    //进入大奖池的比例
    private BigDecimal toBigPoolProp = new BigDecimal("0.03");
    @Autowired
    private SlotsPoolDao slotsPoolDao;


    public DollarExpressGameManager() {
        super(DollarExpressPlayerGameData.class);
    }

    @Override
    public void init() {
        super.init(CoreConst.GameType.DOLLAR_EXPRESS);
        this.libDao.init(CoreConst.GameType.DOLLAR_EXPRESS);

//        generateLib(100000);
    }

    /**
     * 生成结果库
     *
     * @param count
     */
    public void generateLib(int count) {
        boolean flag = this.generate.compareAndSet(false, true);
        if (!flag) {
            log.debug("当前正在生成结果库，请勿打扰....");
            return;
        }

        log.info("开始生成结果库，预期生成 {} 条", count);
        DollarExpressGenerate dollarExpressGenerate = new DollarExpressGenerate(CoreConst.GameType.DOLLAR_EXPRESS);

        String newDocName = this.libDao.getNewMongoLibName();

        List<DollarExpressResultLib> libList = new ArrayList<>();
        int i = 0;
        int saveCount = 0;

        int expectGenerateCount = count;
        int restCount = Math.min(count, 100);

        while (count > 0) {
            int reduceCount = 1;
            i++;
            try {
                dollarExpressGenerate.generateOne();

                for (Map.Entry<Integer, DollarExpressResultLib> en : dollarExpressGenerate.getBranchLibMap().entrySet()) {
                    libList.add(en.getValue());
                }

                if (libList.size() >= restCount) {
                    saveCount += libDao.batchSave(libList, newDocName);
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
        this.libDao.moveToRedis(newDocName, this.resultLibSectionMap);
        this.generate.compareAndSet(true, false);
        log.info("生成结果库结束，预期 {} 条，成功保存到数据库 {} 条", expectGenerateCount, saveCount);

        this.clearLibEvent = new TimerEvent<>(this, 1, "clearLibEvent").withTimeUnit(TimeUnit.MINUTES);
        this.timerCenter.add(this.clearLibEvent);
    }

    /**
     * 开始游戏
     *
     * @param playerController
     * @param betValue
     * @return
     */
    public DollarExpressGameRunInfo startGame(PlayerController playerController, long betValue) {
        DollarExpressGameRunInfo gameRunInfo = new DollarExpressGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            //获取玩家游戏数据
            DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerController);
            if (playerGameData == null) {
                log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},wareId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getWareId());
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == DollarExpressConstant.Status.NORMAL) {  //正常
                gameRunInfo = normal(playerController, gameRunInfo, playerGameData, betValue);
            } else if (status == DollarExpressConstant.Status.NOTMAL_ALL_BOARD || status == DollarExpressConstant.Status.GOLD_ALL_BOARD) {  //二选一
                gameRunInfo.setCode(Code.FORBID);
                log.debug("当前正处于二选一状态，禁止开始游戏操作 playerId = {},gameType = {},wareId = {}, status = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getWareId(), status);
                return gameRunInfo;
            } else if (status == DollarExpressConstant.Status.ALL_BOARD_TRAIN) {  //二选一之拉火车
                gameRunInfo = allBoardTrain(gameRunInfo, playerGameData);
            } else if (status == DollarExpressConstant.Status.ALL_BOARD_GOLD_TRAIN) {  //二选一之拉黄金火车
                gameRunInfo = allBoardGoldTrain(gameRunInfo, playerGameData);
            } else if (status == DollarExpressConstant.Status.ALL_BOARD_FREE) {  //二选一之免费模式
                gameRunInfo = allBoardFree(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.debug("开始游戏失败，检测到错误状态 playerId = {},gameType = {},wareId = {},status = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getWareId(), status);
                return gameRunInfo;
            }

            //给玩家加钱
            if (gameRunInfo.getAllTimes() > 0) {
                long addGold = playerGameData.getLastBet() * gameRunInfo.getAllTimes();
                if (addGold > 0) {
                    long poolGold = slotsPoolDao.addToPool(this.gameType, playerGameData.getWareId(), -addGold);
                    CommonResult<Player> result = slotsPlayerService.addGold(playerGameData.playerId(), addGold, "SLOTS_BET_REWARD");
                    if (!result.success()) {
                        log.warn("给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                        gameRunInfo.setCode(result.code);
                        return gameRunInfo;
                    }
                    log.debug("玩家添加金币 gameType = {},wareId = {},addGold = {},pool = {}", this.gameType, playerGameData.getWareId(), addGold, poolGold);
                }
            }

            gameRunInfo.setTotalDollars(playerGameData.getTotalDollars());
            gameRunInfo.setStatus(playerGameData.getStatus());
            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    /**
     * 二选一选哪个？？？
     *
     * @param playerController
     * @param chooseStatus
     * @return
     */
    public DollarExpressGameRunInfo chooseFreeGameType(PlayerController playerController, int chooseStatus) {
        DollarExpressGameRunInfo gameRunInfo = new DollarExpressGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            //获取玩家游戏数据
            DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerController);
            if (playerGameData == null) {
                log.debug("获取玩家游戏数据失败，二选一失败 playerId = {},gameType = {},wareId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getWareId());
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            if (playerGameData.getStatus() == DollarExpressConstant.Status.NOTMAL_ALL_BOARD) {  //普通二选一
                if (chooseStatus == DollarExpressConstant.Status.ALL_BOARD_TRAIN) {  //拉普通火车
                    playerGameData.setStatus(DollarExpressConstant.Status.ALL_BOARD_TRAIN);
                    playerGameData.addLastAgainGameIndex();
                } else {
                    playerGameData.setStatus(DollarExpressConstant.Status.ALL_BOARD_FREE);
                    playerGameData.addLastFreeGameIndex();
                }
            } else if (playerGameData.getStatus() == DollarExpressConstant.Status.GOLD_ALL_BOARD) {  //黄金二选一
                if (chooseStatus == DollarExpressConstant.Status.ALL_BOARD_GOLD_TRAIN) {  //拉黄金火车
                    playerGameData.setStatus(DollarExpressConstant.Status.ALL_BOARD_GOLD_TRAIN);
                    playerGameData.addLastAgainGameIndex();
                } else {
                    playerGameData.setStatus(DollarExpressConstant.Status.ALL_BOARD_FREE);
                    playerGameData.addLastFreeGameIndex();
                }
            } else {
                gameRunInfo.setCode(Code.FORBID);
                log.debug("当前不处于二选一状态，禁止二选一操作 playerId = {},gameType = {},wareId = {},status = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getWareId(), playerGameData.getStatus());
                return gameRunInfo;
            }

            log.info("玩家进行二选一，playerId = {},gameType = {},wareId = {},chooseStatus = {}", playerController.playerId(), playerGameData.getGameType(), playerController.getPlayer().getWareId(), chooseStatus);
            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    /**
     * 普通正常流程
     *
     * @param playerController
     * @param gameRunInfo
     * @param playerGameData
     * @param betValue
     * @return
     */
    private DollarExpressGameRunInfo normal(PlayerController playerController, DollarExpressGameRunInfo gameRunInfo, DollarExpressPlayerGameData playerGameData, long betValue) {
        //获取倍场配置
        BaseRoomCfg baseRoomCfg = getBaseRoomCfg(playerController.getPlayer().getWareId());
        if (baseRoomCfg == null) {
            log.warn("获取倍场配置失败 playerId = {},gameType = {},wareId = {},betValue = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getWareId(), betValue);
            gameRunInfo.setCode(Code.NOT_FOUND);
            return gameRunInfo;
        }

        //检查押分是否合法
        boolean match = baseRoomCfg.getLineBetScore().stream().anyMatch(bet -> bet == betValue);
        if (!match) {
            log.warn("押分值不合法 playerId = {},gameType = {},wareId = {},betValue = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getWareId(), betValue);
            gameRunInfo.setCode(Code.PARAM_ERROR);
            return gameRunInfo;
        }

        Player player = slotsPlayerService.get(playerController.playerId());
        if (player.getGold() < betValue) {
            log.debug("玩家余额不足，无法快乐的玩游戏 playerId = {},gameType = {},wareId = {},betValue = {},currentGold = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getWareId(), betValue, player.getGold());
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

        DollarExpressResultLib resultLib = null;
        for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
            //获取倍数区间
            CommonResult<Integer> resultLibSectionResult = getResultLibSection(libCfgResult.data.getModelId(), resultLibTypeResult.data);
            if (!resultLibSectionResult.success()) {
                continue;
            }

            //根据倍数区间从结果库里面随机获取一条
            resultLib = libDao.getLibBySectionIndex(libCfgResult.data.getModelId(), resultLibTypeResult.data, resultLibSectionResult.data);
            if (resultLib == null) {
                log.debug("获取结果库失败 gameType = {},libType = {},sectionIndex = {},retry = {}", this.gameType, resultLibTypeResult.data, resultLibSectionResult.data, i);
                continue;
            }
            break;
        }

        //如果前面没有获取到lib，则获取一个无奖励的结果
        if (resultLib == null) {
            resultLib = libDao.getLibBySectionIndex(libCfgResult.data.getModelId(), resultLibTypeResult.data, this.norRewardSectionIndex);
            log.debug("前面获取结果库失败，所以找一个不中奖的结果返回 gameType = {},libType = {}", this.gameType, resultLibTypeResult.data);
        }

        if (resultLib == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("获取结果库失败 gameType = {},libType = {}", this.gameType, resultLibTypeResult.data);
            return gameRunInfo;
        }

        //给池子加钱
        CommonResult<Player> result = goldToPool(playerGameData, betValue);
        if (!result.success()) {
            gameRunInfo.setCode(result.code);
            return gameRunInfo;
        }

        playerGameData.setLib(resultLib);

        long bet = BigDecimal.valueOf(betValue).divide(BigDecimal.valueOf(baseRoomCfg.getBetCoefficient()), 0, BigDecimal.ROUND_HALF_UP).longValue();
        playerGameData.setLastBet(bet);

        //是否会触发二选一
        if (resultLib.isChooseOne()) {
            if (resultLib.getGoldTrainCount() > 0) {
                playerGameData.setStatus(DollarExpressConstant.Status.GOLD_ALL_BOARD);
            } else {
                playerGameData.setStatus(DollarExpressConstant.Status.NOTMAL_ALL_BOARD);
            }
            playerGameData.setLib(resultLib);
        }

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setAwardLineInfos(resultLib.getAwardLineInfoList());

        gameRunInfo.setDollarsInfo(conversDollarsInfo(playerGameData,resultLib.getIconArr(),resultLib.getDollarInfo(),resultLib.getGoldTrainCount()));
        gameRunInfo.setGoldTrainCount(resultLib.getGoldTrainCount());
        gameRunInfo.setTrainList(resultLib.getTrainList());
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
        Map<Integer, DollarExpressAgainGame> againGameMap = playerGameData.getLib().getAgainGameMap();
        if (againGameMap == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("未在该条结果库中找到重转信息1 gameType = {},libId = {}", this.gameType, playerGameData.getLib().getId());
            return gameRunInfo;
        }
        DollarExpressAgainGame againGame = againGameMap.get(playerGameData.getLastAgainGameIndex());
        if (againGame == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("未在该条结果库中找到重转信息2 gameType = {},libId = {},index = {}", this.gameType, playerGameData.getLib().getId(), playerGameData.getLastAgainGameIndex());
            return gameRunInfo;
        }

        gameRunInfo.setIconArr(againGame.getIconArr());
        gameRunInfo.setTrainList(againGame.getTrainList());

        playerGameData.addLastAgainGameIndex();
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
        Map<Integer, DollarExpressAgainGame> againGameMap = playerGameData.getLib().getAgainGameMap();
        if (againGameMap == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("未在该条结果库中找到重转信息3 gameType = {},libId = {}", this.gameType, playerGameData.getLib().getId());
            return gameRunInfo;
        }
        DollarExpressAgainGame againGame = againGameMap.get(playerGameData.getLastAgainGameIndex());
        if (againGame == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("未在该条结果库中找到重转信息4 gameType = {},libId = {},index = {}", this.gameType, playerGameData.getLib().getId(), playerGameData.getLastAgainGameIndex());
            return gameRunInfo;
        }

        gameRunInfo.setIconArr(againGame.getIconArr());

        gameRunInfo.setDollarsInfo(conversDollarsInfo(playerGameData,againGame.getIconArr(),0,againGame.getDollarTimesList(),againGame.getGoldTrainCount()));

        gameRunInfo.setGoldTrainCount(againGame.getGoldTrainCount());

        playerGameData.addLastAgainGameIndex();
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
        Map<Integer, DollarExpressFreeGame> freeGameMap = playerGameData.getLib().getFreeGameMap();
        if (freeGameMap == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("未在该条结果库中找到免费转信息1 gameType = {},libId = {}", this.gameType, playerGameData.getLib().getId());
            return gameRunInfo;
        }
        DollarExpressFreeGame freeGame = freeGameMap.get(playerGameData.getLastFreeGameIndex());
        if (freeGame == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("未在该条结果库中找到免费转信息2 gameType = {},libId = {},index = {}", this.gameType, playerGameData.getLib().getId(), playerGameData.getLastAgainGameIndex());
            return gameRunInfo;
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setAwardLineInfos(freeGame.getAwardLineInfoList());
        gameRunInfo.setDollarsInfo(conversDollarsInfo(playerGameData,freeGame.getIconArr(),freeGame.getDollarInfo(),freeGame.getGoldTrainCount()));
        gameRunInfo.setGoldTrainCount(freeGame.getGoldTrainCount());
        gameRunInfo.setTrainList(freeGame.getTrainList());

        playerGameData.addLastFreeGameIndex();
        return gameRunInfo;
    }


    /**
     * 给池子加钱
     *
     * @param gameData
     * @param betValue
     * @return
     */
    private CommonResult<Player> goldToPool(DollarExpressPlayerGameData gameData, long betValue) {
        CommonResult<Player> result = slotsPlayerService.addGold(gameData.playerId(), -betValue, "SLOTS_BET");
        if (!result.success()) {
            log.debug("把钱添加到池子失败,扣除玩家金额失败 playerId = {},betValue = {},code = {}", gameData.playerId(), betValue, result.code);
            return result;
        }

        long toPoolGold = BigDecimal.valueOf(betValue).multiply(this.toBigPoolProp).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
        if (toPoolGold < 1) {
            log.debug("把钱添加到池子失败,加入的金额小于1， playerId = {},betValue = {},code = {}", gameData.playerId(), betValue, result.code);
            result.code = Code.FAIL;
            return result;
        }
        long poolCoin = slotsPoolDao.addToPool(this.gameType, gameData.getWareId(), toPoolGold);
        log.debug("当前池子 wareId = {},v = {}", gameData.getWareId(), poolCoin);
        return result;
    }

    /**
     * 转化美元信息
     * @param playerGameData
     * @param iconArr
     * @param dollarCashTimes
     * @param dollarTimesList
     * @param goldTrainCount
     * @return
     */
    private DollarsInfo conversDollarsInfo(DollarExpressPlayerGameData playerGameData, int[] iconArr,int dollarCashTimes,List<Integer> dollarTimesList, int goldTrainCount) {
        if (iconArr == null || iconArr.length == 0) {
            return null;
        }

        DollarsInfo dollarsInfo = new DollarsInfo();

        //检查保险箱
        if (dollarCashTimes > 0) {
            for (int i = 17; i <= 20; i++) {
                if (iconArr[i] == DollarExpressConstant.BaseElement.ID_SAFE_BOX) {
                    dollarsInfo.coinIndexId = i;
                    break;
                }
            }
        }

        //检查美钞
        dollarsInfo.dollarIndexIds = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            if (iconArr[i] == DollarExpressConstant.BaseElement.ID_DOLLAR) {
                dollarsInfo.dollarIndexIds.add(i);
            }
        }
        playerGameData.addDollasCount(dollarsInfo.dollarIndexIds.size());

        //给美金赋值
        if (dollarTimesList != null && !dollarTimesList.isEmpty()) {
            dollarsInfo.dollarValueList = new ArrayList<>();
            for (int times : dollarTimesList) {
                dollarsInfo.dollarValueList.add(playerGameData.getLastBet() * times);
            }
        }

        dollarsInfo.goldTrainCount = goldTrainCount;
        return dollarsInfo;
    }

    /**
     * 转化美元信息
     * @param playerGameData
     * @param iconArr
     * @param dollarInfo
     * @param goldTrainCount
     * @return
     */
    private DollarsInfo conversDollarsInfo(DollarExpressPlayerGameData playerGameData, int[] iconArr,DollarInfo dollarInfo, int goldTrainCount) {
        if (dollarInfo == null || iconArr == null || iconArr.length == 0) {
            return null;
        }

        return conversDollarsInfo(playerGameData,iconArr,dollarInfo,goldTrainCount);
    }

    @Override
    protected DollarExpressResultLibDao getResultLibDao() {
        return this.libDao;
    }
}
