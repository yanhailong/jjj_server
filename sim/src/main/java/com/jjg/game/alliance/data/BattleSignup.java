package com.jjg.game.alliance.data;

/**
 * 对决报名条目 (内嵌于 {@link AllianceBattleData#getSignups()})。
 *
 * @author 11
 * @date 2026/6/11
 */
public class BattleSignup {
    //报名时联盟等级 (同等级匹配依据, 报名时刻快照)
    private int level;
    //报名时间(ms)
    private long signupTime;
    //报名操作人 (盟主)
    private long operatorId;

    public BattleSignup() {
    }

    public BattleSignup(int level, long signupTime, long operatorId) {
        this.level = level;
        this.signupTime = signupTime;
        this.operatorId = operatorId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getSignupTime() {
        return signupTime;
    }

    public void setSignupTime(long signupTime) {
        this.signupTime = signupTime;
    }

    public long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(long operatorId) {
        this.operatorId = operatorId;
    }
}
