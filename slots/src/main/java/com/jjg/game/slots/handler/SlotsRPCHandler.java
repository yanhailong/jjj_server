package com.jjg.game.slots.handler;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.rpc.GmToSlotsBridge;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import com.jjg.game.slots.manager.SlotsFactoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/1/28
 */
@Component
public class SlotsRPCHandler implements GmToSlotsBridge {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SlotsFactoryManager slotsFactoryManager;

    @Override
    public int cleanStatus(long playerId, int gameType, int roomCfgId) {
        try {
            AbstractSlotsGameManager gameManager = slotsFactoryManager.getGameManager(gameType, roomCfgId);
            if (gameManager == null) {
                log.warn("清除游戏状态时，未获取到gameManager，playerId = {},gameType = {},roomCfgId = {}", playerId, gameType, roomCfgId);
                return Code.NOT_FOUND;
            }

            gameManager.cleanStatus(playerId, roomCfgId);
            log.info("玩家清除游戏状态 playerId = {},gameType = {},roomCfgId = {}", playerId, gameType, roomCfgId);
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("", e);
            return Code.EXCEPTION;
        }
    }
}
