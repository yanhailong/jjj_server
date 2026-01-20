package com.jjg.game.slots.dao;

import com.jjg.game.core.dao.room.AbstractFriendRoomDao;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.data.SlotsFriendRoom;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class SlotsFriendRoomDao extends AbstractFriendRoomDao<SlotsFriendRoom, RoomPlayer> {
    private final RoomSlotsPoolDao roomSlotsPoolDao;
    public SlotsFriendRoomDao(RoomSlotsPoolDao roomSlotsPoolDao) {
        super(SlotsFriendRoom.class);
        this.roomSlotsPoolDao = roomSlotsPoolDao;
    }

    @Override
    protected SlotsFriendRoom createNewEmptyFriendRoom(WarehouseCfg warehouseCfg) {
        return new SlotsFriendRoom();
    }

    public void save(SlotsFriendRoom room){
        redisTemplate.opsForHash().put(getTableName(room.getGameType()), room.getId(), room);
    }


    public Map<Object,Object> getRoomsByGameType(int gameType){
        return redisTemplate.opsForHash().entries(getTableName(gameType));
    }

    public SlotsFriendRoom getRoomByCfgId(int roomCfgId,long roomId){
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        if (warehouseCfg == null) {
            log.warn("获取好友房失败,未找到相关配置 roomCfgId = {},roomId = {}", roomCfgId, roomId);
            return null;
        }

        SlotsFriendRoom room = getRoom(warehouseCfg.getGameID(), roomId);
        if (room == null) {
            log.warn("获取好友房失败,未找到房间信息 roomCfgId = {},roomId = {}", roomCfgId, roomId);
            return null;
        }
        Number number = roomSlotsPoolDao.getBigPoolByRoomId(roomId);
        room.setPredictCostGoldNum(number == null ? 0 : number.longValue());
        return room;
    }
}
