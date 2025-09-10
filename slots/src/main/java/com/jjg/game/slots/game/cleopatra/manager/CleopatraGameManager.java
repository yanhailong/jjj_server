package com.jjg.game.slots.game.cleopatra.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.cleopatra.data.CleopatraGameRunInfo;
import com.jjg.game.slots.game.cleopatra.data.CleopatraPlayerGameData;
import com.jjg.game.slots.game.cleopatra.data.CleopatraResultLib;
import com.jjg.game.slots.game.mahjiongwin.dao.MahjiongWinGameDataDao;
import com.jjg.game.slots.game.mahjiongwin.dao.MahjiongWinResultLibDao;
import com.jjg.game.slots.game.mahjiongwin.manager.MahjiongWinGenerateManager;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 麻将胡了游戏逻辑处理器
 * @author 11
 * @date 2025/8/1 17:25
 */
@Component
public class CleopatraGameManager extends AbstractSlotsGameManager<CleopatraPlayerGameData, CleopatraResultLib> {
    @Autowired
    private MahjiongWinResultLibDao libDao;
    @Autowired
    private MahjiongWinGenerateManager generateManager;
    @Autowired
    private MahjiongWinGameDataDao gameDataDao;

    public CleopatraGameManager() {
        super(CleopatraPlayerGameData.class,CleopatraResultLib.class);
    }

    @Override
    public void init() {
        log.info("启动埃及艳后游戏管理器...");
        this.libDao.init(this.gameType);
    }

    /**
     * 玩家开始游戏
     * @param playerController
     * @param stake
     * @return
     */
    public CleopatraGameRunInfo playerStartGame(PlayerController playerController, long stake){
        return null;
    }



    @Override
    protected MahjiongWinResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected MahjiongWinGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected MahjiongWinGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.MAHJIONG_WIN;
    }

    @Override
    protected void offlineSaveGameDataDto(CleopatraPlayerGameData gameData) {

    }

    @Override
    public void shutdown() {
        try{
            super.shutdown();
            log.info("已关闭埃及艳后游戏管理器");
        }catch (Exception e){
            log.error("",e);
        }
    }
}
