package com.jjg.game.slots.game.wolfmoon.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.SlotsResultLib;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;
import com.jjg.game.slots.game.wolfmoon.dao.WolfMoonResultLibDao;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonGameRunInfo;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonPlayerGameData;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
public abstract class AbstractWolfMoonGameManager extends AbstractSlotsGameManager<WolfMoonPlayerGameData, WolfMoonResultLib, WolfMoonGameRunInfo> {
    private final WolfMoonGenerateManager generateManager;
    private final WolfMoonResultLibDao wolfMoonResultLibDao;
    private final Map<Integer, Integer> modelChoose = Map.of(1, WolfMoonConstant.SpecialMode.FREE_HIGH_PAY,
            2, WolfMoonConstant.SpecialMode.FREE_FIXED_STACKED_WILD,
            3, WolfMoonConstant.SpecialMode.FREE_INCREASING_MULTIPLIER);

    public AbstractWolfMoonGameManager(WolfMoonGenerateManager generateManager, WolfMoonResultLibDao wolfMoonResultLibDao) {
        super(WolfMoonPlayerGameData.class, WolfMoonResultLib.class, WolfMoonGameRunInfo.class);
        this.generateManager = generateManager;
        this.wolfMoonResultLibDao = wolfMoonResultLibDao;
    }

    @Override
    public void init() {
        log.info("启动狼月游戏管理器...");
        super.init();
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.WOLF_MOON;
    }

    @Override
    protected WolfMoonResultLibDao getResultLibDao() {
        return this.wolfMoonResultLibDao;
    }

    @Override
    protected WolfMoonGenerateManager getGenerateManager() {
        return this.generateManager;
    }


    @Override
    public WolfMoonGameRunInfo startGame(PlayerController playerController, WolfMoonPlayerGameData playerGameData, long betValue, boolean auto) {
        WolfMoonGameRunInfo gameRunInfo = new WolfMoonGameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        try {
            gameRunInfo.setAuto(auto);
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            if (warehouseCfg == null) {
                gameRunInfo.setCode(Code.SAMPLE_ERROR);
                return gameRunInfo;
            }
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == WolfMoonConstant.Status.NORMAL) {  //正常
                normal(gameRunInfo, playerGameData, betValue);
            } else if (status == WolfMoonConstant.Status.FREE) {  //免费模式
                free(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.debug("开始游戏失败，检测到错误状态 playerId = {},gameType = {},roomCfgId = {},status = {}", playerGameData.getPlayerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                return gameRunInfo;
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo, playerGameData, warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData);
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

    @Override
    protected WolfMoonGameRunInfo normal(WolfMoonGameRunInfo gameRunInfo, WolfMoonPlayerGameData playerGameData, long betValue, WolfMoonResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(WolfMoonConstant.SpecialMode.FREE_CHOOSE)) {  //是否会触发二选一
            playerGameData.setStatus(WolfMoonConstant.Status.FREE_CHOOSE);
            log.debug("触发二选一  playerId = {},libId = {},status = {}", playerGameData.getPlayerId(), resultLib.getId(), playerGameData.getStatus());
        }
        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());
        gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(playerGameData.getStatus());
        return gameRunInfo;

    }

    /**
     * 免费模式二选一
     *
     * @param playerController 玩家控制器
     * @param chooseType       免费游戏类型 1-高赔付符号 2-固定堆叠百搭符号 3-递增奖励倍数
     */
    public WolfMoonGameRunInfo freeChooseOne(PlayerController playerController, int chooseType) {
        //获取玩家游戏数据
        WolfMoonPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，二选一失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new WolfMoonGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        if (playerGameData.getStatus() != WolfMoonConstant.Status.FREE_CHOOSE) {
            log.debug("玩家当前不处于二选一状态，二选一失败 playerId = {},gameType = {},roomCfgId = {},status = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), playerGameData.getStatus());
            return new WolfMoonGameRunInfo(Code.FORBID, playerController.playerId());
        }
        int model = modelChoose.getOrDefault(chooseType, 0);
        if (model <= 0) {
            return new WolfMoonGameRunInfo(Code.PARAM_ERROR, playerController.playerId());
        }
        //根据选择生成结果库
        CommonResult<WolfMoonResultLib> libResult = getLibFromDB(playerGameData, model);
        if (!libResult.success()) {
            return new WolfMoonGameRunInfo(libResult.code, playerController.playerId());
        }
        playerGameData.setFreeLib(libResult.data);
        playerGameData.setRemainFreeCount(new AtomicInteger(libResult.data.getAddFreeCount()));
        playerGameData.setStatus(WolfMoonConstant.Status.FREE);
        playerGameData.setFreeGameType(chooseType);
        WolfMoonGameRunInfo wolfMoonGameRunInfo = new WolfMoonGameRunInfo(Code.SUCCESS, playerController.playerId());
        wolfMoonGameRunInfo.setData(playerGameData);
        return wolfMoonGameRunInfo;
    }

    @Override
    protected void resetFreeState(WolfMoonPlayerGameData gameData) {
        gameData.setFreeLib(null);
        gameData.setFreeIndex(new AtomicInteger(0));
        gameData.setRemainFreeCount(new AtomicInteger(0));
        gameData.setFreeAllWin(0);
        gameData.setFreeGameType(0);
    }

    /**
     * 免费模式获取结果库
     *
     */
    private CommonResult<WolfMoonResultLib> freeGetLib(WolfMoonPlayerGameData playerGameData) {
        CommonResult<WolfMoonResultLib> result = new CommonResult<>(Code.SUCCESS);
        log.debug("开始获取免费结果库 playerId = {}", playerGameData.getPlayerId());

        SlotsResultLib<?> freeLib = playerGameData.getFreeLib();

        if (freeLib == null) {
            log.warn("未在该条结果库中找到免费转信息 gameType = {},modelId = {}", this.gameType, playerGameData.getLastModelId());
            result.code = Code.NOT_FOUND;
            return result;
        }

        if (freeLib.getSpecialAuxiliaryInfoList() == null || freeLib.getSpecialAuxiliaryInfoList().isEmpty()) {
            log.warn("未在该条结果库中找到免费转信息1 gameType = {},libId = {}", this.gameType, freeLib.getId());
            result.code = Code.NOT_FOUND;
            return result;
        }

        log.debug("找到免费旋转的结果库 libId = {}", freeLib.getId());

        //找到结果库中免费游戏的结果
        SpecialAuxiliaryInfo specialAuxiliaryInfo = null;
        for (SpecialAuxiliaryInfo tmpInfo : freeLib.getSpecialAuxiliaryInfoList()) {
            if (tmpInfo.getFreeGames() == null || tmpInfo.getFreeGames().isEmpty()) {
                continue;
            }
            specialAuxiliaryInfo = tmpInfo;
            break;
        }

        if (specialAuxiliaryInfo == null) {
            log.warn("未在该条结果库中找到免费转信息2 gameType = {},libId = {}", this.gameType, freeLib.getId());
            result.code = Code.NOT_FOUND;
            return result;
        }

        int index = playerGameData.getFreeIndex().getAndAdd(1);
        JSONObject jsonObject = specialAuxiliaryInfo.getFreeGames().get(index);
        log.debug("获取免费游戏的下标 index = {},allLen = {}", index, specialAuxiliaryInfo.getFreeGames().size());
        WolfMoonResultLib freeGame = JSON.parseObject(jsonObject.toJSONString(), this.libClass);

        if (freeGame == null) {
            log.warn("未在该条结果库中找到免费转信息3 gameType = {},libId = {}", this.gameType, freeLib.getId());
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
     * 免费游戏
     *
     */
    private void free(WolfMoonGameRunInfo gameRunInfo, WolfMoonPlayerGameData playerGameData) {
        SlotsResultLib<?> freeLib = playerGameData.getFreeLib();
        if (freeLib == null) {
            endFreeAction(playerGameData);
            gameRunInfo.setCode(Code.NOT_FOUND);
            return;
        }
        CommonResult<WolfMoonResultLib> libResult = freeGetLib(playerGameData);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return;
        }

        WolfMoonResultLib freeGame = libResult.data;
        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
        }

        gameRunInfo.setCurrentMultiplier(freeGame.getBaseMultiple());
        if(generateManager.getFreeAddCfg() != null){
            gameRunInfo.setCurrentMultiplier(generateManager.getFreeAddCfg().getFirst() + freeGame.getBaseMultiple());
        }

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(playerGameData.getStatus());
        if (afterCount == 0) {
            endFreeAction(playerGameData);
        }
    }


    private void endFreeAction(WolfMoonPlayerGameData playerGameData) {
        playerGameData.setStatus(WolfMoonConstant.Status.NORMAL);
        resetFreeState(playerGameData);
        log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.getPlayerId(), playerGameData.getRoomCfgId());
    }
}
