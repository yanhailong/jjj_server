package com.jjg.game.slots.game.wolfmoon.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;
import com.jjg.game.slots.game.wolfmoon.dao.WolfMoonGameDataDao;
import com.jjg.game.slots.game.wolfmoon.dao.WolfMoonResultLibDao;
import com.jjg.game.slots.game.wolfmoon.data.*;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
public abstract class AbstractWolfMoonGameManager extends AbstractSlotsGameManager<WolfMoonPlayerGameData, WolfMoonResultLib, WolfMoonGameRunInfo> {
    @Autowired
    protected WolfMoonGenerateManager generateManager;
    @Autowired
    protected WolfMoonResultLibDao wolfMoonResultLibDao;
    @Autowired
    protected WolfMoonGameDataDao gameDataDao;

    public AbstractWolfMoonGameManager() {
        super(WolfMoonPlayerGameData.class, WolfMoonResultLib.class, WolfMoonGameRunInfo.class);
    }

    @Override
    public void init() {
//        log.info("启动狼月游戏管理器...");
//        super.init();
//        addUpdatePoolEvent();
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
    protected WolfMoonGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected WolfMoonGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected Class<? extends SlotsPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return WolfMoonPlayerGameDataDTO.class;
    }

    @Override
    public WolfMoonGameRunInfo enterGame(PlayerController playerController) throws Exception {
        WolfMoonPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败 playerId = {}", playerController.playerId());
            return createGameRunInfo(playerController.playerId(), Code.NOT_FOUND);
        }

        WolfMoonGameRunInfo gameRunInfo = createGameRunInfo(playerController.playerId(), Code.SUCCESS);

        return gameRunInfo;
    }

    // ==================== 开始游戏 ====================

    @Override
    public WolfMoonGameRunInfo startGame(PlayerController playerController, WolfMoonPlayerGameData playerGameData, long betValue, boolean auto) {
        WolfMoonGameRunInfo gameRunInfo = new WolfMoonGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());


            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    @Override
    protected WolfMoonGameRunInfo normal(WolfMoonGameRunInfo gameRunInfo, WolfMoonPlayerGameData playerGameData, long betValue, WolfMoonResultLib resultLib) {


        return gameRunInfo;
    }

}
