package com.jjg.game.room.manager;

import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.data.*;


/**
 * @author 11
 * @date 2025/6/25 18:22
 */
public class AbstractGoldRoomManager extends AbstractRoomManager {

    public AbstractGoldRoomManager() {
        super();
    }

    /**
     * 随机加入房间
     *
     * @param playerController
     * @param gameType
     * @return
     */
    public void joinRandRoom(PlayerController playerController, int gameType, int roomCfgId) {
        try {
            AbstractRoomDao<Room, ? extends RoomPlayer> roomDao = getRoomDao(gameType);
            int roomId = roomDao.getCanJoinRoomId(gameType, roomCfgId);
            if (roomId < 1) {
                log.debug("加入房间失败，获取到的房间id小于1，playerId = {},roomId = {},gameType = {},roomCfgId = {}",
                    playerController.playerId(), roomId, gameType, roomCfgId);
                return;
            }
            super.joinRoom(playerController, gameType, roomId);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    public void clearRoom() {
        try {
            log.debug("系统开始清除房间");


        } catch (Exception e) {
            log.error("", e);
        }
    }
}
