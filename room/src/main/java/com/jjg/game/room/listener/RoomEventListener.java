package com.jjg.game.room.listener;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.listener.SessionEnterListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.manager.AbstractRoomManager;
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
public class RoomEventListener implements SessionEnterListener, SessionCloseListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private CoreLogger logger;
    @Autowired
    private ClusterSystem clusterSystem;

    private AbstractRoomManager roomManager;

    private Map<Integer, IPlayerRoomEventListener> roomListenerMap = new HashMap<>();

    public void init() {
        Map<String, IPlayerRoomEventListener> listenerMap =
            CommonUtil.getContext().getBeansOfType(IPlayerRoomEventListener.class);
        for (Map.Entry<String, IPlayerRoomEventListener> en : listenerMap.entrySet()) {
            int[] arr = en.getValue().getGameTypes();
            if (arr != null && arr.length > 0) {
                for (int gameType : arr) {
                    roomListenerMap.put(gameType, en.getValue());
                }
            }
        }

        Map<String, AbstractRoomManager> roomManagerMap =
            CommonUtil.getContext().getBeansOfType(AbstractRoomManager.class);
        for (Map.Entry<String, AbstractRoomManager> en : roomManagerMap.entrySet()) {
            this.roomManager = en.getValue();
            break;
        }
    }

    @Override
    public void sessionClose(PFSession session) {
        PlayerController playerController = (PlayerController) session.getReference();
        if (playerController == null) {
            log.warn("玩家退出游戏服务器时 playerController 为空,playerId={},sessionId={}", session.getPlayerId(),
                session.sessionId());
            return;
        }

        PlayerSessionInfo info = playerSessionService.getInfo(playerController.playerId());
        if (info == null) {
            log.warn("玩家退出游戏服务器时 PlayerSessionInfo 为空 playerId = {}", playerController.playerId());
            return;
        }

        if (info.getGameType() < 1) {
            log.warn("玩家退出游戏服务器时 PlayerSessionInfo 中的gameType小于1 playerId = {}", playerController.playerId());
            return;
        }

        IPlayerRoomEventListener playerRoomEventListener = roomListenerMap.get(info.getGameType());
        if (playerRoomEventListener == null) {
            log.warn("玩家退出游戏服务器时未找到 playerRoomEventListener, playerId = {},gameType = {}",
                playerController.playerId(), info.getGameType());
            return;
        }

        playerRoomEventListener.exit(session, playerController, info);

        playerSessionService.offline(playerController.playerId(), 0, 0, 0, 0);
        // 调用房间Controller的offline消息
        Object scene = playerController.getScene();
        if (scene instanceof AbstractRoomController<?, ?> abstractRoomController) {
            // 房间进入断线流程，
            // TODO 考虑保存玩家的数据在内存中，如果有自动托管逻辑，可以使用LRU保存一定的玩家。如果没有需要判断玩家当前处于具体游戏的哪个阶段
            //  ，是否在需要完成整局再退出房间
            abstractRoomController.playerOffline(playerController);
        }
        logger.exitGame(playerController.getPlayer(), info.getGameType());
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

            IPlayerRoomEventListener playerRoomEventListener = roomListenerMap.get(info.getGameType());
            if (playerRoomEventListener == null) {
                log.warn("sessionEnter时 未找到 playerRoomEventListener, playerId = {},gameType = {}", playerId,
                    info.getGameType());
                return;
            }

            final PlayerSessionInfo tempInfo = info;

            Player player = playerService.doSave(playerId, p -> {
                p.setGameType(tempInfo.getGameType());
                p.setWareId(tempInfo.getRoomCfgId());
            });

            info = playerSessionService.enterGameServer(info, player.getRoomId());

            PlayerController playerController = new PlayerController(session, player);
            session.setReference(playerController);

            logger.enterGame(player, info.getGameType(), info.getRoomCfgId());

            if (player.getRoomId() > 0) {
                // 设置workId
                session.setWorkId(player.getRoomId());
                int code = roomManager.joinRoom(playerController, info.getGameType(), player.getRoomId());
                if (code == Code.SUCCESS) {
                    return;
                }
            }
            playerRoomEventListener.enter(session, playerController, info);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public void exitGame(PlayerController playerController){
        try{
//            IPlayerRoomEventListener playerRoomEventListener = roomListenerMap.get(playerController.getPlayer().getGameType());
//            if(playerRoomEventListener == null){
//                log.warn("退出游戏时 未找到 playerRoomEventListener, playerId = {},gameType = {}", playerController.playerId(),playerController.getPlayer().getGameType());
//                return;
//            }
//            playerRoomEventListener.exit(playerController);
            clusterSystem.switchNode(playerController.getSession(), NodeType.HALL);
        }catch (Exception e){
            log.error("",e);
        }
    }
}
