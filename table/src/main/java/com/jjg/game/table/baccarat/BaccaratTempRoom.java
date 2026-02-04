package com.jjg.game.table.baccarat;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.room.listener.IPlayerRoomEventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 百家乐临时房间 TODO 当前只获取了玩家当前服的百家乐房间，如果后续关闭游戏服，但玩家一直停留在百家乐
 *
 * @author 2CL
 */
@Component
public class BaccaratTempRoom implements IPlayerRoomEventListener {
    // 观察百家乐路单的玩家集合
    private final Map<Integer, Set<Long>> baccaratObserverPlayers =
            new ConcurrentHashMap<>();

    @Override
    public int[] getGameTypes() {
        return new int[]{EGameType.BACCARAT.getGameTypeId()};
    }

    @Override
    public void enter(PFSession session, PlayerController playerController, PlayerSessionInfo playerSessionInfo) {
        baccaratObserverPlayers
                .computeIfAbsent(playerSessionInfo.getRoomCfgId(), k -> new ConcurrentHashSet<>())
                .add(playerController.playerId());
    }

    @Override
    public void exit(PFSession session, PlayerController playerController) {
        int roomCfgId = playerController.getPlayer().getRoomCfgId();
        if (baccaratObserverPlayers.containsKey(roomCfgId)) {
            baccaratObserverPlayers.get(roomCfgId).remove(playerController.playerId());
        }
    }

    public Set<Long> getBaccaratObserverPlayers(int roomCfgId) {
        return baccaratObserverPlayers.getOrDefault(roomCfgId, Set.of());
    }
}
