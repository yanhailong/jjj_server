package com.jjg.game.room.message;

import com.jjg.game.core.pb.AbstractMessage;

import java.util.HashSet;
import java.util.Set;

/**
 * 房间消息构造器
 *
 * @author 2CL
 */
public class RoomMessageBuilder<T extends AbstractMessage> {

    /**
     * 需要的发送的消息
     */
    private T data;

    /**
     * 指定发送的玩家ID，如果为空则对房间内的所有玩家进行广播
     */
    private Set<Long> playerIds = new HashSet<>();

    public static RoomMessageBuilder<AbstractMessage> newBuilder() {
        return new RoomMessageBuilder<>();
    }

    public RoomMessageBuilder<T> setData(T data) {
        this.data = data;
        return this;
    }

    /**
     * 向房间内的所有玩家广播消息
     */
    public RoomMessageBuilder<T> toRoomAllPlayers() {
        this.playerIds = new HashSet<>();
        return this;
    }

    public RoomMessageBuilder<T> setPlayerIds(Set<Long> playerIds) {
        this.playerIds = playerIds;
        return this;
    }

    public T getData() {
        return data;
    }

    public Set<Long> getPlayerIds() {
        return playerIds;
    }
}
