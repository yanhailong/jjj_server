package com.jjg.game.core.data;

import com.jjg.game.core.constant.Code;

/**
 * @author 11
 * @date 2025/6/12 17:38
 */
public class AbstractGameRunInfo {
    protected int code;
    protected long playerId;

    public AbstractGameRunInfo(int code,long playerId) {
        this.code = code;
        this.playerId = playerId;
    }

    public boolean success(){
        return this.code == Code.SUCCESS;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }
}
