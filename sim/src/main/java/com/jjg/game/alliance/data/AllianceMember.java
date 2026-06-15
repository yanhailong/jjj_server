package com.jjg.game.alliance.data;

import com.jjg.game.alliance.constant.AllianceConst;

/**
 * 联盟成员条目 (内嵌于 {@link AllianceData#getMembers()})。
 * <p>
 * 只存联盟维度的关系数据; 昵称/头像等展示信息查询时从 Player 批量取(不冗余, 避免改名脏数据)。
 *
 * @author 11
 * @date 2026/6/11
 */
public class AllianceMember {
    //职位 (AllianceConst.Position)
    private int position = AllianceConst.Position.MEMBER;
    //入盟时间(ms)
    private long joinTime;
    //最近活跃时间(ms): 登录时更新, 供对决报名"近N天活跃人数"模式统计
    private long lastActiveTime;

    public AllianceMember() {
    }

    public AllianceMember(int position, long joinTime) {
        this.position = position;
        this.joinTime = joinTime;
        this.lastActiveTime = joinTime;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(long joinTime) {
        this.joinTime = joinTime;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
}
