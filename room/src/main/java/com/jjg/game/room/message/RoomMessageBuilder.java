package com.jjg.game.room.message;

import com.jjg.game.common.pb.AbstractMessage;

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
     * 指定发送的玩家ID，如果为空则对房间内的所有玩家进行广播 发送优先级1
     */
    private Set<Long> playerIds = new HashSet<>();

    /**
     * 需要排除的玩家ID 发送优先级3
     */
    private Set<Long> exceptPlayers = new HashSet<>();

    /**
     * 是否给所有玩家推送 发送优先级2
     */
    private boolean toAll = false;

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
    public RoomMessageBuilder<T> toAllPlayer() {
        this.toAll = true;
        return this;
    }

    public boolean isToAll() {
        return toAll;
    }

    public RoomMessageBuilder<T> sendPlayer(Long playerId,T data){
        return setPlayerIds(Set.of(playerId)).setData(data);
    }
    public RoomMessageBuilder<T> sendAllPlayer(T data){
        return toAllPlayer().setData(data);
    }

    public void setExceptPlayers(Set<Long> exceptPlayers) {
        this.exceptPlayers = exceptPlayers;
    }

    public Set<Long> getExceptPlayers() {
        return exceptPlayers;
    }

    public RoomMessageBuilder<T> exceptPlayer(Long playerId) {
        this.exceptPlayers.add(playerId);
        return this;
    }

    public RoomMessageBuilder<T> setPlayerIds(Set<Long> playerIds) {
        this.playerIds = playerIds;
        return this;
    }

    public RoomMessageBuilder<T> addPlayerId(Long playerId) {
        this.playerIds.add(playerId);
        return this;
    }

    public void setToAll(boolean toAll) {
        this.toAll = toAll;
    }
    public T getData() {
        return data;
    }

    public Set<Long> getPlayerIds() {
        return playerIds;
    }
}
