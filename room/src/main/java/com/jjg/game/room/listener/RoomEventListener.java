package com.jjg.game.room.listener;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.listener.SessionEnterListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.gameevent.*;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.handler.CurrentChangeEventHandler;
import com.jjg.game.room.handler.PlayerRechargeEventHandler;
import com.jjg.game.room.manager.AbstractRoomManager;
import com.jjg.game.room.timer.RoomEventType;
import com.jjg.game.sampledata.bean.RoomCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author 11
 * @date 2025/6/18 13:28
 */
@Component
public class RoomEventListener implements SessionEnterListener, SessionCloseListener, GameEventListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private CoreLogger logger;
    @Autowired
    private ClusterSystem clusterSystem;

    private AbstractRoomManager roomManager;

    private final Map<Integer, IPlayerRoomEventListener> roomListenerMap = new HashMap<>();
    @Autowired
    private TaskManager taskManager;

    public void init() {
        Map<String, IPlayerRoomEventListener> listenerMap =
                CommonUtil.getContext().getBeansOfType(IPlayerRoomEventListener.class);
        for (Map.Entry<String, IPlayerRoomEventListener> en : listenerMap.entrySet()) {
            int[] arr = en.getValue().getGameTypes();
            if (arr != null) {
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
        if (session.getReference() instanceof PlayerController playerController &&
                playerController.getScene() instanceof AbstractRoomController<?, ?> roomController) {
            roomController.getRoomProcessor().tryPublish(0, new BaseHandler<String>() {
                @Override
                public void action() {
                    exitRoomAction(session, false);
                }
            });
        }
    }

    public void exitRoomAction(PFSession session, boolean exit) {
        if (session == null || !(session.getReference() instanceof PlayerController playerController)) {
            log.warn("玩家退出游戏服务器时 playerController 为空 playerId={},sessionId={}", session == null ? "null" : session.getPlayerId(),
                    session == null ? "null" : session.sessionId());
            return;
        }
        if (playerController.getPlayer() == null) {
            log.warn("玩家退出游戏服务器时 playerController 为空,playerId={},sessionId={}", session.getPlayerId(),
                    session.sessionId());
            return;
        }
        log.info("玩家：{} 房间开始进入session关闭流程", playerController.playerId());
        // hall会在sessionClose时删除PlayerSession的数据,虽然RoomEventListener的调用顺序在hallPlayerEventListener之前，
        // 但是sessionClose消息不能保证到达顺序在hallPlayerEventListener之前，如果hallPlayerEventListener先调用则会出现找不到session的情况
        // 或者考虑在所有sessionClose调用完成后再删除session信息
        /*PlayerSessionInfo info = playerSessionService.getInfo(playerController.playerId());
        if (info == null) {
            log.warn("玩家退出游戏服务器时 PlayerSessionInfo 为空 playerId = {}", playerController.playerId());
            return;
        }*/
        int gameType = playerController.getPlayer().getGameType();
        if (gameType < 1) {
            log.warn("玩家退出游戏服务器时 PlayerSessionInfo 中的gameType小于1 playerId = {}", playerController.playerId());
            return;
        }

        playerSessionService.offline(playerController.getPlayer(), false);
        // 调用房间Controller的offline消息
        Object scene = playerController.getScene();
        if (scene instanceof AbstractRoomController<?, ?> roomController) {
            // 房间进入断线流程，
            // TODO 考虑保存玩家的数据在内存中，如果有自动托管逻辑，可以使用LRU保存一定的玩家。
            //  如果没有需要判断玩家当前处于具体游戏的哪个阶段，是否在需要完成整局再退出房间
            // abstractRoomController.playerOffline(playerController);
            // TODO 先让玩家直接退出，后续添加断线重连逻辑
            GamePlayer gamePlayer = roomController.getGameController().getGamePlayer(playerController.playerId());
            if (exit) {
                roomManager.exitRoom(playerController);
            } else {
                log.info("玩家掉线 player: {}", playerController.playerId());
                roomManager.disconnectedExitRoom(playerController);
            }
            //先取玩家信息,退出成功后会删掉
            int onlineTimeLen = 0;
            if (gamePlayer != null) {
                onlineTimeLen = TimeHelper.nowInt() - gamePlayer.getEnterGameTime();
            }
            logger.exitGame(playerController.getPlayer(), onlineTimeLen, playerController.getPlayer().getDeviceType());
        }

        IPlayerRoomEventListener playerRoomEventListener = roomListenerMap.get(gameType);
        if (Objects.nonNull(playerRoomEventListener)) {
            playerRoomEventListener.exit(session, playerController);
        } else {
            log.warn("玩家退出游戏服务器时未找到 playerRoomEventListener, playerId = {},gameType = {}",
                    playerController.playerId(), gameType);
        }
        log.info("房间 session close 成功 player: {}", playerController.playerId());
        session.setReference(null);
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

            logger.enterGame(player, info.getGameType(), info.getRoomCfgId(), player.getDeviceType());
            // 玩家房间ID不为0 且 不能是百家乐重连进入的房间
            if (player.getRoomId() > 0) {
                // 设置workId
                session.setWorkId(player.getRoomId());
                PlayerSessionInfo finalInfo = info;
                //刚进来的时候没有workId，会导致线程问题
                roomManager.getProcessorExecutors().tryPublish(session.getWorkId(), 0, new BaseHandler<String>() {
                    @Override
                    public void action() {
                        PlayerController playerController = new PlayerController(session, player);
                        session.setReference(playerController);
                        int code = roomManager.joinRoom(playerController, finalInfo.getGameType(), finalInfo.getRoomCfgId(), player.getRoomId());
                        if (code != Code.SUCCESS) {
                            // 加入失败,需要客户端主动确认当前玩家处于哪个场景中，ReqConfirmPlayerScene
                            playerService.doSave(playerId, p -> {
                                p.setGameType(0);
                                p.setRoomCfgId(0);
                                p.setRoomId(0);
                            });
                            // 将玩家切回到大厅, 此处不发消息是因为客户端在进入时可能还未初始化完成，收不到消息不能做处理
                            // 但是会请求玩家当前的场景位置，如果玩家在大厅会直接切回到大厅，如果在房间则正常进入房间
                            clusterSystem.switchNode(playerController.getSession(), NodeType.HALL);
                            return;
                        }
                        IPlayerRoomEventListener playerRoomEventListener = roomListenerMap.get(finalInfo.getGameType());
                        if (playerRoomEventListener == null) {
                            log.warn("sessionEnter时 未找到 playerRoomEventListener, playerId = {},gameType = {}", playerId, finalInfo.getGameType());
                            return;
                        }
                        playerRoomEventListener.enter(session, playerController, finalInfo);
                    }
                });
            } else {
                PlayerController playerController = new PlayerController(session, player);
                session.setReference(playerController);
                IPlayerRoomEventListener playerRoomEventListener = roomListenerMap.get(info.getGameType());
                if (playerRoomEventListener == null) {
                    log.warn("sessionEnter时 未找到 playerRoomEventListener, playerId = {},gameType = {}", playerId, info.getGameType());
                    return;
                }
                playerRoomEventListener.enter(session, playerController, info);
            }
        } catch (Exception e) {
            log.error("player: {} 进入session时发生异常: {}", playerId, e.getMessage(), e);
        }
    }

    public int exitGame(PlayerController playerController) {
        try {
            exitRoomAction(playerController.getSession(), true);
            clusterSystem.switchNode(playerController.getSession(), NodeType.HALL, playerController.ipAddress(), playerController.playerId());
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("退出房间异常, {}", e.getMessage(), e);
        }
        return Code.FAIL;
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof CurrencyChangeEvent event) {
            //获取玩家所在线程
            AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController = roomManager.getGameControllerByPlayerId(event.getPlayer().getId());
            if (gameController == null) {
                log.error("货币变化日志找不到玩家所在房间 playerId:{} changeValue:{}", event.getPlayer().getId(), JSON.toJSONString(event.getCurrencyMap()));
                return;
            }
            CurrentChangeEventHandler handler = new CurrentChangeEventHandler(event.getPlayer(), gameController, event.getCurrencyMap());
            //抛到对应房间线程处理
            gameController.addGameTimeEvent(new TimerEvent<>(gameController, handler), RoomEventType.CURRENCY_CHANGE_EVENT);
        }

        if (gameEvent instanceof PlayerEventCategory.PlayerRechargeEvent event) {
            //获取玩家所在线程
            AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController = roomManager.getGameControllerByPlayerId(event.getPlayer().getId());
            if (gameController == null) {
                log.error("玩家充值事件找不到玩家所在房间 playerId:{} order:{}", event.getPlayer().getId(), JSON.toJSONString(event.getOrder()));
                return;
            }
            PlayerRechargeEventHandler handler = new PlayerRechargeEventHandler(event.getPlayer().getId(), gameController, event.getOrder());
            //抛到对应房间线程处理
            gameController.addGameTimeEvent(new TimerEvent<>(gameController, handler), RoomEventType.PLAYER_RECHARGE_EVENT);
        }
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.CURRENCY_CHANGE, EGameEventType.RECHARGE);
    }
}
