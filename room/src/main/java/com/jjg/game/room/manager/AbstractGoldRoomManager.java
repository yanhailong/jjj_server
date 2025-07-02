package com.jjg.game.room.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.data.*;
import com.jjg.game.room.dao.AbstractGoldRoomDao;

import java.util.List;

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
    public void joinRandRoom(PlayerController playerController, int gameType, int wareId) {
        try {
            AbstractRoomDao<Room, ? extends RoomPlayer> roomDao = getRoomDao(gameType);
            int roomId = roomDao.getCanJoinRoomId(gameType, wareId);
            if (roomId < 1) {
                log.debug("加入房间失败，获取到的房间id小于1，playerId = {},roomId = {},gameType = {},wareId = {}",
                    playerController.playerId(), roomId, gameType, wareId);
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

            if (nodeConfig.getGameTypes() == null || nodeConfig.getGameTypes().length == 0) {
                log.debug("该程序设置的支持游戏类型为空，清除房间失败");
                return;
            }

            for (int gameType : nodeConfig.getGameTypes()) {
                AbstractRoomDao<Room, ? extends RoomPlayer> roomDao = getRoomDao(gameType);
                for (int wareId : CoreConst.WARE_IDS) {
                    List<Object> allRoomIds = roomDao.getAllRoomIds(gameType, wareId);
                    if (allRoomIds == null || allRoomIds.isEmpty()) {
                        continue;
                    }
                    for (Object obj : allRoomIds) {
                        int roomId = Integer.parseInt(obj.toString());
                        Long res = roomDao.removeRoom(gameType, roomId, wareId);
                        if (res != null && res > 0) {
                            log.info("成功清除房间 gameType = {},roomId = {}", gameType, roomId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
