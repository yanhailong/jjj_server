package com.jjg.game.ploy.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

/**
 * 玩家在策略游戏中的数据
 *
 * @author 11
 * @date 2026/3/19
 */
public class PlayerPloyGameData {

    @Id
    private String id;
    @Transient
    @JsonIgnore
    protected transient PlayerController playerController;
    //游戏 gameType
    protected int gameType;
    //场次 roomCfgId
    protected int roomCfgId;
    //最近一次的押注
    protected long lastBet;
    //最近一次的押注
    protected long lastBetTime;
    //盈利情况
    protected long win;
    //中奖倍数
    protected int winTimes;
    //最近一次活跃时间
    @Transient
    @JsonIgnore
    protected transient long lastActiveTime;
    @Transient
    @JsonIgnore
    protected transient PloyBetDivideInfo ployBetDivideInfo;
    //@Field(targetType = FieldType.DECIMAL128)
    //private BigDecimal amount;

    public static String buildId(long playerId, long roomCfgId) {
        return playerId + ":" + roomCfgId;
    }

    public String getId() {
        return id;
    }

    public PlayerController getPlayerController() {
        return playerController;
    }

    public void setPlayerController(PlayerController playerController) {
        this.playerController = playerController;
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

    public long getLastBet() {
        return lastBet;
    }

    public void setLastBet(long lastBet) {
        this.lastBet = lastBet;
    }

    public long getLastBetTime() {
        return lastBetTime;
    }

    public void setLastBetTime(long lastBetTime) {
        this.lastBetTime = lastBetTime;
    }

    public long getWin() {
        return win;
    }

    public void setWin(long win) {
        this.win = win;
    }

    public int getWinTimes() {
        return winTimes;
    }

    public void setWinTimes(int winTimes) {
        this.winTimes = winTimes;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public PloyBetDivideInfo getPloyBetDivideInfo() {
        return ployBetDivideInfo;
    }

    public void setPloyBetDivideInfo(PloyBetDivideInfo ployBetDivideInfo) {
        this.ployBetDivideInfo = ployBetDivideInfo;
    }

    public long playerId() {
        if (this.playerController == null) {
            return 0;
        }
        return this.playerController.playerId();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void updatePlayer(Player player) {
        if (this.playerController == null || player == null) {
            return;
        }
        this.playerController.setPlayer(player);
    }

    public void changePlayerLastMoney(long money){
        if(this.ployBetDivideInfo != null){
            this.ployBetDivideInfo.setPlayerAfterMoney(money);
        }
    }
}
