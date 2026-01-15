package com.jjg.game.slots.game.hulk.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.hulk.dao.HulkGameDataDao;
import com.jjg.game.slots.game.hulk.dao.HulkResultLibDao;
import com.jjg.game.slots.game.hulk.data.HulkGameRunInfo;
import com.jjg.game.slots.game.hulk.data.HulkPlayerGameData;
import com.jjg.game.slots.game.hulk.data.HulkPlayerGameDataDTO;
import com.jjg.game.slots.game.hulk.data.HulkResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author 11
 * @date 2026/1/15
 */
public abstract class AbstractHulkGameManager extends AbstractSlotsGameManager<HulkPlayerGameData, HulkResultLib> {
    @Autowired
    protected HulkResultLibDao libDao;
    @Autowired
    protected HulkGenerateManager generateManager;
    @Autowired
    protected HulkGameDataDao gameDataDao;


    public AbstractHulkGameManager() {
        super(HulkPlayerGameData.class, HulkResultLib.class);
    }

    @Override
    public void init() {

    }

    @Override
    public HulkGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        HulkPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new HulkGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        HulkGameRunInfo gameRunInfo = new HulkGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }

    /**
     * 玩家开始游戏
     *
     * @param playerController
     * @param stake
     * @return
     */
    public HulkGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        HulkPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new HulkGameRunInfo(Code.NOT_FOUND, playerController.playerId());
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
    protected HulkGameRunInfo startGame(PlayerController playerController, HulkPlayerGameData playerGameData, long stake, boolean auto) {
        HulkGameRunInfo gameRunInfo = new HulkGameRunInfo(Code.SUCCESS, playerGameData.playerId());

        return gameRunInfo;
    }

    @Override
    protected HulkResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected HulkGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected HulkGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected Class<HulkPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return HulkPlayerGameDataDTO.class;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.HULK;
    }
}
