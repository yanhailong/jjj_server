package com.jjg.game.core.data;

import com.jjg.game.core.constant.EGameType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间对象
 *
 * @author 11
 * @date 2025/6/25 9:24
 */
public class Room {
    //房间id
    protected long id;
    //房间类型
    protected RoomType type;
    //游戏类型
    protected int gameType;
    //场次id
    protected int roomCfgId;
    //该房间内的玩家
    protected Map<Long, RoomPlayer> roomPlayers = new ConcurrentHashMap<>();
    //座位号 -> 玩家id,座位号从0开始
    protected Map<Integer, Long> playerSits;
    //房间人数最大限制
    protected int maxLimit;
    //所在节点
    protected String path;
    //创建时间
    protected int createTime;
    //创建者,如果为0表示系统创建
    protected long creator;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getRoomCfgId() {
        return roomCfgId;
    }

    public void setRoomCfgId(int roomCfgId) {
        this.roomCfgId = roomCfgId;
    }

    public Map<Long, RoomPlayer> getRoomPlayers() {
        return roomPlayers;
    }

    public void setRoomPlayers(Map<Long, RoomPlayer> roomPlayers) {
        this.roomPlayers = roomPlayers;
    }

    public Map<Integer, Long> getPlayerSits() {
        return playerSits;
    }

    public void setPlayerSits(Map<Integer, Long> playerSits) {
        this.playerSits = playerSits;
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    public long getCreator() {
        return creator;
    }

    public void setCreator(long creator) {
        this.creator = creator;
    }

    /**
     * 判断房间是否能加入新玩家
     *
     * @return
     */
    public boolean canEnter() {
        if (this.roomPlayers == null) {
            return true;
        }
        return this.roomPlayers.size() < maxLimit;
    }

    /**
     * 判断该玩家是否在房间
     *
     * @param playerId
     * @return
     */
    public boolean hasPlayer(long playerId) {
        if (this.roomPlayers == null) {
            return false;
        }
        return this.roomPlayers.containsKey(playerId);
    }

    /**
     * 判断该座位上是否有人
     *
     * @param sit
     * @return
     */
    public boolean setHasPlayer(int sit) {
        if (this.playerSits == null) {
            return false;
        }
        return this.playerSits.containsKey(sit);
    }

    public void addPlayer(RoomPlayer roomPlayer) {
        if (this.playerSits == null) {
            this.playerSits = new HashMap<>();
        }
        if (this.roomPlayers == null) {
            this.roomPlayers = new ConcurrentHashMap<>();
        }
        this.playerSits.put(roomPlayer.getSit(), roomPlayer.getPlayerId());
        this.roomPlayers.put(roomPlayer.getPlayerId(), roomPlayer);
    }

    /**
     * 移除房间中所有的机器人
     */
    public void removeAllRobotPlayer() {
        if (this.roomPlayers == null || this.roomPlayers.isEmpty()) {
            return;
        }
        this.roomPlayers.entrySet().removeIf(entry -> {
            boolean isRobot = entry.getValue().isRobot();
            if (isRobot && playerSits != null) {
                playerSits.remove(entry.getValue().getSit());
            }
            return isRobot;
        });
    }

    public RoomPlayer getPlayer(long playerId) {
        if (this.roomPlayers == null) {
            return null;
        }
        return this.roomPlayers.get(playerId);
    }

    /**
     * 玩家退出房间
     *
     * @param playerId
     * @return
     */
    public RoomPlayer exit(long playerId) {
        if (this.roomPlayers == null) {
            return null;
        }

        RoomPlayer removePlayer = this.roomPlayers.remove(playerId);
        if (removePlayer == null) {
            return null;
        }
        if (this.roomPlayers.isEmpty()) {
            this.roomPlayers = new ConcurrentHashMap<>();
        }
        if (this.playerSits != null) {
            this.playerSits.remove(removePlayer.getSit());
            if (this.playerSits.isEmpty()) {
                this.playerSits = null;
            }
        }
        return removePlayer;
    }

    /**
     * 批量退出玩家
     */
    public List<RoomPlayer> exitPlayers(List<Long> playerIds) {
        if (this.roomPlayers == null) {
            return new ArrayList<>();
        }
        List<RoomPlayer> removedPlayers = new ArrayList<>();
        Iterator<Map.Entry<Long, RoomPlayer>> iterator = roomPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, RoomPlayer> entry = iterator.next();
            if (playerIds.contains(entry.getKey())) {
                removedPlayers.add(entry.getValue());
                iterator.remove();
                if (!this.playerSits.isEmpty()) {
                    this.playerSits.remove(entry.getValue().getSit());
                }
            }
        }
        return removedPlayers;
    }

    /**
     * 房间中机器人的数量
     */
    public int countRobots() {
        return (int) roomPlayers.values().stream().filter(RoomPlayer::isRobot).count();
    }

    /**
     * 房间中真人的数量
     */
    public int countPlayers() {
        return (int) roomPlayers.values().stream().filter(r -> !r.isRobot()).count();
    }

    public boolean empty() {
        if (this.roomPlayers == null) {
            return true;
        }
        return this.roomPlayers.isEmpty();
    }

    /**
     * 返回房间id, 默认为系统庄家
     */
    public long roomBankerId() {
        return Long.MIN_VALUE;
    }

    /**
     * 庄家总金币
     */
    public long bankerTotalGold() {
        return Long.MIN_VALUE;
    }

    /**
     * 扣除庄家金币
     */
    public void deductBankerPredicateItem(long deductItemNum) {
    }

    /**
     * 添加庄家金币
     */
    public void addBankerBankerPredicateItem(long addGold) {
    }

    public String logStr() {
        EGameType eGameType = EGameType.getGameByTypeId(gameType);
        return "roomId: " + id + " game: " + eGameType.getGameDesc() + " roomCfgId: " + roomCfgId;
    }
}
