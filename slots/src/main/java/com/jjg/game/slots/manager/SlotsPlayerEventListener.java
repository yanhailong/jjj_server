package com.jjg.game.slots.manager;

import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.listener.SessionEnterListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.dao.PlayerLastGameInfoDao;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;


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
    private PlayerLastGameInfoDao playerLastGameInfoDao;
    @Autowired
    private SlotsFactoryManager slotsFactoryManager;
    @Autowired
    private PlayerSessionTokenDao playerSessionTokenDao;


    @Override
    public void sessionClose(PFSession session) {
        exitGame(session);
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

            //先检查是否为断线重连
            Optional<PlayerLastGameInfo> op = playerLastGameInfoDao.findById(playerId);
            if (op.isPresent()) {
                PlayerLastGameInfo playerLastGameInfo = op.get();
                if (playerLastGameInfo.isHalfwayOffline() && StringUtils.isNotEmpty(playerLastGameInfo.getNodePath())) {
                    info.setGameType(playerLastGameInfo.getGameType());
                    info.setRoomCfgId(playerLastGameInfo.getRoomCfgId());
                }
            } else {
                if (info.getGameType() < 1) {
                    log.warn("sessionEnter时 PlayerSessionInfo 中的gameType小于1 playerId = {}", playerId);
                    return;
                }
            }

            AbstractSlotsGameManager gameManager = slotsFactoryManager.getGameManager(info.getGameType());
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

            //创建 PlayerGameData
            gameManager.createPlayerGameData(playerController);

            slotsFactoryManager.clearPlayerEvent(playerId);

            PlayerSessionToken playerSessionToken = playerSessionTokenDao.getByPlayerId(playerId);
            logger.enterGame(player, player.getGameType(), player.getRoomCfgId(), playerSessionToken.getDevice());
            log.debug("玩家进入slots 游戏 playerId = {},gameType = {}", playerId, player.getGameType());
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public void exitGame(PFSession session) {
        PlayerController playerController = (PlayerController) session.getReference();
        if (playerController == null) {
            log.warn("玩家退出游戏服务器时 playerController 为空,playerId={},sessionId={}", session.getPlayerId(),
                    session.sessionId());
            return;
        }

        AbstractSlotsGameManager gameManager = slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType());
        if (gameManager == null) {
            log.debug("退出游戏时，获取游戏管理器失败 playerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
            return;
        }

        SlotsPlayerGameData playerGameData = gameManager.exit(playerController);
        playerSessionService.offline(playerController.getPlayer(), false);

        //计算玩游戏的时长
        int onlineTimeLen = 0;
        if (playerGameData != null) {
            onlineTimeLen = TimeHelper.nowInt() - playerGameData.getCreateTime();
        }
        session.setReference(null);
        logger.exitGame(playerController.getPlayer(), onlineTimeLen, playerController.getPlayer().getDeviceType());
        log.debug("退出游戏结算 playerId = {}", playerController.playerId());
    }

}
