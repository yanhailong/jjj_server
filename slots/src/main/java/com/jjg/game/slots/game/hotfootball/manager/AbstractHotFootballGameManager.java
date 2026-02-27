package com.jjg.game.slots.game.hotfootball.manager;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.hotfootball.HotFootballConstant;
import com.jjg.game.slots.game.hotfootball.dao.HotFootballGameDataDao;
import com.jjg.game.slots.game.hotfootball.dao.HotFootballResultLibDao;
import com.jjg.game.slots.game.hotfootball.data.HotFootballGameRunInfo;
import com.jjg.game.slots.game.hotfootball.data.HotFootballPlayerGameData;
import com.jjg.game.slots.game.hotfootball.data.HotFootballPlayerGameDataDTO;
import com.jjg.game.slots.game.hotfootball.data.HotFootballResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractHotFootballGameManager extends AbstractSlotsGameManager<HotFootballPlayerGameData, HotFootballResultLib, HotFootballGameRunInfo> {
    @Autowired
    protected HotFootballResultLibDao libDao;
    @Autowired
    protected HotFootballGenerateManager generateManager;
    @Autowired
    protected HotFootballGameDataDao gameDataDao;

    public AbstractHotFootballGameManager() {
        super(HotFootballPlayerGameData.class, HotFootballResultLib.class, HotFootballGameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("启动火热足球游戏管理器...");
        super.init();
    }

    @Override
    public HotFootballGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        HotFootballPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new HotFootballGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        resetFreeStateIfInvalid(playerGameData, HotFootballConstant.Status.FREE, HotFootballConstant.Status.NORMAL, "火热足球");

        HotFootballGameRunInfo gameRunInfo = new HotFootballGameRunInfo(Code.SUCCESS, playerGameData.playerId());
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
    public HotFootballGameRunInfo startGame(PlayerController playerController, HotFootballPlayerGameData playerGameData, long betValue, boolean auto) {
        HotFootballGameRunInfo gameRunInfo = new HotFootballGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == HotFootballConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == HotFootballConstant.Status.FREE) {
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
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), playerGameData.getAllBetScore(), warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
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
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    @Override
    protected HotFootballGameRunInfo normal(HotFootballGameRunInfo gameRunInfo, HotFootballPlayerGameData playerGameData, long betValue, HotFootballResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(HotFootballConstant.SpecialMode.FREE)) {  //是否会触发免费
            playerGameData.setStatus(HotFootballConstant.Status.FREE);
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

        log.debug("id = {}", resultLib.getId());

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(HotFootballConstant.Status.NORMAL);
        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    protected HotFootballGameRunInfo free(HotFootballGameRunInfo gameRunInfo, HotFootballPlayerGameData playerGameData) {
        CommonResult<HotFootballResultLib> libResult = freeGetLib(playerGameData, HotFootballConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        HotFootballResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
        }

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        if (afterCount < 1) {
            playerGameData.setStatus(HotFootballConstant.Status.NORMAL);
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
        gameRunInfo.setStatus(HotFootballConstant.Status.FREE);
        return gameRunInfo;
    }

    @Override
    protected void onAutoExitAction(HotFootballPlayerGameData gameData, int eventId) {
//        if (gameData.getStatus() == HotFootballConstant.Status.FREE) {
//            freeStateAction(gameData, (playerGameData) ->
//                    startGame(new PlayerController(null, null), playerGameData, playerGameData.getAllBetScore(), true));
//        }
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.HOT_FOOTBALL;
    }

    @Override
    protected HotFootballResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected HotFootballGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected HotFootballGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected Class<? extends SlotsPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return HotFootballPlayerGameDataDTO.class;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭火热足球游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
