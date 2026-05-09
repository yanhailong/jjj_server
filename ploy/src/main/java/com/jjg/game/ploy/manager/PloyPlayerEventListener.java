package com.jjg.game.ploy.manager;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.listener.SessionEnterListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.recharge.service.RechargeService;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.ploy.controller.AbstractPloyController;
import com.jjg.game.ploy.data.PlayerPloyGameData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/5/9
 */
@Component
public class PloyPlayerEventListener implements SessionEnterListener, SessionCloseListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private CoreLogger logger;
    @Autowired
    private PlayerSessionTokenDao playerSessionTokenDao;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private RechargeService rechargeService;
    @Autowired
    private PloyFactoryManager ployFactoryManager;

    @Override
    public void sessionClose(PFSession session) {
        exitGame(session, ExitType.DROPPED);
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
                log.warn("sessionEnter时 PlayerSessionInfo中gameType小于1 getPlayerId = {}", playerId);
                return;
            }

            //检查slots游戏管理器
            AbstractPloyController gameManager = ployFactoryManager.getGameController(info.getGameType(), info.getRoomCfgId());
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
                enterGame(session, player, playerController, info, gameManager);
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
     * @param gameController
     */
    private void enterGame(PFSession session, Player player, PlayerController playerController, PlayerSessionInfo playerSessionInfo, AbstractPloyController gameController) {
        //放入玩家对应线程中处理避免和回存冲突
        PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(session.getWorkId(), 0, new BaseHandler<String>() {
            @Override
            public void action() throws Exception {
                playerController.setScene(gameController);
                taskManager.loadTaskData(player.getId());
                //创建 PlayerGameData
                gameController.createPlayerGameData(playerController);
                //大厅非重连会检查一次，这里再检查一次
                rechargeService.loadOfflineRecharge(player.getId());
            }
        });
        PlayerSessionToken playerSessionToken = playerSessionTokenDao.getByPlayerId(player.getId());
        logger.enterGame(player, player.getGameType(), player.getRoomCfgId(), playerSessionToken.getDevice());
        log.debug("玩家进入ploy 游戏 playerId = {},gameType = {}", player.getId(), player.getGameType());
    }

    /**
     * 退出游戏
     *
     * @param session  PFSession
     * @param exitType 退出类型
     */
    public int exitGame(PFSession session, ExitType exitType) {
        PlayerController playerController = (PlayerController) session.getReference();
        if (playerController == null) {
            log.warn("玩家退出游戏服务器时 playerController 为空,playerId={},sessionId={}", session.getPlayerId(),
                    session.sessionId());
            return Code.SUCCESS;
        }

        AbstractPloyController gameController = ployFactoryManager.getGameController(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
        if (gameController == null) {
            log.debug("退出游戏时，获取游戏管理器失败 playerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
            return Code.SUCCESS;
        }
        PlayerPloyGameData playerGameData = gameController.getPlayerGameData(playerController.playerId());
        if (playerGameData == null) {
            return Code.SUCCESS;
        }

        playerGameData = gameController.exit(playerController, exitType);
        playerSessionService.offline(playerController.getPlayer(), exitType == ExitType.DROPPED);
        //计算玩游戏的时长
        int onlineTimeLen = 0;
        if (playerGameData != null && playerGameData.getCreateTime() != 0) {
            onlineTimeLen = TimeHelper.nowInt() - playerGameData.getCreateTime();
        }
        session.setReference(null);
        logger.exitGame(playerController.getPlayer(), onlineTimeLen, playerController.getPlayer().getDeviceType());
        log.debug("玩家退出ploy游戏 playerId = {}", playerController.playerId());
        return Code.SUCCESS;
    }
}
