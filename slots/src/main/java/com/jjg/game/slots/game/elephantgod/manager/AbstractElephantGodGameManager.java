package com.jjg.game.slots.game.elephantgod.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.elephantgod.ElephantGodConstant;
import com.jjg.game.slots.game.elephantgod.dao.ElephantGodGameDataDao;
import com.jjg.game.slots.game.elephantgod.dao.ElephantGodResultLibDao;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodGameRunInfo;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodPlayerGameData;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodPlayerGameDataDTO;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;

public abstract class AbstractElephantGodGameManager extends AbstractSlotsGameManager<ElephantGodPlayerGameData, ElephantGodResultLib, ElephantGodGameRunInfo> {
    private final ElephantGodResultLibDao libDao;
    private final ElephantGodGenerateManager generateManager;
    private final ElephantGodGameDataDao gameDataDao;

    public AbstractElephantGodGameManager(ElephantGodResultLibDao libDao, ElephantGodGenerateManager generateManager, ElephantGodGameDataDao gameDataDao) {
        super(ElephantGodPlayerGameData.class, ElephantGodResultLib.class, ElephantGodGameRunInfo.class);
        this.libDao = libDao;
        this.generateManager = generateManager;
        this.gameDataDao = gameDataDao;
    }


    @Override
    protected ElephantGodGameRunInfo startGame(PlayerController playerController, ElephantGodPlayerGameData playerGameData, long betValue, boolean auto) {
        ElephantGodGameRunInfo gameRunInfo = new ElephantGodGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == ElephantGodConstant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, betValue);
            } else if (status == ElephantGodConstant.Status.FREE) {
                free(gameRunInfo, playerGameData);
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

    /**
     * 免费游戏
     *
     */
    protected void free(ElephantGodGameRunInfo gameRunInfo, ElephantGodPlayerGameData playerGameData) {
        CommonResult<ElephantGodResultLib> libResult = freeGetLib(playerGameData, ElephantGodConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return;
        }
        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);
        ElephantGodResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
        }
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());
        //累计免费模式的中奖金额
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        if (afterCount == 0) {
            playerGameData.setStatus(ElephantGodConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        }
        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(playerGameData.getStatus());
    }


    @Override
    protected ElephantGodGameRunInfo normal(ElephantGodGameRunInfo gameRunInfo, ElephantGodPlayerGameData playerGameData, long betValue, ElephantGodResultLib resultLib) {
        return null;
    }

    @Override
    public void init() {
        log.info("启动象财神游戏管理器...");
        super.init();
    }


    @Override
    protected ElephantGodResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected ElephantGodGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected ElephantGodGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected Class<? extends SlotsPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return ElephantGodPlayerGameDataDTO.class;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.ELEPHANT_GOD;
    }
}
