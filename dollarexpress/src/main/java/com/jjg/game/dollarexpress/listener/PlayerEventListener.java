package com.jjg.game.dollarexpress.listener;

import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.listener.SessionEnterListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.dollarexpress.data.PlayerGameData;
import com.jjg.game.dollarexpress.logger.DollarExpressLogger;
import com.jjg.game.dollarexpress.manager.DollarExpressManager;
import com.jjg.game.dollarexpress.service.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/10 18:02
 */
@Component
public class PlayerEventListener implements SessionEnterListener, SessionCloseListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private PlayerService playerService;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private DollarExpressManager dollarExpressManager;
    @Autowired
    private DollarExpressLogger logger;

    @Override
    public void sessionClose(PFSession session) {
        log.info("玩家退出美元快递游戏服务器,sessionId={},playerId={}", session.sessionId(), session.getPlayerId());
        PlayerController playerController = (PlayerController) session.getReference();
        if (playerController == null) {
            return;
        }

        PlayerGameData playerGameData = (PlayerGameData)playerController.getScene();
        if(playerGameData != null){
//            gameController.gainScore(true);
        }

        playerSessionService.offline(playerController.player,true,null,false);
        logger.exitGame(playerController.player,playerController.player.getGameType());
    }

    @Override
    public void sessionEnter(PFSession session, long playerId) {
        try{
            session.setPlayerId(playerId);

            Player player = playerService.get(playerId);

            PlayerController playerController = new PlayerController(session, player);
            session.setReference(playerController);

            //更新session
            playerSessionService.enterGameServer(player,false,null);

            //创建 PlayerGameData
            dollarExpressManager.createPlayerGameData(playerController);

            logger.enterGame(player,player.getGameType());
            log.info("玩家进入美元快递游戏服务器,sessionId={},playerId={}", session.sessionId(), playerId);
        }catch (Exception e){
            log.error("",e);
        }
    }
}
