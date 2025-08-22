package com.jjg.game.slots.game.mahjiongwin.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.game.mahjiongwin.MahjiongWinLogger;
import com.jjg.game.slots.game.mahjiongwin.dao.MahjiongWinGameDataDao;
import com.jjg.game.slots.game.mahjiongwin.dao.MahjiongWinResultLibDao;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinGameRunInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinPlayerGameData;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 麻将胡了游戏逻辑处理器
 * @author 11
 * @date 2025/8/1 17:25
 */
@Component
public class MahjiongWinGameManager extends AbstractSlotsGameManager<MahjiongWinPlayerGameData> {
    @Autowired
    private MahjiongWinResultLibDao libDao;
    @Autowired
    private MajiongWinGenerateManager generateManager;
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private MahjiongWinLogger logger;
    @Autowired
    private MahjiongWinGameDataDao gameDataDao;

    public MahjiongWinGameManager() {
        super(MahjiongWinPlayerGameData.class);
    }

    @Override
    public void init() {
        log.info("启动麻将胡了游戏管理器...");
        this.libDao.init(this.gameType);
    }

    /**
     * 玩家开始游戏
     * @param playerController
     * @param stake
     * @return
     */
    public MahjiongWinGameRunInfo playerStartGame(PlayerController playerController,long stake){
        return null;
    }

    @Override
    public void shutdown() {
        try{
            super.shutdown();
            log.info("已关闭麻将胡了游戏管理器");
        }catch (Exception e){
            log.error("",e);
        }
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.MAHJIONG_WIN;
    }

    @Override
    protected void offlineSaveGameDataDto(MahjiongWinPlayerGameData gameData) {

    }
}
