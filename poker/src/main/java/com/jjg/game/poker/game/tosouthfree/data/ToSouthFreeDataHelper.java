package com.jjg.game.poker.game.tosouthfree.data;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.tosouthfree.room.data.ToSouthFreeGameDataVo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import com.jjg.game.sampledata.bean.SouthernMoneyCfg;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/8/6 10:19
 */
public class ToSouthFreeDataHelper extends PokerDataHelper {
    private ToSouthFreeDataHelper() {
    }

    /**
     * 根据权重获取倍数
     * 配置格式: List<List<Integer>>
     * 每个子List: [倍数, 权重]
     */
    public static int getMultipleByWeight(List<List<Integer>> configList) {
        if (configList == null || configList.isEmpty()) {
            return 1;
        }
        Map<Integer, Integer> weightMap = new LinkedHashMap<>();
        for (List<Integer> item : configList) {
            if (item.size() == 2) {
                weightMap.put(item.get(0), item.get(1));
            }
        }
        return RandomUtils.randomByWeight(weightMap);
    }

    /**
     * 初始化机器人策略
     */
    public static void initData() {

    }

    public static SouthernMoneyCfg getSouthernMoneyCfg(ToSouthFreeGameDataVo gameDataVo) {
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        return GameDataManager.getSouthernMoneyCfg(roomCfg.getId());
    }

    public static int getPoolId(ToSouthFreeGameDataVo gameDataVo) {
        SouthernMoneyCfg cfg = getSouthernMoneyCfg(gameDataVo);
        return cfg.getPoolId();
    }
}
