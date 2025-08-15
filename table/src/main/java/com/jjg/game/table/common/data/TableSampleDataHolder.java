package com.jjg.game.table.common.data;

import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BetRobotCfg;
import com.jjg.game.sampledata.bean.ChessRobotCfg;
import com.jjg.game.sampledata.bean.RobotActionCfg;
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
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(ChessRobotCfg.EXCEL_NAME, TableSampleDataHolder::cacheBetActionData)
            .addInitSampleFileObserveWithCallBack(BetRobotCfg.EXCEL_NAME, TableSampleDataHolder::cacheBetActionData);
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
