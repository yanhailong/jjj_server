package com.jjg.game.slots.data;

import com.jjg.game.sim.constant.CoopTaskConst;
import com.jjg.game.sim.data.CoopTaskRule;
import com.jjg.game.slots.manager.CoopRoomManager;

import java.util.LinkedHashMap;
import java.util.Map;

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
    //成员 playerId -> member (LinkedHashMap 保持进房座位序; 变更均在房间锁内)
    private final Map<Long, CoopMember> members = new LinkedHashMap<>();
    //座位自增序
    private int seatSeq;

    //全队共享特殊事件累计
    private int sharedProgress;
    private long startTime;
    //失败兜底时限 (start + rule.durationMinutes; 0=不限)
    private long deadline;
    private long finishTime;
    private boolean success;

    public CoopRoom(long roomId, int taskId, long ownerId, int gameType, int roomCfgId, CoopTaskRule rule) {
        this.roomId = roomId;
        this.taskId = taskId;
        this.ownerId = ownerId;
        this.gameType = gameType;
        this.roomCfgId = roomCfgId;
        this.rule = rule;
        this.createTime = System.currentTimeMillis();
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

    public int getSharedProgress() {
        return sharedProgress;
    }

    public void setSharedProgress(int sharedProgress) {
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
}
