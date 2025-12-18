package com.jjg.game.table.common;

import com.jjg.game.core.data.BetFriendRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.room.friendroom.AbstractFriendRoomController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.dao.BetTableFriendRoomDao;


/**
 * 好友房
 *
 * @author 2CL
 */
public class TableFriendRoomController extends AbstractFriendRoomController<Room_BetCfg, BetFriendRoom> {

    public TableFriendRoomController(Class<? extends RoomPlayer> roomPlayerClazz, BetFriendRoom room) {
        super(roomPlayerClazz, room);
    }

    public void onFriendRoomCreate() {
        if (getRoomDao() instanceof BetTableFriendRoomDao dao) {
            boolean roomPoolKey = dao.setRoomPoolKey(room.getGameType(), room.getId(), room.getPool());
            if (!roomPoolKey) {
                log.error("好友房初始化房间池失败，gameType: {}, roomId: {}", room.getGameType(), room.getId());
            }
        }
    }

    @Override
    protected boolean checkBankerCanNextRound() {
        return true;
    }

    @Override
    public void reloadRoomCfg() {
        // 重载配置表引用
        roomCfg = GameDataManager.getRoom_BetCfg(room.getRoomCfgId());
        gameController.getGameDataVo().reloadRoomCfg();
    }
}
