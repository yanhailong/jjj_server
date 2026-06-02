package com.jjg.game.slots.game.bountyduel.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.game.bountyduel.BountyDuelConstant;
import com.jjg.game.slots.game.bountyduel.dao.BountyDuelResultLibDao;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelGameRunInfo;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelPlayerGameData;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractBountyDuelGameManager extends AbstractSlotsGameManager<BountyDuelPlayerGameData, BountyDuelResultLib, BountyDuelGameRunInfo> {
    @Autowired
    protected BountyDuelResultLibDao libDao;
    @Autowired
    protected BountyDuelGenerateManager generateManager;

    public AbstractBountyDuelGameManager() {
        super(BountyDuelPlayerGameData.class, BountyDuelResultLib.class, BountyDuelGameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("启动赏金大对决游戏管理器...");
        super.init();
    }

    @Override
    public BountyDuelGameRunInfo startGame(PlayerController playerController, BountyDuelPlayerGameData playerGameData, long betValue, boolean auto) {
        BountyDuelGameRunInfo gameRunInfo = new BountyDuelGameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        try {
            gameRunInfo.setAuto(auto);

            Player player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            int status = playerGameData.getStatus();
            if (status == BountyDuelConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == BountyDuelConstant.Status.FREE) {
                gameRunInfo = free(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId={}, gameType={}", playerController.playerId(), playerController.getPlayer().getGameType());
                return gameRunInfo;
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            rewardFromBigPool(gameRunInfo, playerGameData);
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            triggerWinTask(playerController.getPlayer(), gameRunInfo, playerGameData, warehouseCfg.getTransactionItemId());

            player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);
            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            int times = calWinTimes(gameRunInfo, playerGameData);
            log.debug("计算出获奖倍数 times={}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

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
    protected BountyDuelGameRunInfo normal(BountyDuelGameRunInfo gameRunInfo, BountyDuelPlayerGameData playerGameData,
                                           long betValue, BountyDuelResultLib resultLib) {
        if (resultLib.getLibTypeSet().contains(BountyDuelConstant.SpecialMode.FREE)) {
            playerGameData.setStatus(BountyDuelConstant.Status.FREE);
            playerGameData.setRemainFreeCount(new AtomicInteger(resultLib.getAddFreeCount()));
            long times = generateManager.calLineTimes(resultLib.getAwardLineInfoList());
            times += generateManager.calAfterAddIcons(resultLib.getAddIconInfos());
            playerGameData.setFreeLib(resultLib);
            gameRunInfo.addBigPoolTimes(times);
            log.debug("触发免费模式 playerId={}, libId={}, status={}, addFreeCount={}, times={}",
                    playerGameData.getPlayerId(), resultLib.getId(), playerGameData.getStatus(), resultLib.getAddFreeCount(), times);
        } else {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }

        log.debug("id={}", resultLib.getId());

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(BountyDuelConstant.Status.NORMAL);
        return gameRunInfo;
    }

    protected BountyDuelGameRunInfo free(BountyDuelGameRunInfo gameRunInfo, BountyDuelPlayerGameData playerGameData) {
        CommonResult<BountyDuelResultLib> libResult = freeGetLib(playerGameData, BountyDuelConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        BountyDuelResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount={}, afterCount={}", freeGame.getAddFreeCount(), afterCount);
        }

        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        if (afterCount < 1) {
            playerGameData.setStatus(BountyDuelConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);

            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            log.debug("免费模式结束，回到普通状态 playerId={}, roomCfgId={}", playerGameData.getPlayerId(), playerGameData.getRoomCfgId());
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(BountyDuelConstant.Status.FREE);
        return gameRunInfo;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.BOUNTY_DUEL;
    }

    @Override
    protected BountyDuelResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected BountyDuelGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("关闭赏金大对决游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
