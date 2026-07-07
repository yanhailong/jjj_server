package com.jjg.game.sim.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 协作房间路由记录 (Redis, key = coopRoom:{roomId})。
 * <p>
 * 职责仅两个: 1) 加入者按 nodePath 路由到房间所在 slots 节点; 2) hall 侧加入前的廉价预检查
 * (存在/未开始/未满员)。成员与状态的权威数据在 slots 节点内存房间, 变更时由 slots 覆写本记录;
 * 预检查存在竞态属可接受 (slots 侧最终判定)。TTL 兜底节点崩溃后的记录残留。
 *
 * @author 11
 * @date 2026/7/6
 */
public class CoopRoomRecord {
    private long roomId;
    private int taskId;
    //发起者 (房主)
    private long ownerId;
    private int gameType;
    //进入游戏所用房间配置id (warehouse.xlsx, 加入者切节点后进同一游戏场景)
    private int roomCfgId;
    //房间所在 slots 节点路径
    private String nodePath;
    //状态 {@link com.jjg.game.sim.constant.CoopTaskConst.RoomStatus}
    private int status;
    //成员 (含房主; 快照, 权威在 slots 房间)
    private List<Long> memberIds = new ArrayList<>();
    //总人数上限 (含房主)
    private int maxMembers;
    private long createTime;
    //FINISHED 结算重投载荷
    private boolean success;
    private long finishTime;
    private long sharedProgress;
    private List<Long> settlementHelperIds = new ArrayList<>();

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getRoomCfgId() {
        return roomCfgId;
    }

    public void setRoomCfgId(int roomCfgId) {
        this.roomCfgId = roomCfgId;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<Long> getMemberIds() {
        if (memberIds == null) {
            memberIds = new ArrayList<>();
        }
        return memberIds;
    }

    public void setMemberIds(List<Long> memberIds) {
        this.memberIds = memberIds == null ? new ArrayList<>() : memberIds;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public long getSharedProgress() {
        return sharedProgress;
    }

    public void setSharedProgress(long sharedProgress) {
        this.sharedProgress = sharedProgress;
    }

    public List<Long> getSettlementHelperIds() {
        if (settlementHelperIds == null) {
            settlementHelperIds = new ArrayList<>();
        }
        return settlementHelperIds;
    }

    public void setSettlementHelperIds(List<Long> settlementHelperIds) {
        this.settlementHelperIds = settlementHelperIds == null ? new ArrayList<>() : settlementHelperIds;
    }
}
