package com.jjg.game.activity.sharepromote.data;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/16 19:39
 */
public class SharePromotePlayerData {
    //领取的历史记录
    private List<String> history;
    //邀请码
    private String code;
    //绑定玩家数
    private int bindCount;

    public List<String> getHistory() {
        return history;
    }

    public void setHistory(List<String> history) {
        this.history = history;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getBindCount() {
        return bindCount;
    }

    public void setBindCount(int bindCount) {
        this.bindCount = bindCount;
    }
}
