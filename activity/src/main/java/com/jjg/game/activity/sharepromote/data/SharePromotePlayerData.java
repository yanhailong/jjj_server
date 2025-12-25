package com.jjg.game.activity.sharepromote.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    //有效下级 ID
    private List<Long> validSubordinateIds;
    //未领取的玩家 ID
    private Map<Long, Long> notClaimedPlayerIds;

    public Map<Long, Long> getNotClaimedPlayerIds() {
        return notClaimedPlayerIds;
    }

    public void setNotClaimedPlayerIds(Map<Long, Long> notClaimedPlayerIds) {
        this.notClaimedPlayerIds = notClaimedPlayerIds;
    }

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

    public List<Long> getValidSubordinateIds() {
        return validSubordinateIds;
    }

    public void setValidSubordinateIds(List<Long> validSubordinateIds) {
        this.validSubordinateIds = validSubordinateIds;
    }

    public void addNotClaimedPlayerId(Long playerId, Long time) {
        if (notClaimedPlayerIds == null) {
            notClaimedPlayerIds = new HashMap<>();
        }
        notClaimedPlayerIds.put(playerId, time);
    }


    public void addValidSubordinateId(Long playerId) {
        if (validSubordinateIds == null) {
            validSubordinateIds = new ArrayList<>();
        }
        validSubordinateIds.add(playerId);
    }
}
