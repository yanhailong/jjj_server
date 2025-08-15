package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 匹配房间，需要存库
 *
 * @author 2CL
 */
@Document(collection = "FriendRoom")
public class FriendRoom extends Room {
    @Id
    protected long id;
    // 房间过期时间
    protected long roomOverdueTime;
    // 房间名
    protected String roomName;

    public long getRoomOverdueTime() {
        return roomOverdueTime;
    }

    public void setRoomOverdueTime(long roomOverdueTime) {
        this.roomOverdueTime = roomOverdueTime;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
}
