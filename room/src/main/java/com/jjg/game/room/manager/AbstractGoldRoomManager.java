package com.jjg.game.room.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.constant.GameConstant;
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

            for (int gameMajorType : nodeConfig.getGameMajorTypes()) {
                List<EGameType> list = GameConstant.MAJOR_TYPE_ID_SET.get(gameMajorType);
                for(EGameType eGameType : list){
                    AbstractRoomDao<Room, ? extends RoomPlayer> roomDao = getRoomDao(eGameType.getGameTypeId());
                    if(roomDao == null){
                        continue;
                    }
                    for (int wareId : CoreConst.WARE_IDS) {
                        List<Object> allRoomIds = roomDao.getAllRoomIds(eGameType.getGameTypeId(), wareId);
                        if (allRoomIds == null || allRoomIds.isEmpty()) {
                            continue;
                        }
                        for (Object obj : allRoomIds) {
                            int roomId = Integer.parseInt(obj.toString());
                            Long res = roomDao.removeRoom(eGameType.getGameTypeId(), roomId, wareId);
                            if (res != null && res > 0) {
                                log.info("成功清除房间 gameType = {},roomId = {}", eGameType.getGameTypeId(), roomId);
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
