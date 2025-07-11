package com.jjg.game.slots.listener;

import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.room.listener.IPlayerRoomEventListener;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressPlayerGameData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/27 9:34
 */
@Component
public class SlotsRoomEventListener implements IPlayerRoomEventListener{
    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public int[] getGameTypes() {
//        return SlotsConst.GameType.SUPPORT_GAME_TYPES;
        return new int[]{};
    }

    @Override
    public void enter(PFSession session, PlayerController playerController, PlayerSessionInfo playerSessionInfo) {
        //创建 PlayerGameData
//        dollarExpressManager.createPlayerGameData(playerController,playerSessionInfo);
        //推送配置信息
//        dollarExpressSendMessageManager.sendConfigMessage(playerController, playerSessionInfo.getWareId());
        log.info("玩家进入slots游戏服务器,sessionId={},playerId={},wareId = {}", session.sessionId(), playerController.playerId(),playerSessionInfo.getWareId());
    }

    @Override
    public void exit(PFSession session,PlayerController playerController, PlayerSessionInfo playerSessionInfo) {
        log.info("玩家退出slots游戏服务器,sessionId={},playerId={}", session.sessionId(), playerController.playerId());
        DollarExpressPlayerGameData dollarExpressPlayerGameData = (DollarExpressPlayerGameData)playerController.getScene();
        if(dollarExpressPlayerGameData != null){
//            gameController.gainScore(true);
        }
    }
}
