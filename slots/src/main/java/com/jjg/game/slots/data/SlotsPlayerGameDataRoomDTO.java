package com.jjg.game.slots.data;

import org.springframework.data.annotation.Id;

/**
 * 房间模式玩家数据基类
 * @author lm
 * @date 2026/2/4
 */
public class SlotsPlayerGameDataRoomDTO extends SlotsPlayerGameDataDTO {
    @Id
    protected String id;
    //房间id(仅在好友房/房间模式使用)
    protected long roomId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public void buildRoomKey() {
        this.id = this.playerId + ":" + this.roomCfgId + ":" + this.roomId;
    }
}
