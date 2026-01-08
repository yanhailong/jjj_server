package com.jjg.game.slots.game.luckymouse.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.game.luckymouse.dao.LuckyMouseGameDataDao;
import com.jjg.game.slots.game.luckymouse.dao.LuckyMouseResultLibDao;
import com.jjg.game.slots.game.luckymouse.data.LuckyMousePlayerGameData;
import com.jjg.game.slots.game.luckymouse.data.LuckyMousePlayerGameDataDTO;
import com.jjg.game.slots.game.luckymouse.data.LuckyMouseResultLib;
import com.jjg.game.slots.logger.SlotsLogger;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

public class AbstractLuckyMouseGameManager extends AbstractSlotsGameManager<LuckyMousePlayerGameData, LuckyMouseResultLib> {
    @Autowired
    private LuckyMouseResultLibDao libDao;
    @Autowired
    private LuckyMouseGenerateManager generateManager;
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private SlotsLogger logger;
    @Autowired
    private LuckyMouseGameDataDao gameDataDao;

    public AbstractLuckyMouseGameManager() {
        super(LuckyMousePlayerGameData.class, LuckyMouseResultLib.class);
    }

    @Override
    public void init() {

    }

    @Override
    protected LuckyMouseResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected LuckyMouseGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected LuckyMouseGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected Class<LuckyMousePlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return LuckyMousePlayerGameDataDTO.class;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.LUCKY_MOUSE;
    }
}
