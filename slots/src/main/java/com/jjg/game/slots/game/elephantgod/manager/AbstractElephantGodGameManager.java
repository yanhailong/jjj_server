package com.jjg.game.slots.game.elephantgod.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.elephantgod.dao.ElephantGodGameDataDao;
import com.jjg.game.slots.game.elephantgod.dao.ElephantGodResultLibDao;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodGameRunInfo;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodPlayerGameData;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodPlayerGameDataDTO;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractElephantGodGameManager extends AbstractSlotsGameManager<ElephantGodPlayerGameData, ElephantGodResultLib, ElephantGodGameRunInfo> {
    @Autowired
    private ElephantGodResultLibDao libDao;
    @Autowired
    private ElephantGodGenerateManager generateManager;
    @Autowired
    private ElephantGodGameDataDao gameDataDao;

    @Override
    protected ElephantGodGameRunInfo startGame(PlayerController playerController, ElephantGodPlayerGameData playerGameData, long betValue, boolean auto) {
        return null;
    }

    @Override
    protected ElephantGodGameRunInfo normal(ElephantGodGameRunInfo gameRunInfo, ElephantGodPlayerGameData playerGameData, long betValue, ElephantGodResultLib resultLib) {
        return null;
    }

    @Override
    public void init() {

    }

    public AbstractElephantGodGameManager() {
        super(ElephantGodPlayerGameData.class, ElephantGodResultLib.class, ElephantGodGameRunInfo.class);
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
    protected Class<ElephantGodPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return ElephantGodPlayerGameDataDTO.class;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.ELEPHANT_GOD;
    }
}
