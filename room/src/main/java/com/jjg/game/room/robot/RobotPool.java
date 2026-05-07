package com.jjg.game.room.robot;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 本节点机器人池。
 *
 * @author 2CL
 */
public class RobotPool {

    // 已经分配到房间流程中的机器人ID。
    private final Set<Long> activeRobotIds = new HashSet<>();
    private final ReentrantLock lock = new ReentrantLock();
    // itemId -> 货币数量 -> robotId -> 机器人
    private Map<Integer, NavigableMap<Long, LinkedHashMap<Long, RobotPoolEntry>>> robotPoolByItemId = new HashMap<>();

    /**
     * 根据最新配置重建可用机器人池。
     * 已经分配出去的机器人保留在 activeRobotIds 中，不会因为热加载重新回到池内。
     *
     * @param robotPoolEntries 最新可用机器人配置快照
     */
    public void reload(Collection<RobotPoolEntry> robotPoolEntries) {
        Map<Integer, NavigableMap<Long, LinkedHashMap<Long, RobotPoolEntry>>> tempRobotPoolByItemId = new HashMap<>();
        lock.lock();
        try {
            for (RobotPoolEntry robotPoolEntry : robotPoolEntries) {
                // 配置热加载时跳过已借出的机器人，避免同一个机器人被重复分配到多个房间。
                if (activeRobotIds.contains(robotPoolEntry.robotId())) {
                    continue;
                }
                addRobotToPool(tempRobotPoolByItemId, robotPoolEntry);
            }
            robotPoolByItemId = tempRobotPoolByItemId;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 按房间入场条件从池中获取一个机器人。
     * 获取成功后会立即从全部货币索引中删除，并加入 activeRobotIds。
     *
     * @param request 机器人获取条件
     * @return 匹配到的机器人；没有可用机器人时返回 null
     */
    public RobotPoolEntry acquire(RobotAcquireRequest request) {
        lock.lock();
        try {
            return acquireLocked(request);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 回收已借出的机器人。
     * 只有 activeRobotIds 中存在的机器人才能被回收到池内，防止重复回收。
     *
     * @param robotPoolEntry 机器人池条目
     */
    public void recycle(RobotPoolEntry robotPoolEntry) {
        if (robotPoolEntry == null) {
            return;
        }
        lock.lock();
        try {
            if (!activeRobotIds.remove(robotPoolEntry.robotId())) {
                return;
            }
            removeRobotFromIndexedPools(robotPoolEntry);
            // 回收时按 entry 中的每种货币重新挂回对应索引。
            addRobotToPool(robotPoolByItemId, robotPoolEntry);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 丢弃已借出的机器人，只释放借出状态，不重新挂回池内。
     * 配置被删除或禁用时使用，避免 activeRobotIds 残留导致后续恢复配置也无法入池。
     *
     * @param robotId 当前节点内生成后的真实机器人ID
     */
    public void discard(long robotId) {
        lock.lock();
        try {
            activeRobotIds.remove(robotId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 判断指定货币池中是否还有可分配机器人。
     *
     * @param itemId 房间入场使用的货币道具ID
     */
    public boolean hasAvailableRobot(int itemId) {
        lock.lock();
        try {
            NavigableMap<Long, LinkedHashMap<Long, RobotPoolEntry>> robotPool = robotPoolByItemId.get(itemId);
            return robotPool != null && !robotPool.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 锁内执行的机器人筛选逻辑。
     * 先通过 TreeMap.subMap 跳到满足最低入场金额的区间，再按等级和上限过滤。
     */
    private RobotPoolEntry acquireLocked(RobotAcquireRequest request) {
        NavigableMap<Long, LinkedHashMap<Long, RobotPoolEntry>> robotPool = robotPoolByItemId.get(request.itemId());
        if (robotPool == null || robotPool.isEmpty()) {
            return null;
        }
        long maxAmount = request.enterMax() == -1 ? Long.MAX_VALUE : request.enterMax();
        if (maxAmount < request.enterLimit()) {
            return null;
        }
        // 先在金额索引层面收窄上下限，避免遍历一定不符合房间上限的机器人。
        NavigableMap<Long, LinkedHashMap<Long, RobotPoolEntry>> candidatePool = robotPool.subMap(request.enterLimit(), true, maxAmount, true);
        if (candidatePool.isEmpty()) {
            return null;
        }
        RobotPoolEntry selectedRobotPoolEntry = null;
        search:
        for (Map.Entry<Long, LinkedHashMap<Long, RobotPoolEntry>> amountEntry : candidatePool.entrySet()) {
            LinkedHashMap<Long, RobotPoolEntry> robotEntries = amountEntry.getValue();
            for (RobotPoolEntry robotPoolEntry : robotEntries.values()) {
                // 等级不满足时不移除，后续低等级房间仍可能使用该机器人。
                if (robotPoolEntry.robotCfg().getPlayerLevel() < request.playerLevelLimit()) {
                    continue;
                }
                long amount = robotPoolEntry.getAmount(request.itemId());
                // 金额超过房间上限时同样保留在池里，等待更高档位房间使用。
                if (amount < request.enterLimit() || amount > maxAmount) {
                    continue;
                }
                selectedRobotPoolEntry = robotPoolEntry;
                break search;
            }
        }
        if (selectedRobotPoolEntry == null) {
            return null;
        }
        // 一个机器人在多个货币索引中都有位置，借出时必须全部摘除。
        removeRobotFromIndexedPools(selectedRobotPoolEntry);
        activeRobotIds.add(selectedRobotPoolEntry.robotId());
        return selectedRobotPoolEntry;
    }

    /**
     * 将机器人按所有可用货币金额挂入索引。
     *
     * @param robotPoolByItemId 目标索引
     * @param robotPoolEntry    机器人池条目
     */
    private void addRobotToPool(Map<Integer, NavigableMap<Long, LinkedHashMap<Long, RobotPoolEntry>>> robotPoolByItemId, RobotPoolEntry robotPoolEntry) {
        for (Map.Entry<Integer, Long> entry : robotPoolEntry.itemAmounts().entrySet()) {
            robotPoolByItemId
                    .computeIfAbsent(entry.getKey(), key -> new TreeMap<>())
                    .computeIfAbsent(entry.getValue(), key -> new LinkedHashMap<>())
                    .put(robotPoolEntry.robotId(), robotPoolEntry);
        }
    }

    /**
     * 从机器人涉及到的货币索引中定点删除。
     * 依赖 RobotPoolEntry 记录的 itemId -> amount，不扫描整个池。
     */
    private void removeRobotFromIndexedPools(RobotPoolEntry robotPoolEntry) {
        for (Map.Entry<Integer, Long> entry : robotPoolEntry.itemAmounts().entrySet()) {
            NavigableMap<Long, LinkedHashMap<Long, RobotPoolEntry>> robotPool = robotPoolByItemId.get(entry.getKey());
            if (robotPool == null) {
                continue;
            }
            LinkedHashMap<Long, RobotPoolEntry> robotEntries = robotPool.get(entry.getValue());
            if (robotEntries == null) {
                continue;
            }
            robotEntries.remove(robotPoolEntry.robotId());
            if (robotEntries.isEmpty()) {
                robotPool.remove(entry.getValue());
                if (robotPool.isEmpty()) {
                    robotPoolByItemId.remove(entry.getKey());
                }
            }
        }
    }
}
