package com.jjg.game.slots.game.acedj.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.acedj.AceDjConstant;
import com.jjg.game.slots.game.acedj.dao.AceDjGameDataDao;
import com.jjg.game.slots.game.acedj.dao.AceDjResultLibDao;
import com.jjg.game.slots.game.acedj.data.AceDjGameRunInfo;
import com.jjg.game.slots.game.acedj.data.AceDjPlayerGameData;
import com.jjg.game.slots.game.acedj.data.AceDjPlayerGameDataDTO;
import com.jjg.game.slots.game.acedj.data.AceDjResultLib;
import com.jjg.game.slots.logger.SlotsLogger;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractAceDjGameManager extends AbstractSlotsGameManager<AceDjPlayerGameData, AceDjResultLib, AceDjGameRunInfo> {
    @Autowired
    protected AceDjResultLibDao libDao;
    @Autowired
    protected AceDjGenerateManager generateManager;
    @Autowired
    protected SlotsLogger logger;
    @Autowired
    protected AceDjGameDataDao gameDataDao;

    public AbstractAceDjGameManager() {
        super(AceDjPlayerGameData.class, AceDjResultLib.class, AceDjGameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("启动王牌Dj游戏管理器...");
        super.init();
    }

    @Override
    public AceDjGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        AceDjPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new AceDjGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        if (playerGameData.getStatus() == AceDjConstant.Status.FREE &&
                (playerGameData.getFreeLib() == null || playerGameData.getRemainFreeCount().get() <= 0)) {
            playerGameData.setStatus(AceDjConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.setFreeIndex(new AtomicInteger(0));
            playerGameData.setRemainFreeCount(new AtomicInteger(0));
            log.info("王牌Dj玩家状态异常，重置为正常状态,状态为{}, playerId = {}", playerGameData.getStatus(), playerController.playerId());
        }

        AceDjGameRunInfo gameRunInfo = new AceDjGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }

    /**
     * 开始游戏
     *
     * @param playerController
     * @param playerGameData
     * @param auto
     * @return
     */
    @Override
    public AceDjGameRunInfo startGame(PlayerController playerController, AceDjPlayerGameData playerGameData, long betValue, boolean auto) {
        AceDjGameRunInfo gameRunInfo = new AceDjGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == AceDjConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == AceDjConstant.Status.FREE) {
                gameRunInfo = free(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
                return gameRunInfo;
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(player, gameRunInfo.getAllWinGold(), betValue, warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData, betValue);
            log.debug("计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            //系统自动玩的游戏，不会走跑马灯
            if (!auto) {
                checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            }
            gameRunInfo.setData(playerGameData);
        } catch (Exception e) {
            log.error("", e);
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
    @Override
    protected AceDjGameRunInfo normal(AceDjGameRunInfo gameRunInfo, AceDjPlayerGameData playerGameData, long betValue, AceDjResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(AceDjConstant.SpecialMode.FREE)) {  //是否会触发免费
            playerGameData.setStatus(AceDjConstant.Status.FREE);
            int againFreeCount = 0;
            int allCount = 0;
            for (SpecialAuxiliaryInfo info : resultLib.getSpecialAuxiliaryInfoList()) {
                for (JSONObject json : info.getFreeGames()) {
                    Integer addFreeCount = json.getInteger("addFreeCount");
                    if (addFreeCount != null && addFreeCount > 0) {
                        againFreeCount += addFreeCount;
                    }
                }
                allCount += info.getFreeGames().size();
            }
            //设置添加的免费次数
            int addCount = allCount - againFreeCount;
            playerGameData.setRemainFreeCount(new AtomicInteger(addCount));

            long times = generateManager.calLineTimes(resultLib.getAwardLineInfoList());
            times += generateManager.calAfterAddIcons(resultLib.getAddIconInfos());

            playerGameData.setFreeLib(resultLib);

            gameRunInfo.addBigPoolTimes(times);
            log.debug("触发免费模式  playerId = {},libId = {},status = {},addFreeCount = {},times = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus(), addCount, times);
        } else {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }

        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));

        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(AceDjConstant.Status.NORMAL);
        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    protected AceDjGameRunInfo free(AceDjGameRunInfo gameRunInfo, AceDjPlayerGameData playerGameData) {
        CommonResult<AceDjResultLib> libResult = freeGetLib(playerGameData, AceDjConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        AceDjResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
        }

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        if (afterCount < 1) {
            playerGameData.setStatus(AceDjConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);

            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(AceDjConstant.Status.FREE);
        return gameRunInfo;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.ACE_DJ;
    }

    @Override
    protected AceDjResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected AceDjGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected AceDjGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected Class<AceDjPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return AceDjPlayerGameDataDTO.class;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭王牌Dj游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected void onAutoExitAction(AceDjPlayerGameData gameData, int eventId) {
//        if (gameData.getStatus() == AceDjConstant.Status.FREE) {
//            freeStateAction(gameData, (playerGameData) ->
//                    startGame(new PlayerController(null, null), playerGameData, playerGameData.getAllBetScore(), true));
//        }
    }
}
