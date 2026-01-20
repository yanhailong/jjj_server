package com.jjg.game.slots.manager;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.listener.SessionEnterListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.data.PlayerSessionToken;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.recharge.service.RechargeService;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.data.SlotsPlayerGameData;
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
    private SlotsFactoryManager slotsFactoryManager;
    @Autowired
    private PlayerSessionTokenDao playerSessionTokenDao;

    @Autowired
    private SlotsRoomManager slotsRoomManager;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private RechargeService rechargeService;

    @Override
    public void sessionClose(PFSession session) {
        exitGame(session, false);
    }

    @Override
    public void sessionEnter(PFSession session, long playerId) {
        try {
            session.setPlayerId(playerId);
            session.setWorkId(playerId);

            PlayerSessionInfo info = playerSessionService.getInfo(playerId);
            if (info == null) {
                log.warn("sessionEnter时 PlayerSessionInfo 为空 playerId = {}", playerId);
                return;
            }

            if (info.getGameType() < 1) {
                log.warn("sessionEnter时 PlayerSessionInfo中gameType小于1 playerId = {}", playerId);
                return;
            }

            //检查slots游戏管理器
            AbstractSlotsGameManager gameManager = slotsFactoryManager.getGameManager(info.getGameType(), info.getRoomCfgId());
            if (gameManager == null) {
                log.debug("sessionEnter时，获取游戏管理器失败 playerId = {},gameType = {}", playerId, info.getGameType());
                return;
            }

            final PlayerSessionInfo tempInfo = info;

            Player player = playerService.doSave(playerId, p -> {
                p.setGameType(tempInfo.getGameType());
                p.setRoomCfgId(tempInfo.getRoomCfgId());
            });

            playerSessionService.enterGameServer(player);

            PlayerController playerController = new PlayerController(session, player);
            session.setReference(playerController);

            if (player.getRoomId() < 1) {
                enterSlotsGame(session, player, playerController, info, gameManager);
            } else {
                enterRoomSlotsGame(session, player, playerController, info, gameManager);
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 进入slots游戏
     *
     * @param session
     * @param player
     * @param playerSessionInfo
     * @param gameManager
     */
    private void enterSlotsGame(PFSession session, Player player, PlayerController playerController, PlayerSessionInfo playerSessionInfo, AbstractSlotsGameManager gameManager) {
        //放入玩家对应线程中处理避免和回存冲突
        PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(session.getWorkId(), 0, new BaseHandler<String>() {
            @Override
            public void action() throws Exception {
                taskManager.loadTaskData(player.getId());
                //创建 PlayerGameData
                gameManager.createPlayerGameData(playerController);
                //大厅非重连会检查一次，这里再检查一次
                rechargeService.loadOfflineRecharge(player.getId());
            }
        });
        PlayerSessionToken playerSessionToken = playerSessionTokenDao.getByPlayerId(player.getId());
        logger.enterGame(player, player.getGameType(), player.getRoomCfgId(), playerSessionToken.getDevice());
        log.debug("玩家进入slots 游戏 playerId = {},gameType = {}", player.getId(), player.getGameType());
    }

    /**
     * 进入好友房slots游戏
     *
     * @param session
     * @param player
     * @param playerSessionInfo
     * @param gameManager
     */
    private void enterRoomSlotsGame(PFSession session, Player player, PlayerController playerController, PlayerSessionInfo playerSessionInfo, AbstractSlotsGameManager gameManager) {
        SlotsRoomController slotsRoomController = slotsRoomManager.enterRoom(player.getGameType(), player.getRoomId(), player.getId());
        if (slotsRoomController == null) {
            log.warn("进入好友房slots时失败 playerId = {},gameType = {},roomId = {}", player.getId(), player.getGameType(), player.getRoomId());
            playerService.doSave(player.getId(), p -> {
                p.setRoomId(0);
            });
            return;
        }

        //放入玩家对应线程中处理避免和回存冲突
        PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(session.getWorkId(), 0, new BaseHandler<String>() {
            @Override
            public void action() throws Exception {
                playerController.setScene(slotsRoomController);
                //创建 PlayerGameData
                taskManager.loadTaskData(player.getId());
                gameManager.createPlayerGameData(playerController);
                //大厅非重连会检查一次，这里再检查一次
                rechargeService.loadOfflineRecharge(player.getId());
            }
        });
        PlayerSessionToken playerSessionToken = playerSessionTokenDao.getByPlayerId(player.getId());
        logger.enterGame(player, player.getGameType(), player.getRoomCfgId(), playerSessionToken.getDevice());
        log.debug("玩家进入好友房slots 游戏 playerId = {},gameType = {},roomId = {}", player.getId(), player.getGameType(), player.getRoomId());
    }

    /**
     * 退出游戏
     *
     * @param session        PFSession
     * @param initiativeExit 是否主动退出
     */
    public int exitGame(PFSession session, boolean initiativeExit) {
        PlayerController playerController = (PlayerController) session.getReference();
        if (playerController == null) {
            log.warn("玩家退出游戏服务器时 playerController 为空,playerId={},sessionId={}", session.getPlayerId(),
                    session.sessionId());
            return Code.SUCCESS;
        }

        AbstractSlotsGameManager gameManager = slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
        if (gameManager == null) {
            log.debug("退出游戏时，获取游戏管理器失败 playerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
            return Code.SUCCESS;
        }
        SlotsPlayerGameData playerGameData = gameManager.getPlayerGameData(playerController);
        if (playerGameData == null) {
            return Code.SUCCESS;
        }
        boolean canExit = gameManager.canExit(playerGameData);
        if (initiativeExit && !canExit) {
            return Code.FAIL;
        }
        playerGameData = gameManager.exit(playerController, initiativeExit || canExit);
        playerSessionService.offline(playerController.getPlayer(), !canExit);
        //计算玩游戏的时长
        int onlineTimeLen = 0;
        if (playerGameData != null) {
            onlineTimeLen = TimeHelper.nowInt() - playerGameData.getCreateTime();
        }
        session.setReference(null);
        logger.exitGame(playerController.getPlayer(), onlineTimeLen, playerController.getPlayer().getDeviceType());
        log.debug("玩家退出slots游戏 playerId = {}", playerController.playerId());
        return Code.SUCCESS;
    }

}
