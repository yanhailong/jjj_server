package com.jjg.game.dollarexpress.listener;

import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;
import com.jjg.game.dollarexpress.data.PlayerGameData;
import com.jjg.game.dollarexpress.manager.DollarExpressManager;
import com.jjg.game.dollarexpress.manager.DollarExpressSendMessageManager;
import com.jjg.game.room.listener.IPlayerRoomEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/10 18:02
 */
@Component
public class DollarExpressRoomEventListener implements IPlayerRoomEventListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private DollarExpressManager dollarExpressManager;
    @Autowired
    private DollarExpressSendMessageManager dollarExpressSendMessageManager;

    @Override
    public int[] getGameTypes() {
        return DollarExpressConst.GameType.SUPPORT_GAME_TYPES;
    }

    @Override
    public void enter(PFSession session, PlayerController playerController, PlayerSessionInfo playerSessionInfo) {
        //创建 PlayerGameData
        dollarExpressManager.createPlayerGameData(playerController,playerSessionInfo);
        //推送配置信息
        dollarExpressSendMessageManager.sendConfigMessage(playerController, playerSessionInfo.getWareId());
        log.info("玩家进入美元快递游戏服务器,sessionId={},playerId={},wareId = {}", session.sessionId(), playerController.playerId(),playerSessionInfo.getWareId());
    }

    @Override
    public void exit(PFSession session,PlayerController playerController, PlayerSessionInfo playerSessionInfo) {
        log.info("玩家退出美元快递游戏服务器,sessionId={},playerId={}", session.sessionId(), playerController.playerId());
        PlayerGameData playerGameData = (PlayerGameData)playerController.getScene();
        if(playerGameData != null){
//            gameController.gainScore(true);
        }
    }
}
