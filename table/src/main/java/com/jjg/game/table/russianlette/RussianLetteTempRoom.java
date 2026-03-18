package com.jjg.game.table.russianlette;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.listener.IPlayerRoomEventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 俄罗斯转盘临时房间（观察者管理）
 * <p>
 * 跟踪正在浏览俄罗斯转盘房间列表但尚未进入具体房间的玩家（观察者）。
 * 当游戏阶段变化时，通过 {@link RussianLetteMessageBuilder} 向这些观察者推送摘要更新。
 *
 * @author lhc
 */
@Component
public class RussianLetteTempRoom implements IPlayerRoomEventListener {

    /** 观察俄罗斯转盘房间列表的玩家集合（选房界面），按场次配置 ID 分组 */
    private final Map<Integer, Set<Long>> observerPlayers = new ConcurrentHashMap<>();

    /** 请求了 OtherSummaryList 的玩家集合（游戏中查看同场次其他房间），按场次配置 ID 分组 */
    private final Map<Integer, Set<Long>> otherSummaryObservers = new ConcurrentHashMap<>();

    private final TaskManager taskManager;

    public RussianLetteTempRoom(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public int[] getGameTypes() {
        return new int[]{EGameType.RUSSIAN_ROULETTE.getGameTypeId()};
    }

    @Override
    public void enter(PFSession session, PlayerController playerController, PlayerSessionInfo playerSessionInfo) {
        // 玩家当前仍在真实房间场景时，不应加入临时房间
        if (playerController.getPlayer().getRoomId() > 0 && playerController.getScene() instanceof AbstractRoomController<?, ?>) {
            return;
        }
        session.setWorkId(playerController.playerId());
        // 加载任务
        taskManager.loadTaskData(playerController.playerId());
        observerPlayers
                .computeIfAbsent(playerSessionInfo.getRoomCfgId(), k -> new ConcurrentHashSet<>())
                .add(playerController.playerId());
    }

    @Override
    public void exit(PFSession session, PlayerController playerController) {
        long playerId = playerController.playerId();
        observerPlayers.values().forEach(playerIds -> playerIds.remove(playerId));
        otherSummaryObservers.values().forEach(playerIds -> playerIds.remove(playerId));
    }

    @Override
    public boolean containsPlayer(long playerId) {
        return observerPlayers.values().stream().anyMatch(playerIds -> playerIds.contains(playerId));
    }

    public Set<Long> getObserverPlayers(int roomCfgId) {
        return observerPlayers.getOrDefault(roomCfgId, Set.of());
    }

    // =========================================================================
    // OtherSummary 观察者（游戏中查看同场次其他房间的玩家）
    // =========================================================================

    /**
     * 注册 OtherSummary 观察者
     * <p>玩家请求 {@code reqRussianLetteOtherSummaryList} 后调用，
     * 后续同场次房间状态变化时会收到 {@code NotifyRussianLetteTableSummary} 推送。</p>
     */
    public void addOtherSummaryObserver(int roomCfgId, long playerId) {
        otherSummaryObservers
                .computeIfAbsent(roomCfgId, k -> new ConcurrentHashSet<>())
                .add(playerId);
    }

    /**
     * 移除 OtherSummary 观察者
     * <p>玩家退出房间、切换房间、完全退出游戏时调用。</p>
     */
    public void removeOtherSummaryObserver(long playerId) {
        otherSummaryObservers.values().forEach(playerIds -> playerIds.remove(playerId));
    }

    /**
     * 获取指定场次的 OtherSummary 观察者集合
     */
    public Set<Long> getOtherSummaryObservers(int roomCfgId) {
        return otherSummaryObservers.getOrDefault(roomCfgId, Set.of());
    }
}
