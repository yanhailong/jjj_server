package com.jjg.game.room.data.robot;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jjg.game.room.data.room.GamePlayer;

/**
 * 机器人游戏数据
 *
 * @author 2CL
 */
public class GameRobotPlayer extends GamePlayer {
    /**
     * 机器人行为id
     */
    private transient int actionId;
    /**
     * 上一局是否获胜 1获胜 2未获胜
     */
    private transient int lastWin;

    @JSONField(serialize = false, deserialize = false)
    @JsonIgnore
    public int getActionId() {
        return actionId;
    }

    @JSONField(serialize = false, deserialize = false)
    @JsonIgnore
    public void setActionId(int actionId) {
        this.actionId = actionId;
    }
    @JSONField(serialize = false, deserialize = false)
    @JsonIgnore
    public int getLastWin() {
        return lastWin;
    }
    @JSONField(serialize = false, deserialize = false)
    @JsonIgnore
    public void setLastWin(int lastWin) {
        this.lastWin = lastWin;
    }
}
