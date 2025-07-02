package com.jjg.game.table.common.data;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.room.sample.bean.BetRobotCfg;
import com.jjg.game.room.sample.bean.ChessRobotCfg;
import com.jjg.game.room.sample.bean.RobotActionCfg;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存table类的配置数据
 *
 * @author 2CL
 */
@Repository
public class TableSampleDataHolder implements ConfigExcelChangeListener {

    // 机器人押注策略缓存
    private static final Map<Integer, Map<Integer, Integer>> BET_ACTION_DATA_CACHE = new HashMap<>();

    @Override
    public void change(String className) {
        if (className.equals(ChessRobotCfg.class.getSimpleName()) || className.equals(BetRobotCfg.class.getSimpleName())) {
            cacheBetActionData();
        }
    }

    public static void cacheBetActionData() {
        BET_ACTION_DATA_CACHE.clear();
        for (Map.Entry<Integer, RobotActionCfg> entry : GameDataManager.getRobotActionCfgMap().entrySet()) {
            BET_ACTION_DATA_CACHE.computeIfAbsent(entry.getValue().getActionID(), k -> new HashMap<>())
                .put(entry.getValue().getGameID(), entry.getKey());
        }
    }

    public static Integer getBetActionDataCache(int actionId, int gameId) {
        return BET_ACTION_DATA_CACHE.getOrDefault(actionId, new HashMap<>()).get(gameId);
    }
}
