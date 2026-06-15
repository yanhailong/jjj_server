package com.jjg.game.alliance.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 互助求助订单 (内嵌于 {@link AllianceData#getHelpOrders()})。
 * <p>
 * helpers 内嵌记录已帮助玩家, 配合"helpers.pid 不存在"的条件更新保证每单每人只帮一次;
 * 订单有界(每人每日求助上限)且超时惰性清理, 不会无界膨胀。
 *
 * @author 11
 * @date 2026/6/11
 */
public class AllianceHelpOrder {
    //订单唯一 id (雪花)
    private long orderId;
    //订单类型 (AllianceConst.HelpType)
    private int type;
    //求助者
    private long ownerId;
    //求助目标: TASK=任务实例uid, BUILD_SPEEDUP=buildingId
    private long targetId;
    //展示名 (任务名/建筑名, 由发起方传入, 仅前端卡片展示用)
    private String targetName;
    //发起时间(ms)
    private long createTime;
    //可被帮助次数上限
    private int maxHelp;
    private int helpCount;
    //已帮助玩家 pid -> 帮助时间(ms)
    private Map<Long, Long> helpers = new HashMap<>();

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public long getTargetId() {
        return targetId;
    }

    public void setTargetId(long targetId) {
        this.targetId = targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getMaxHelp() {
        return maxHelp;
    }

    public void setMaxHelp(int maxHelp) {
        this.maxHelp = maxHelp;
    }

    public int getHelpCount() {
        return helpCount;
    }

    public void setHelpCount(int helpCount) {
        this.helpCount = helpCount;
    }

    public Map<Long, Long> getHelpers() {
        return helpers;
    }

    public void setHelpers(Map<Long, Long> helpers) {
        this.helpers = helpers == null ? new HashMap<>() : helpers;
    }

    public int helpedCount() {
        int helperSize = helpers == null ? 0 : helpers.size();
        return Math.max(helpCount, helperSize);
    }

    public boolean helpedBy(long playerId) {
        return helpers != null && helpers.containsKey(playerId);
    }

    /**
     * 是否已满(不可再被帮助)
     */
    public boolean full() {
        return helpedCount() >= maxHelp;
    }
}
