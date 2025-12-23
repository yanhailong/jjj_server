package com.jjg.game.slots.game.goldsnakefortune.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.goldsnakefortune.dao.GoldSnakeFortuneGameDataDao;
import com.jjg.game.slots.game.goldsnakefortune.dao.GoldSnakeFortuneResultLibDao;
import com.jjg.game.slots.game.goldsnakefortune.data.GoldSnakeFortuneGameRunInfo;
import com.jjg.game.slots.game.goldsnakefortune.data.GoldSnakeFortunePlayerGameData;
import com.jjg.game.slots.game.goldsnakefortune.data.GoldSnakeFortuneResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractGoldSnakeFortuneGameManager extends AbstractSlotsGameManager<GoldSnakeFortunePlayerGameData, GoldSnakeFortuneResultLib> {
    @Autowired
    protected GoldSnakeFortuneResultLibDao libDao;
    @Autowired
    protected GoldSnakeFortuneGenerateManager generateManager;
    @Autowired
    protected GoldSnakeFortuneGameDataDao gameDataDao;

    public AbstractGoldSnakeFortuneGameManager() {
        super(GoldSnakeFortunePlayerGameData.class, GoldSnakeFortuneResultLib.class);
    }

    /**
     * 玩家开始游戏
     *
     * @param playerController
     * @param stake
     * @return
     */
    public GoldSnakeFortuneGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        GoldSnakeFortunePlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new GoldSnakeFortuneGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, stake, false);
    }

    /**
     * 开始游戏
     *
     * @param playerController
     * @param playerGameData
     * @param stake
     * @return
     */
    protected GoldSnakeFortuneGameRunInfo startGame(PlayerController playerController, GoldSnakeFortunePlayerGameData playerGameData, long stake, boolean auto) {
        GoldSnakeFortuneGameRunInfo gameRunInfo = new GoldSnakeFortuneGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(player.getGold());

            //todo

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), stake);

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(player.getGold());

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData, stake);
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
    protected GoldSnakeFortuneResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected GoldSnakeFortuneGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected GoldSnakeFortuneGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected void offlineSaveGameDataDto(GoldSnakeFortunePlayerGameData gameData) {

    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.GOLD_SNAKE_FORTUNE;
    }
}
