package com.jjg.game.sim;

import com.jjg.game.common.listener.OnSwitchNode;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.ExitType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sim.data.SimPlayerGameData;
import com.jjg.game.sim.service.SimPlayerGameDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟经营游戏管理器
 *
 * @author 11
 * @date 2026/5/15
 */
@Component
public class SimManager implements OnSwitchNode {
    @Autowired
    private SimPlayerGameDataService simPlayerGameDataService;

    //储存玩家游戏数据
    protected Map<Long, SimPlayerGameData> gameDataMap = new ConcurrentHashMap<>();

    /**
     * 初始化
     */
    public void init() {

    }

    /**
     * 玩家进入游戏
     *
     * @param playerController
     */
    public void onEnterGame(PlayerController playerController) {

    }

    /**
     * 玩家退出游戏
     *
     * @param playerId
     */
    public void onExitGame(long playerId, ExitType exitType) {
        saveToRedis(playerId);
    }

    /**
     * 获取gameData
     *
     * @param playerId
     * @param fromDB
     * @return
     */
    public SimPlayerGameData getGameData(long playerId, boolean fromDB) {
        SimPlayerGameData data = this.gameDataMap.get(playerId);
        if (data != null) {
            return data;
        }

        if (fromDB) {
            data = simPlayerGameDataService.getSimPlayerGameData(playerId, true);
            if (data != null) {
                this.gameDataMap.put(playerId, data);
            }
        }
        return data;
    }

    /**
     * 服务器关闭
     */
    public void shutdown() {

    }

    /**
     * 将SimPlayerGameData从内存移除，并保存到redis
     *
     * @param playerId
     */
    private void saveToRedis(long playerId) {
        SimPlayerGameData data = this.gameDataMap.remove(playerId);
        if (data != null) {
            this.simPlayerGameDataService.saveToRedis(data);
        }
    }

    @Override
    public void onSwitchNodeAction(PFSession pfSession) {
        saveToRedis(pfSession.getPlayerId());
    }
}
