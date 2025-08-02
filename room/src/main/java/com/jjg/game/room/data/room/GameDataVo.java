package com.jjg.game.room.data.room;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.sample.bean.RoomCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 每个小游戏的游戏配置,房间游戏数据 Value Object 值对象，
 * 即使玩家离开数据依然会被房间引用到的数据
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
    // 游戏开始时间
    private long startTime;
    // 游戏结束时间
    private long stopTime;
    // 每个阶段的结束时间
    protected long phaseEndTime;
    // 每个阶段需要运行的时间
    protected long phaseRunTime;


    /**
     * 必须初始化的参数是房间配置RoomCfg，如果后续子类添加数据需要在自己的构造函数中添加
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

    public void addGamePlayer(GamePlayer gamePlayer) {
        gamePlayerMap.put(gamePlayer.getId(), gamePlayer);
    }

    public Map<Long, GamePlayer> getGamePlayerMap() {
        return gamePlayerMap;
    }

    /**
     * 获取除了机器人之外的玩家
     */
    public Map<Long, GamePlayer> getGamePlayerMapExceptRobot() {
        return gamePlayerMap.values().stream()
            .filter((gamePlayer) -> !(gamePlayer instanceof GameRobotPlayer))
            .collect(HashMap::new, (map, gamePlayer) -> map.put(gamePlayer.getId(), gamePlayer), HashMap::putAll);
    }

    public GamePlayer getGamePlayer(long playerId) {
        return gamePlayerMap.get(playerId);
    }

    public long getPhaseEndTime() {
        return phaseEndTime;
    }

    public void setPhaseEndTime(long phaseEndTime) {
        this.phaseEndTime = phaseEndTime;
    }

    public long getPhaseRunTime() {
        return phaseRunTime;
    }

    public void setPhaseRunTime(long phaseRunTime) {
        this.phaseRunTime = phaseRunTime;
    }

    public int getPlayerNum() {
        return gamePlayerMap.size();
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStopTime() {
        return stopTime;
    }

    public void reloadRoomCfg() {
    }

    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }

    public String roomLogInfo() {
        EGameType eGameType = EGameType.getGameByTypeId(roomCfg.getGameID());
        return "游戏类型：" + eGameType.getGameDesc() + " 房间配置ID: " + roomCfg.getId() + " 房间ID: " + roomId;
    }
}
