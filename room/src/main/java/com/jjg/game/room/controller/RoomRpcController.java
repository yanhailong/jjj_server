package com.jjg.game.room.controller;

import com.jjg.game.common.curator.NodeType;
import com.jjg.game.core.dao.PlayerLastGameInfoDao;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerLastGameInfo;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.handler.CoreRPCController;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.room.manager.RoomManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author 11
 * @date 2026/2/4
 */
@Component
public class RoomRpcController extends CoreRPCController {

    @Autowired
    private RoomManager roomManager;
    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private PlayerLastGameInfoDao playerLastGameInfoDao;
    @Autowired
    private PlayerSessionService playerSessionService;

    @Override
    public int kickToHall(long playerId) {
        if (playerId < 1) {
            log.warn("kickToHall failed, playerId is invalid. playerId={}", playerId);
            return Code.FAIL;
        }
        try {
            PlayerController playerController = null;
            String ipAddress = null;
            AbstractRoomController<?, ?> roomController = roomManager.getRoomControllerByPlayer(playerId);
            if (roomController != null) {
                playerController = roomController.getPlayerController(playerId);
            }

            if (playerController != null) {
                ipAddress = playerController.ipAddress();
                int exitCode = roomManager.exitRoom(playerController, true);
                if (exitCode != Code.SUCCESS) {
                    log.warn("kickToHall failed when exitRoom. playerId={},code={}", playerId, exitCode);
                    return exitCode;
                }
            }

            playerService.doSave(playerId, p -> {
                p.setGameType(0);
                p.setRoomCfgId(0);
                p.setRoomId(0);
            });

            Optional<PlayerLastGameInfo> lastGameInfoOpt = playerLastGameInfoDao.findById(playerId);
            if (lastGameInfoOpt.isPresent()) {
                PlayerLastGameInfo lastGameInfo = lastGameInfoOpt.get();
                lastGameInfo.setGameUniqueId(0);
                lastGameInfo.setGameType(0);
                lastGameInfo.setRoomCfgId(0);
                lastGameInfo.setRoomId(0);
                lastGameInfo.setNodePath(null);
                lastGameInfo.setHalfwayOffline(false);
                lastGameInfo.setExtra(null);
                playerLastGameInfoDao.save(lastGameInfo);
            }

            PlayerSessionInfo sessionInfo = playerSessionService.getInfo(playerId);
            if (sessionInfo != null) {
                sessionInfo.setGameType(0);
                sessionInfo.setRoomCfgId(0);
                sessionInfo.setReconnect(false);
                playerSessionService.save(sessionInfo);
            }

            if (playerController != null && playerController.getSession() != null) {
                clusterSystem.switchNode(playerController.getSession(), NodeType.HALL, ipAddress, playerController.playerId());
            }

            log.info("kickToHall success. playerId={}", playerId);
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("kickToHall exception. playerId={}", playerId, e);
            return Code.EXCEPTION;
        }
    }
}
