package com.jjg.game.room.listener;

import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.listener.SessionEnterListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/18 13:28
 */
@Component
public class PlayerEventListener implements SessionEnterListener, SessionCloseListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private CoreLogger logger;

    private Map<Integer, IPlayerRoomEventListener> roomListenerMap = new HashMap<>();
    
    public void init(){
        Map<String, IPlayerRoomEventListener> listenerMap = CommonUtil.getContext().getBeansOfType(IPlayerRoomEventListener.class);
        for(Map.Entry<String, IPlayerRoomEventListener> en : listenerMap.entrySet()){
            int[] arr = en.getValue().getGameTypes();
            if(arr != null && arr.length > 0){
                for(int gameType : arr){
                    roomListenerMap.put(gameType,en.getValue());
                }
            }
        }
    }

    @Override
    public void sessionClose(PFSession session) {
        PlayerController playerController = (PlayerController) session.getReference();
        if (playerController == null) {
            log.warn("玩家退出游戏服务器时 playerController 为空,playerId={},sessionId={}", session.getPlayerId(), session.sessionId());
            return;
        }

        PlayerSessionInfo info = playerSessionService.getInfo(playerController.playerId());
        if(info == null){
            log.warn("玩家退出游戏服务器时 PlayerSessionInfo 为空 playerId = {}", playerController.playerId());
            return;
        }

        if(info.getGameType() < 1){
            log.warn("玩家退出游戏服务器时 PlayerSessionInfo 中的gameType小于1 playerId = {}", playerController.playerId());
            return;
        }

        IPlayerRoomEventListener playerRoomEventListener = roomListenerMap.get(info.getGameType());
        if(playerRoomEventListener == null){
            log.warn("玩家退出游戏服务器时未找到 playerRoomEventListener, playerId = {},gameType = {}", playerController.playerId(),info.getGameType());
            return;
        }

        playerRoomEventListener.exit(session,playerController,info);

        playerSessionService.offline(playerController.playerId(),0,0,0,0);
        logger.exitGame(playerController.player, info.getGameType());
    }

    @Override
    public void sessionEnter(PFSession session, long playerId) {
        try{
            session.setPlayerId(playerId);

            PlayerSessionInfo info = playerSessionService.getInfo(playerId);
            if(info == null){
                log.warn("sessionEnter时 PlayerSessionInfo 为空 playerId = {}", playerId);
                return;
            }

            if(info.getGameType() < 1){
                log.warn("sessionEnter时 PlayerSessionInfo 中的gameType小于1 playerId = {}", playerId);
                return;
            }

            IPlayerRoomEventListener playerRoomEventListener = roomListenerMap.get(info.getGameType());
            if(playerRoomEventListener == null){
                log.warn("sessionEnter时 未找到 playerRoomEventListener, playerId = {},gameType = {}", playerId,info.getGameType());
                return;
            }

            info = playerSessionService.enterGameServer(playerId, info.getGameType(), info.getWareId());
            Player player = playerService.get(playerId);
            PlayerController playerController = new PlayerController(session, player);
            session.setReference(playerController);

            logger.enterGame(player, info.getGameType(),info.getWareId());
            playerRoomEventListener.enter(session,playerController,info);
        }catch (Exception e){
            log.error("",e);
        }
    }
}
