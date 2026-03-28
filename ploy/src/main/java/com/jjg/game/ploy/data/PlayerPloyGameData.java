package com.jjg.game.ploy.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;

/**
 * 玩家在策略游戏中的数据
 *
 * @author 11
 * @date 2026/3/19
 */
@CompoundIndex(
        name = "ploy_game_data_idx",
        def = "{'playerId': 1, 'roomCfgId': 1}",
        unique = true
)
public class PlayerPloyGameData {
    @Transient
    @JsonIgnore
    protected transient PlayerController playerController;
    //进策略游戏前的 gameType
    protected int gameType;
    //进策略游戏前的 roomCfgId
    protected int roomCfgId;
    //最近一次的押注
    protected long lastBet;
    //最近一次的押注
    protected long lastBetTime;
    //押注前余额
    protected long beforeMoney;
    //押注后余额
    protected long afterMoney;
    //盈利情况
    protected long win;
    //中奖倍数
    protected int winTimes;
    //最近一次的poolResultLib表的id
    protected int poolResultLibCfgId;
    //最近一次活跃时间
    @Transient
    @JsonIgnore
    protected transient long lastActiveTime;
    //@Field(targetType = FieldType.DECIMAL128)
    //private BigDecimal amount;

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

    public int getPoolResultLibCfgId() {
        return poolResultLibCfgId;
    }

    public void setPoolResultLibCfgId(int poolResultLibCfgId) {
        this.poolResultLibCfgId = poolResultLibCfgId;
    }

    public long getLastBetTime() {
        return lastBetTime;
    }

    public void setLastBetTime(long lastBetTime) {
        this.lastBetTime = lastBetTime;
    }

    public long getBeforeMoney() {
        return beforeMoney;
    }

    public void setBeforeMoney(long beforeMoney) {
        this.beforeMoney = beforeMoney;
    }

    public long getAfterMoney() {
        return afterMoney;
    }

    public void setAfterMoney(long afterMoney) {
        this.afterMoney = afterMoney;
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

    public long playerId() {
        if (this.playerController == null) {
            return 0;
        }
        return this.playerController.playerId();
    }

    public void updatePlayer(Player player) {
        if (this.playerController == null || player == null) {
            return;
        }
        this.playerController.setPlayer(player);
    }
}
