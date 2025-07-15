package com.jjg.game.slots.game.dollarexpress.listener;

import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.room.listener.IPlayerRoomEventListener;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.game.dollarexpress.constant.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.manager.DollarExpressGameManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/7/11 10:21
 */
@Component
public class DollarExpressRoomEventListener implements IPlayerRoomEventListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private DollarExpressGameManager gameManager;

    @Override
    public int[] getGameTypes() {
        return DollarExpressConstant.GameType.SUPPORT_GAME_TYPES;
    }

    @Override
    public void enter(PFSession session, PlayerController playerController, PlayerSessionInfo playerSessionInfo) {
        //创建 PlayerGameData
        gameManager.createPlayerGameData(playerController);
        log.info("玩家进入美元快递游戏 playerId = {},wareId = {}", playerSessionInfo.getPlayerId(), playerController.player.getWareId());
    }

    @Override
    public void exit(PFSession session, PlayerController playerController, PlayerSessionInfo playerSessionInfo) {

    }
}
