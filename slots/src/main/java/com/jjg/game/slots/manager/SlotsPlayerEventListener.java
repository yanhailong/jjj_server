package com.jjg.game.slots.manager;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.listener.SessionEnterListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.slots.game.dollarexpress.manager.DollarExpressGameManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * @author 11
 * @date 2025/7/24 17:04
 */
@Component
public class SlotsPlayerEventListener implements SessionEnterListener, SessionCloseListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private CoreLogger logger;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private DollarExpressGameManager dollarExpressGameManager;


    @Override
    public void sessionClose(PFSession session) {
        PlayerController playerController = (PlayerController) session.getReference();
        if (playerController == null) {
            log.warn("玩家退出游戏服务器时 playerController 为空,playerId={},sessionId={}", session.getPlayerId(),
                    session.sessionId());
            return;
        }

        boolean exit = dollarExpressGameManager.exit(playerController);
        playerSessionService.offline(playerController.getPlayer(), !exit);
        logger.exitGame(playerController.getPlayer());
    }

    @Override
    public void sessionEnter(PFSession session, long playerId) {
        try {
            session.setPlayerId(playerId);

            PlayerSessionInfo info = playerSessionService.getInfo(playerId);
            if (info == null) {
                log.warn("sessionEnter时 PlayerSessionInfo 为空 playerId = {}", playerId);
                return;
            }

            if (info.getGameType() < 1) {
                log.warn("sessionEnter时 PlayerSessionInfo 中的gameType小于1 playerId = {}", playerId);
                return;
            }

            final PlayerSessionInfo tempInfo = info;

            Player player = playerService.doSave(playerId, p -> {
                p.setGameType(tempInfo.getGameType());
                p.setRoomCfgId(tempInfo.getRoomCfgId());
            });

            info = playerSessionService.enterGameServer(player);

            PlayerController playerController = new PlayerController(session, player);
            session.setReference(playerController);

            logger.enterGame(player, info.getGameType(), info.getRoomCfgId());

            //创建 PlayerGameData
            dollarExpressGameManager.createPlayerGameData(playerController);
            log.info("玩家进入美元快递游戏 playerId = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getRoomCfgId());
        } catch (Exception e) {
            log.error("", e);
        }
    }


}
