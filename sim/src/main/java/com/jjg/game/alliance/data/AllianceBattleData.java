package com.jjg.game.alliance.data;

import com.jjg.game.alliance.constant.AllianceConst;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * 联盟对决期数据 (每周一期一文档, _id = periodKey 如 "2026W24")。
 * <p>
 * 阶段状态机由 leader 节点定时推进, 状态变更全部走"旧状态条件更新"幂等化(防 leader 切换双跑)。
 * 高频积分不在本文档: 联盟总分/盟内个人分都在 Redis zset (见 AllianceConst.RedisKey),
 * 结算时快照写入 {@link #results}。
 * <p>
 * 镜像协议: {@link #matches} 不要求对称 —— A 的对手是 B 时, B 的对手可以是 C;
 * 每个联盟只关心自己对局的胜负, 因此任意报名数量都能完成匹配。
 *
 * @author 11
 * @date 2026/6/11
 */
@Document("allianceBattleData")
public class AllianceBattleData {
    //期号 (ISO 周, 如 2026W24)
    @Id
    private String period;

    //当前阶段 (AllianceConst.BattleState)
    private int state = AllianceConst.BattleState.NONE;

    //--------- 阶段时间点(ms, 创建时由配置推算固化, 改配置不影响进行中的期) ---------
    private long signupStartTime;
    private long signupEndTime;
    private long matchTime;
    private long fightStartTime;
    private long fightEndTime;

    //报名 allianceId -> 条目
    private Map<Long, BattleSignup> signups = new HashMap<>();
    //匹配结果 allianceId -> 对手 allianceId (镜像协议, 不要求对称)
    private Map<Long, Long> matches = new HashMap<>();
    //结算结果 allianceId -> 结果
    private Map<Long, BattleResult> results = new HashMap<>();

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getSignupStartTime() {
        return signupStartTime;
    }

    public void setSignupStartTime(long signupStartTime) {
        this.signupStartTime = signupStartTime;
    }

    public long getSignupEndTime() {
        return signupEndTime;
    }

    public void setSignupEndTime(long signupEndTime) {
        this.signupEndTime = signupEndTime;
    }

    public long getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(long matchTime) {
        this.matchTime = matchTime;
    }

    public long getFightStartTime() {
        return fightStartTime;
    }

    public void setFightStartTime(long fightStartTime) {
        this.fightStartTime = fightStartTime;
    }

    public long getFightEndTime() {
        return fightEndTime;
    }

    public void setFightEndTime(long fightEndTime) {
        this.fightEndTime = fightEndTime;
    }

    public Map<Long, BattleSignup> getSignups() {
        return signups;
    }

    public void setSignups(Map<Long, BattleSignup> signups) {
        this.signups = signups == null ? new HashMap<>() : signups;
    }

    public Map<Long, Long> getMatches() {
        return matches;
    }

    public void setMatches(Map<Long, Long> matches) {
        this.matches = matches == null ? new HashMap<>() : matches;
    }

    public Map<Long, BattleResult> getResults() {
        return results;
    }

    public void setResults(Map<Long, BattleResult> results) {
        this.results = results == null ? new HashMap<>() : results;
    }

    // ---------------------------------------------------------------------
    // 便捷方法
    // ---------------------------------------------------------------------

    public boolean signedUp(long allianceId) {
        return signups != null && signups.containsKey(allianceId);
    }

    /**
     * 联盟的对手, 未匹配返回 0
     */
    public long opponentOf(long allianceId) {
        if (matches == null) {
            return 0;
        }
        Long opp = matches.get(allianceId);
        return opp == null ? 0 : opp;
    }

    /**
     * 对决进行中 (积分入账窗口)
     */
    public boolean fighting(long now) {
        return state == AllianceConst.BattleState.FIGHTING && now >= fightStartTime && now < fightEndTime;
    }
}
