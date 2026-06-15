package com.jjg.game.alliance.data;

import com.jjg.game.alliance.constant.AllianceConst;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 联盟数据 (每联盟一文档, _id = 6 位数字联盟 id)。
 * <p>
 * 持久化策略: DB 权威 + 字段级原子更新 (见 {@code AllianceDao})。联盟是全服共享数据,
 * 同一联盟的写操作可能来自多个 hall 节点, 全部用 Mongo 条件更新解决竞态(防超员/重复接取/重复帮助),
 * 不依赖任何单节点内存态; 读路径走 {@code AllianceCacheService} 本地缓存 + 失效广播。
 * <p>
 * 成员上限 20+19*10=210, 申请/任务池/互助订单均有界, 单文档体积可控, 子结构全部内嵌。
 *
 * @author 11
 * @date 2026/6/11
 */
@Document("allianceData")
public class AllianceData {
    //联盟 id (6 位数字, Redis 发号)
    @Id
    private long allianceId;
    //名称
    private String name;
    //图标 (系统头像 id)
    private int icon;
    //公告/描述
    private String notice;
    //盟主
    private long leaderId;
    //创建者 (每用户最多创建 1 个的依据记录在玩家侧文档)
    private long creatorId;
    //创建时间(ms)
    private long createTime;

    //等级 (1-20, 由累计声誉推导, 只升不降)
    private int level = 1;
    //累计声誉值 (只增不消耗)
    private long reputation;

    //入盟最低赌场等级 (1-99)
    private int joinMinCasinoLevel = AllianceConst.Cfg.JOIN_LEVEL_MIN;
    //入盟是否需要审核
    private boolean joinNeedAudit;

    //成员数冗余计数: 与 members 同步原子维护, 配合 memberCount < cap 的条件更新防超员
    private int memberCount;
    //成员 pid -> 条目
    private Map<Long, AllianceMember> members = new HashMap<>();

    //入盟申请 pid -> 条目 (有界, 超期惰性清理)
    private Map<Long, AllianceApplication> applications = new HashMap<>();

    //任务池 (TASK_POOL_SIZE 条, 整点惰性补齐; 接取即原子移除)
    private List<AllianceTaskSlot> tasks = new ArrayList<>();
    //任务池最近补齐的整点 (yyyyMMddHH), 条件更新该值保证多节点只补一次
    private long taskRefreshHour;

    //互助求助订单 orderId -> 订单 (有界, 超时惰性清理)
    private Map<Long, AllianceHelpOrder> helpOrders = new HashMap<>();

    public long getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(long allianceId) {
        this.allianceId = allianceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public long getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(long leaderId) {
        this.leaderId = leaderId;
    }

    public long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(long creatorId) {
        this.creatorId = creatorId;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getReputation() {
        return reputation;
    }

    public void setReputation(long reputation) {
        this.reputation = reputation;
    }

    public int getJoinMinCasinoLevel() {
        return joinMinCasinoLevel;
    }

    public void setJoinMinCasinoLevel(int joinMinCasinoLevel) {
        this.joinMinCasinoLevel = joinMinCasinoLevel;
    }

    public boolean isJoinNeedAudit() {
        return joinNeedAudit;
    }

    public void setJoinNeedAudit(boolean joinNeedAudit) {
        this.joinNeedAudit = joinNeedAudit;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public Map<Long, AllianceMember> getMembers() {
        return members;
    }

    public void setMembers(Map<Long, AllianceMember> members) {
        this.members = members == null ? new HashMap<>() : members;
    }

    public Map<Long, AllianceApplication> getApplications() {
        return applications;
    }

    public void setApplications(Map<Long, AllianceApplication> applications) {
        this.applications = applications == null ? new HashMap<>() : applications;
    }

    public List<AllianceTaskSlot> getTasks() {
        return tasks;
    }

    public void setTasks(List<AllianceTaskSlot> tasks) {
        this.tasks = tasks == null ? new ArrayList<>() : tasks;
    }

    public long getTaskRefreshHour() {
        return taskRefreshHour;
    }

    public void setTaskRefreshHour(long taskRefreshHour) {
        this.taskRefreshHour = taskRefreshHour;
    }

    public Map<Long, AllianceHelpOrder> getHelpOrders() {
        return helpOrders;
    }

    public void setHelpOrders(Map<Long, AllianceHelpOrder> helpOrders) {
        this.helpOrders = helpOrders == null ? new HashMap<>() : helpOrders;
    }

    // ---------------------------------------------------------------------
    // 便捷方法
    // ---------------------------------------------------------------------

    public boolean isMember(long playerId) {
        return members != null && members.containsKey(playerId);
    }

    public boolean isLeader(long playerId) {
        return leaderId == playerId;
    }

    /**
     * 近 N 天活跃成员数 (对决报名模式2用; 单文档内统计, 零额外查询)
     */
    public int activeMemberCount(int days) {
        if (members == null || members.isEmpty()) {
            return 0;
        }
        long since = System.currentTimeMillis() - days * 24L * 3600 * 1000;
        int count = 0;
        for (AllianceMember m : members.values()) {
            if (m.getLastActiveTime() >= since) {
                count++;
            }
        }
        return count;
    }

    /**
     * 按 uid 取任务池条目, 无则 null
     */
    public AllianceTaskSlot findTask(long taskUid) {
        if (tasks == null) {
            return null;
        }
        for (AllianceTaskSlot slot : tasks) {
            if (slot.getUid() == taskUid) {
                return slot;
            }
        }
        return null;
    }
}
