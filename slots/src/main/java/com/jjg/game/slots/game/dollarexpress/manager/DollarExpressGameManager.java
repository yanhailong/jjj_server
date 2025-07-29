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
import com.jjg.game.slots.data.FreeAwardRealData;
import com.jjg.game.slots.data.GirdUpdateConfig;
import com.jjg.game.slots.data.PropInfo;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.DollarExpressLogger;
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
    @Autowired
    private DollarExpressGenerateManager dollarExpressGenerate;
    @Autowired
    private DollarExpressGenerateManager dollarExpressGenerateManager;
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private DollarExpressLogger logger;


    private Map<Integer, GirdUpdateConfig> girdUpdateConfigMap;
    //在替换格子时限制while最大循环次数
    private final int updateGirdWhildMaxCount = 30;


    public DollarExpressGameManager() {
        super(DollarExpressPlayerGameData.class);
    }

    @Override
    public void init() {
        log.info("启动美元快递游戏管理器...");
        this.gameType = CoreConst.GameType.DOLLAR_EXPRESS;
        this.libDao.init(this.gameType);

        //计算配置后缓存
        initConfig();
        this.dollarExpressGenerate.init(this.gameType);
    }

    /**
     * 生成结果库
     *
     * @param count
     */
    public void generateLib(int count) {
        boolean lock = libDao.addGenerateLock(this.gameType);
        if (!lock) {
            log.debug("当前正在生成结果库，请勿打扰....");
            return;
        }

        log.info("开始生成结果库，预期生成 {} 条", count);

        String newDocName = this.libDao.getNewMongoLibName();

        List<DollarExpressResultLib> libList = new ArrayList<>();
        int i = 0;
        int saveCount = 0;

        int expectGenerateCount = count;
        int restCount = Math.min(count, 100);

        while (count > 0) {
            int reduceCount = 0;
            i++;
            try {
                List<DollarExpressResultLib> tempList = dollarExpressGenerate.generateOne();
                reduceCount = tempList.size();

                libList.addAll(tempList);

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
        String redisTableName = this.libDao.moveToRedis(newDocName, this.resultLibSectionMap);
        specialResultLibConfig(this.gameType, false);

        log.info("生成结果库结束，预期 {} 条，成功保存到数据库 {} 条,mongoName = {},redisName = {}", expectGenerateCount, saveCount, newDocName, redisTableName);

        this.clearAllLibEvent = new TimerEvent<>(this, 1, "clearLibEvent").withTimeUnit(TimeUnit.MINUTES);
        this.timerCenter.add(this.clearAllLibEvent);

        //通知其他节点，结果库变更
        noticeNodeLibChange(SlotsConst.LibChangeType.LIB_CHANGE, Collections.EMPTY_LIST);
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

            Player player = null;
            //标准池
            if (gameRunInfo.getBigPoolTimes() > 0) {
                long addGold = playerGameData.getLastBet() * gameRunInfo.getBigPoolTimes();
                if (addGold > 0) {
                    CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), addGold, "SLOTS_BET_REWARD");
                    if (!result.success()) {
                        log.warn("给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                        gameRunInfo.setCode(result.code);
                        return gameRunInfo;
                    }
                    gameRunInfo.setAllWinGold(addGold);
                    player = result.data;
                }
            }

            //小奖池
            if (gameRunInfo.getSmallPoolRewardMap() != null && !gameRunInfo.getSmallPoolRewardMap().isEmpty()) {
                Iterator<Map.Entry<Integer, Long>> it = gameRunInfo.getSmallPoolRewardMap().entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Integer, Long> en = it.next();
                    long addGold = en.getValue();
                    CommonResult<Player> result = slotsPoolDao.rewardFromSmallPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), addGold, "SLOTS_TRAIN_" + en.getKey());
                    if (!result.success()) {
                        log.warn("从小池子给玩家添加金币失败 gameType = {},poolId = {}", this.gameType, en.getKey());
                        it.remove();
                        continue;
                    }
                    player = result.data;
                    gameRunInfo.addAllWinGold(addGold);
                }

                gameRunInfo = addTrainPoolGold(gameRunInfo);
            }

            //添加美元收集进度
            if(gameRunInfo.getTotalDollars() < 1){
                gameRunInfo.setTotalDollars(playerGameData.getTotalDollars());
            }

            //玩家当前金币
            if (player == null) {
                player = slotsPlayerService.get(playerGameData.playerId());
            }
            gameRunInfo.setAfterGold(player.getGold());

            //添加大奖展示id
            int times = (int) (gameRunInfo.getAllWinGold() / playerGameData.getLastStake());
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

            List<Integer> rewardIdList = dollarExpressGenerate.getRewardList(dollarExpressGenerate.getPropAndAwardInfo(dollarExpressGenerate.getDollarExpressCollectDollarConfig().getAuxiliaryId()), playerGameData.getLastModelId());
            //处理奖励逻辑
            InversGoldTrainRewardData data = dollarExpressGenerate.handInversGoldTrainReward(rewardIdList);

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

            //3次中奖金币
            if (allAddGold > 0) {
                CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), allAddGold, "SLOTS_INVEST_REWARD");
                if (!result.success()) {
                    gameRunInfo.setCode(result.code);
                    return gameRunInfo;
                }
                gameRunInfo.addAllWinGold(allAddGold);
                gameRunInfo.setInvestRewardGoldList(rewardGoldList);
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

                log.debug("小地图3次都中奖，添加金火车的金币 gameType = {},roomCfgId = {},addGold = {}", this.gameType, playerGameData.getRoomCfgId(), addGold);
            }

            //检查地图是否全部解锁
            if (playerGameData.areaAllUnlock()) {
                gameRunInfo.setAllAreaUnLock(true);
                playerGameData.getAllUnLock().compareAndSet(false, true);
            }
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
        boolean match = baseRoomCfg.getLineBetScore().stream().anyMatch(bet -> bet == betValue);
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

        long bet = BigDecimal.valueOf(betValue).divide(BigDecimal.valueOf(baseRoomCfg.getBetCoefficient()), 0, BigDecimal.ROUND_HALF_UP).longValue();
        playerGameData.setLastStake(betValue);
        playerGameData.setLastBet(bet);
        gameRunInfo.setBet(bet);

        playerGameData.setLib(resultLib);
        gameRunInfo.setIconArr(resultLib.getIconArr());

        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibType() == SlotsConst.SpecialResultLib.TYPE_ALL_BOARD) {  //是否会触发二选一
            int count = dollarExpressGenerateManager.checkAllBoadrd(resultLib.getIconArr());
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
        CommonResult<int[]> updateResult = updateGird(libCfgResult.data.getModelId(), SlotsConst.BaseElementReward.ROTATESTATE_NORMAL, gameRunInfo.getIconArr(), updateGird);
        if (updateResult.success()) {
            gameRunInfo.setIconArr(updateResult.data);
        }
        //检查与美元相关的逻辑
        gameRunInfo = checkDorllar(gameRunInfo, playerGameData, resultLib.getGoldTrainCount(), resultLib.getGoldTrainAllTimes());

        //计算火车奖励
        gameRunInfo = calTrainReward(playerGameData, resultLib.getTrainList(), gameRunInfo);
        //添加火车信息
        gameRunInfo.setTrainList(transTrainPbInfo(resultLib.getTrainList(), gameRunInfo.getBet()));

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

        playerGameData.setTrainLib(trainLib);

        gameRunInfo.setBet(playerGameData.getLastBet());
        gameRunInfo.setStatus(playerGameData.getStatus());

        playerGameData.setStatus(DollarExpressConstant.Status.NORMAL);

        gameRunInfo.setIconArr(trainLib.getIconArr());

        //计算火车奖励
        gameRunInfo = calTrainReward(playerGameData, trainLib.getTrainList(), gameRunInfo);
        //添加火车信息
        gameRunInfo.setTrainList(transTrainPbInfo(trainLib.getTrainList(), gameRunInfo.getBet()));

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

        playerGameData.setGoldTrainlib(goldTrainLib);

        log.debug("成功获取黄金列车结果库 playerId = {},libId = {}", playerGameData.playerId(), goldTrainLib.getId());

        gameRunInfo.setStatus(playerGameData.getStatus());

        playerGameData.setStatus(DollarExpressConstant.Status.NORMAL);
        gameRunInfo.setIconArr(goldTrainLib.getIconArr());

        //格子修改
        CommonResult<int[]> updateResult = updateGird(playerGameData.getLastModelId(), SlotsConst.BaseElementReward.ROTATESTATE_AGAIN, goldTrainLib.getIconArr(), updateGird);
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
        playerGameData.setFreeLib(freeLib);
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
            log.debug("未在该条结果库中找到免费转信息2 gameType = {},libId = {}", this.gameType, playerGameData.getLib().getId());
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
        gameRunInfo.setTrainList(transTrainPbInfo(freeGame.getTrainList(), gameRunInfo.getBet()));
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
            CommonResult<Integer> result = getResultLibSection(playerGameData.getLastModelId(), SlotsConst.SpecialResultLib.TYPE_GOLD_TRAIN);
            if (!result.success()) {
                continue;
            }
            //获取结果库
            goldTrainLib = libDao.getLibBySectionIndex(playerGameData.getLastModelId(), SlotsConst.SpecialResultLib.TYPE_GOLD_TRAIN, result.data);
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
        CommonResult<int[]> updateResult = updateGird(playerGameData.getLastModelId(), SlotsConst.BaseElementReward.ROTATESTATE_AGAIN, goldTrainLib.getIconArr(), updateGird);
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
            log.debug("给小池子加钱成功 gameType = {},roomCfgId = {},add = {},afterGold = {}", gameData.getGameType(), gameData.getRoomCfgId(), toSmallPoolGold, poolCoin);
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
    public CommonResult<int[]> updateGird(int modelId, int spinStatus, int[] arr, boolean update) {
        CommonResult<int[]> result = new CommonResult<>(Code.FAIL);
        if (this.specialGirdCfgMap == null || this.specialGirdCfgMap.isEmpty()) {
            return result;
        }

        log.debug("开始修改格子 modelId = {},spinStaus = {},update = {}", modelId, spinStatus, update);
        int[] newArr = null;
        if (update) {
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
                GirdUpdateConfig config = this.girdUpdateConfigMap.get(cfg.getId());
                //随机次数
                int randCount = config.getRandCountPropInfo().getRandKey();
                if (randCount < 1) {
                    continue;
                }
                //原盘面中剩余的可被替换的图标
                Map<Integer, Integer> canReplaceMap = new HashMap<>();

                //去掉不替换的元素
                for (int i = 1; i < arr.length; i++) {
                    int icon = arr[i];
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

                    //先随机一列
                    int columnId = cloumnPropInfo.getRandKey();
                    int index = (columnId - 1) * dollarExpressGenerate.getBaseInitCfg().getRows() + 1;
                    //计算这一列的坐标
                    for (int j = 0; j < dollarExpressGenerate.getBaseInitCfg().getRows(); j++) {
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
                    int times = dollarExpressGenerate.randDollarTimes();
                    if (dollarsInfo.dollarValueList == null) {
                        dollarsInfo.dollarValueList = new ArrayList<>();
                    }
                    dollarsInfo.dollarValueList.add(times * gameData.getLastBet());
                    gameRunInfo.addDollarsGoldTimes(times);
                }
            } else if (icon == DollarExpressConstant.BaseElement.ID_SAFE_BOX) {
                safeBoxIndex = i;
            } else if(icon == DollarExpressConstant.BaseElement.ID_GOLD_TRAIN){
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

        log.debug("当前美金数量 count = {},values = {},allDollarTimes = {}",dollarsInfo.dollarIndexIds == null ? 0 : dollarsInfo.dollarIndexIds.size(), dollarsInfo.dollarValueList,gameRunInfo.getDollarsGoldTimes());

        //如果盘面中出现美金,且有保险箱，则会触发现金奖励
        if(dollarsInfo.dollarIndexIds != null && !dollarsInfo.dollarIndexIds.isEmpty()) {
            dollarsInfo.coinIndexId = safeBoxIndex;
        }

        //检查是否收集美元
        if (gameData.getLastStake() >= dollarExpressGenerate.getDollarExpressCollectDollarConfig().getStakeMin() && dollarsInfo.dollarIndexIds != null && dollarsInfo.dollarValueList != null) {
            dollarsInfo.collectDollarIndexIds = new ArrayList<>();
            boolean collect = false;
            for (int i = 0; i < dollarsInfo.dollarIndexIds.size(); i++) {
                int index = dollarsInfo.dollarIndexIds.get(i);
                int rand = RandomUtils.randomMinMax(0, 10000);
                if (rand < dollarExpressGenerate.getDollarExpressCollectDollarConfig().getProp()) {
                    collect = true;
                    dollarsInfo.collectDollarIndexIds.add(index);

                    gameData.addDollasCount(1);
//                    gameRunInfo.addTotalDollars(1);
                    log.debug("收集美元 playerId = {},currentCollectCount = {},totalCount = {}", gameData.playerId(), dollarsInfo.dollarValueList.size(), gameData.getTotalDollars());
                }
            }

            if (collect) {
                gameData.addDollarsTotalStake(gameData.getLastStake());
                log.debug("count = {},avg = {}", gameData.getAddDollarsCount(), gameData.getAddDollarsTotalStake() / gameData.getAddDollarsCount());
            }
        }

        //设置黄金列车倍数
        if(goldTrainIndex > 0 && dollarsInfo.dollarIndexIds != null && !dollarsInfo.dollarIndexIds.isEmpty()){
            if(goldTrainCount < 1){
                goldTrainCount = dollarExpressGenerate.getGoldTrainCount();
                if(goldTrainCount < 1){
                    log.warn("获取的 goldTrainCount 小于1");
                }
            }

            int times = goldTrainCount * gameRunInfo.getDollarsGoldTimes();
            gameRunInfo.addBigPoolTimes(times);

            TrainInfo goldTrainInfo = goldTrainPbInfo(goldTrainCount, gameRunInfo.getDollarsGoldTimes() * gameData.getLastBet());
            gameRunInfo.addTrainInfo(goldTrainInfo);
            log.debug("添加黄金列车倍数 playerId = {},times = {},lastBet = {}", gameData.playerId(), times, gameData.getLastBet());
        }

        //设置保险箱倍数
        if (dollarsInfo.coinIndexId > 0 && gameRunInfo.getDollarsGoldTimes() > 0) {
            gameRunInfo.addBigPoolTimes(gameRunInfo.getDollarsGoldTimes());
            log.debug("添加保险箱倍数 playerId = {},times = {}", gameData.playerId(), gameRunInfo.getDollarsGoldTimes());
        }

        gameRunInfo.setDollarsInfo(dollarsInfo);
        return gameRunInfo;
    }

    @Override
    protected DollarExpressResultLibDao getResultLibDao() {
        return this.libDao;
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
            if (cfg.getGirdUpdateType() != SlotsConst.SpecialGird.GIRD_UPDATE_TYPE_APPOINT) {
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

        boolean match = trainList.stream().anyMatch(t -> t.getPoolId() > 0);
        if (!match) {
            return gameRunInfo;
        }

        //获取玩家累计贡献金额
        long contribt = playerGameData.getAllContribtPoolGold(playerGameData.getRoomCfgId());
        if (contribt < 1) {
            return gameRunInfo;
        }

        log.debug("玩家累计贡献金额 playerId = {},contribtGold = {}", playerGameData.playerId(), contribt);

        //真奖池
        Number smallPoolNumber = slotsPoolDao.getSmallPoolByRoomCfgId(playerGameData.getGameType(), playerGameData.getRoomCfgId());
        if (smallPoolNumber == null) {
            log.debug("获取小池子金额为空 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
            return gameRunInfo;
        }
        //假奖池
        Number fakeSmallPoolNumber = slotsPoolDao.getFakeSmallPoolByRoomCfgId(playerGameData.getGameType(), playerGameData.getRoomCfgId());
        if (fakeSmallPoolNumber == null) {
            log.debug("获取(假)小池子金额为空 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
            return gameRunInfo;
        }

        //当真奖池 >= 假奖池时，才允许中奖
        long smallPool = smallPoolNumber.longValue();
        long fakeSmallPool = fakeSmallPoolNumber.longValue();

        if (smallPool < fakeSmallPool) {
            log.debug("真奖池小于假奖池，不允许中奖 playerId = {},roomCfgId = {},smallPool = {},fakeSmallPoolNumber = {}", playerGameData.playerId(), playerGameData.getRoomCfgId(), smallPool, fakeSmallPool);
            return gameRunInfo;
        }

        BigDecimal pool = BigDecimal.valueOf(smallPoolNumber.longValue());

        for (Train t : trainList) {
            PoolCfg poolCfg = GameDataManager.getPoolCfg(t.getPoolId());
            //中奖概率,这里保留了8位，所以最后可以取int值
            int propV = BigDecimal.valueOf(contribt).divide(pool, 8, BigDecimal.ROUND_HALF_UP).divide(BigDecimal.valueOf(poolCfg.getPoolProp()), 8, BigDecimal.ROUND_HALF_UP).multiply(oneHundredMillionBigDecimal).intValue();
            int rand = RandomUtils.randomInt(oneHundredMillion);
            if (rand >= propV) {
                continue;
            }
            //奖池初始金额
            BigDecimal initPoolBigDecimal = BigDecimal.valueOf(playerGameData.getLastStake());
            //间隔时间
            BigDecimal intervalTime = BigDecimal.valueOf(poolCfg.getGrowthRate().get(0));
            //增加万分比
            BigDecimal prop = BigDecimal.valueOf(poolCfg.getGrowthRate().get(1)).divide(tenThousandBigDecimal, 4, RoundingMode.HALF_UP);

            //循环时间
            BigDecimal circulTimeBigDecimal = BigDecimal.ONE.divide(prop, 4, RoundingMode.HALF_UP).multiply(intervalTime);
            //余数y
            int y = TimeHelper.getNowDaySeconds() % circulTimeBigDecimal.intValue();
            //实际金额
            BigDecimal step1 = BigDecimal.valueOf(y).divide(intervalTime, 4, RoundingMode.HALF_UP);
            BigDecimal step2 = step1.multiply(prop);
            BigDecimal step3 = step2.multiply(initPoolBigDecimal);
            BigDecimal addGold = initPoolBigDecimal.add(step3);

            gameRunInfo.addSmallPoolReward(t.getPoolId(), addGold.longValue());
            log.debug("概率计算可以中小奖池 playerId = {},rand = {},propV = {},intervalTime = {},y = {},addGold = {}", playerGameData.playerId(), rand, propV, intervalTime, y, addGold);
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
                lib = dollarExpressGenerate.checkAward(testLibData.getIcons(), lib).get(0);
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
        if (playerGameData.getTotalDollars() < dollarExpressGenerate.getDollarExpressCollectDollarConfig().getMax()) {
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
            playerGameData.setTotalDollars(playerGameData.getTotalDollars() - dollarExpressGenerate.getDollarExpressCollectDollarConfig().getMax());
            log.debug("美金数量达到 {} 个，触发投资小游戏 playerId = {}", dollarExpressGenerate.getDollarExpressCollectDollarConfig().getMax(), playerGameData.playerId());
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
     * 将库里面的火车信息转化为消息
     *
     * @param trainList
     * @param bet
     * @return
     */
    private List<TrainInfo> transTrainPbInfo(List<Train> trainList, long bet) {
        if (trainList == null || trainList.isEmpty()) {
            return null;
        }

        List<TrainInfo> trainInfoList = new ArrayList<>(trainList.size());
        for (Train t : trainList) {
            TrainInfo trainInfo = new TrainInfo();
            trainInfo.type = t.getTrainIconId();
            if (t.getCoachs() != null && !t.getCoachs().isEmpty()) {
                trainInfo.goldList = new ArrayList<>(t.getCoachs().size());
                for (int[] arr : t.getCoachs()) {
                    if (arr[1] < DollarExpressConstant.Common.MINI_POOL_ID) {
                        trainInfo.goldList.add(bet * arr[1]);
                    }
                }
            }
            trainInfo.poolId = t.getPoolId();
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

    private DollarExpressGameRunInfo addTrainPoolGold(DollarExpressGameRunInfo gameRunInfo) {
        if (gameRunInfo.getTrainList() == null || gameRunInfo.getTrainList().isEmpty()) {
            return gameRunInfo;
        }

        for (TrainInfo trainInfo : gameRunInfo.getTrainList()) {
            if (trainInfo.poolId < 1) {
                continue;
            }
            long gold = gameRunInfo.getGoldByPoolId(trainInfo.poolId);
            if (gold < 0) {
                continue;
            }
            if (trainInfo.goldList == null) {
                trainInfo.goldList = new ArrayList<>();
            }
            trainInfo.goldList.add(gold);
        }
        return gameRunInfo;
    }

    @Override
    public void shutdown() {
        log.info("正在关闭美元快递游戏管理器");
    }
}
