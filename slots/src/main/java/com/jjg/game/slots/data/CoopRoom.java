package com.jjg.game.slots.data;

import com.jjg.game.sim.constant.CoopTaskConst;
import com.jjg.game.sim.data.CoopTaskRule;
import com.jjg.game.slots.manager.CoopRoomManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协作任务房间 (slots 节点内存态, 全体成员被路由到本节点, 单房间内聚合进度)。
 * <p>
 * 线程模型: 成员各自的旋转在各自玩家线程执行, 房间状态变更统一经 {@link CoopRoomManager}
 * 以 synchronized(room) 串行化; 房间生命周期短(时限内结束), 锁竞争极低。
 *
 * @author 11
 * @date 2026/7/6
 */
public class CoopRoom {
    private final long roomId;
    private final int taskId;
    private final long ownerId;
    private final int gameType;
    private final int roomCfgId;
    //任务规则 (创建时解析缓存, 避免每次旋转重复解析配置)
    private final CoopTaskRule rule;
    private final long createTime;

    //状态 {@link CoopTaskConst.RoomStatus}
    private volatile int status = CoopTaskConst.RoomStatus.WAITING;
    //成员 playerId -> member (并发安全: 旋转钩子/赠礼在锁外只读; 结构变更仍在房间锁内; 座位序由 seat 字段承载)
    private final Map<Long, CoopMember> members = new ConcurrentHashMap<>();
    //座位自增序
    private int seatSeq;
    //上次发送频道邀请时间 (限频; 房主请求为单线程串行, volatile 足够)
    private volatile long lastInviteTime;

    //全队共享特殊事件累计
    private long sharedProgress;
    private long startTime;
    //失败兜底时限 (start + rule.durationMinutes; 0=不限)
    private long deadline;
    private long finishTime;
    private boolean success;
    //结算必须由 sim 明确确认后才能删除 Redis 路由记录和回收房间
    private boolean settlementAcked;
    private boolean settlementInFlight;
    private long lastSettlementAttempt;
    private List<Long> settlementHelperIds = List.of();

    public CoopRoom(long roomId, int taskId, long ownerId, int gameType, int roomCfgId, CoopTaskRule rule) {
        this(roomId, taskId, ownerId, gameType, roomCfgId, rule, System.currentTimeMillis());
    }

    public CoopRoom(long roomId, int taskId, long ownerId, int gameType, int roomCfgId,
                    CoopTaskRule rule, long createTime) {
        this.roomId = roomId;
        this.taskId = taskId;
        this.ownerId = ownerId;
        this.gameType = gameType;
        this.roomCfgId = roomCfgId;
        this.rule = rule;
        this.createTime = createTime;
    }

    public CoopMember addMember(long playerId) {
        CoopMember member = new CoopMember(playerId, ++seatSeq);
        members.put(playerId, member);
        return member;
    }

    public long getRoomId() {
        return roomId;
    }

    public int getTaskId() {
        return taskId;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public int getGameType() {
        return gameType;
    }

    public int getRoomCfgId() {
        return roomCfgId;
    }

    public CoopTaskRule getRule() {
        return rule;
    }

    public long getCreateTime() {
        return createTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<Long, CoopMember> getMembers() {
        return members;
    }

    /**
     * 按座位序 (进房顺序) 返回成员 (快照展示/份额余数分配等顺序敏感场景用)。
     */
    public List<CoopMember> membersBySeat() {
        List<CoopMember> list = new ArrayList<>(members.values());
        list.sort(Comparator.comparingInt(CoopMember::getSeat));
        return list;
    }

    public long getLastInviteTime() {
        return lastInviteTime;
    }

    public void setLastInviteTime(long lastInviteTime) {
        this.lastInviteTime = lastInviteTime;
    }

    public long getSharedProgress() {
        return sharedProgress;
    }

    public void setSharedProgress(long sharedProgress) {
        this.sharedProgress = sharedProgress;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSettlementAcked() {
        return settlementAcked;
    }

    public void setSettlementAcked(boolean settlementAcked) {
        this.settlementAcked = settlementAcked;
    }

    public boolean isSettlementInFlight() {
        return settlementInFlight;
    }

    public void setSettlementInFlight(boolean settlementInFlight) {
        this.settlementInFlight = settlementInFlight;
    }

    public long getLastSettlementAttempt() {
        return lastSettlementAttempt;
    }

    public void setLastSettlementAttempt(long lastSettlementAttempt) {
        this.lastSettlementAttempt = lastSettlementAttempt;
    }

    public List<Long> getSettlementHelperIds() {
        return settlementHelperIds;
    }

    public void setSettlementHelperIds(List<Long> settlementHelperIds) {
        this.settlementHelperIds = List.copyOf(settlementHelperIds);
    }
}
