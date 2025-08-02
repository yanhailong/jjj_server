package com.jjg.game.poker.game.texas.data;

import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.poker.game.texas.sample.GameDataManager;
import com.jjg.game.poker.game.texas.sample.bean.PokerPoolCfg;
import com.jjg.game.poker.game.texas.sample.bean.TexasCfg;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/8/2 15:20
 */
public class TexasDataHelper {
    //房间配置id->pokerPoolId->牌
    private static Map<Integer, Map<Integer, PokerCard>> cardMapListMap;

    /**
     * 初始化缓存
     */
    public static void initData() {
        Map<Integer, Map<Integer, PokerCard>> map = new HashMap<>();
        for (TexasCfg texasCfg : GameDataManager.getTexasCfgList()) {
            List<PokerPoolCfg> cfgList = GameDataManager.getPokerPoolCfgList();
            Map<Integer, PokerCard> collect = cfgList.stream().filter(pokerPoolCfg -> texasCfg.getPokerPool() == pokerPoolCfg.getPoolId())
                    .collect(Collectors.toMap(PokerPoolCfg::getId, cfg -> new PokerCard(cfg.getId(), cfg.getSuitNum(), cfg.getPointsNum())));
            map.put(texasCfg.getId(), collect);
        }
        cardMapListMap = Collections.unmodifiableMap(map);
    }

    public static Map<Integer, PokerCard> getCardListMap(int id) {
        return cardMapListMap.get(id);
    }

    public static long getDefaultCoinsNum(TexasGameDataVo gameDataVo) {
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        TexasCfg texasCfg = GameDataManager.getTexasCfg(roomCfg.getId());
        return texasCfg.getCoinsNum();
    }

    public static int getExecutionTime(BasePokerGameDataVo gameDataVo, PokerPhase phase) {
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        return roomCfg.getChessStageOrder().getOrDefault(phase.getValue(), 0);
    }
}
