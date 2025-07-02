package com.jjg.game.room.data.room;

import com.jjg.game.room.sample.bean.RoomCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 每个小游戏的游戏配置,房间游戏数据 Value Object 值对象
 *
 * @author 2CL
 */
public class GameDataVo<RC extends RoomCfg> {

    protected static final Logger log = LoggerFactory.getLogger(GameDataVo.class);
    // 房间配置
    protected RC roomCfg;
    // 游戏对应的房间ID
    private long roomId;
    // 玩家数据合集 playerId <=> 玩家数据
    protected Map<Long, GamePlayer> gamePlayerMap = new HashMap<>();

    /**
     * 必须初始化的参数是房间配置RoomCfg，如果后续子类添加需要在
     */
    public GameDataVo(RC roomCfg) {
        this.roomCfg = roomCfg;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public RC getRoomCfg() {
        return roomCfg;
    }

    public Map<Long, GamePlayer> getGamePlayerMap() {
        return gamePlayerMap;
    }

    public GamePlayer getGamePlayer(long playerId) {
        return gamePlayerMap.get(playerId);
    }
}
