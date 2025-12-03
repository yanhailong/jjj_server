package com.jjg.game.slots.game.thor.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinGameRunInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinPlayerGameData;
import com.jjg.game.slots.game.thor.dao.ThorGameDataDao;
import com.jjg.game.slots.game.thor.dao.ThorResultLibDao;
import com.jjg.game.slots.game.thor.data.ThorGameRunInfo;
import com.jjg.game.slots.game.thor.data.ThorPlayerGameData;
import com.jjg.game.slots.game.thor.data.ThorResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/12/1 18:01
 */
@Component
public class ThorGameManager extends AbstractSlotsGameManager<ThorPlayerGameData, ThorResultLib> {

    @Autowired
    private ThorResultLibDao libDao;
    @Autowired
    private ThorGenerateManager generateManager;
    @Autowired
    private ThorGameDataDao gameDataDao;

    public ThorGameManager() {
        super(ThorPlayerGameData.class, ThorResultLib.class);
        this.log = LoggerFactory.getLogger(getClass());
    }


    /**
     * 玩家开始游戏
     *
     * @param playerController
     * @param stake
     * @return
     */
    public ThorGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        ThorPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new ThorGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, stake, false);
    }

    /**
     * 开始游戏
     * @param playerController
     * @param playerGameData
     * @param stake
     * @param isFree
     * @return
     */
    private ThorGameRunInfo startGame(PlayerController playerController, ThorPlayerGameData playerGameData, long stake, boolean isFree) {
        return null;
    }




















    @Override
    protected ThorResultLibDao getResultLibDao() {
        return libDao;
    }

    @Override
    protected ThorGameDataDao getGameDataDao() {
        return gameDataDao;
    }

    @Override
    protected ThorGenerateManager getGenerateManager() {
        return generateManager;
    }

    @Override
    protected void offlineSaveGameDataDto(ThorPlayerGameData gameData) {

    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.THOR;
    }
}
