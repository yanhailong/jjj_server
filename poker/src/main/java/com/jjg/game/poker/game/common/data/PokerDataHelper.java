package com.jjg.game.poker.game.common.data;

import cn.hutool.core.lang.generator.SnowflakeGenerator;
import com.jjg.game.core.data.Card;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.sample.GameDataManager;
import com.jjg.game.poker.game.sample.bean.PokerPoolCfg;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/8/5 18:13
 */
public class PokerDataHelper {
    //poolId->pokerPool表Id->牌
    private static Map<Integer, Map<Integer, PokerCard>> allCardMapListMap;
    //id生成器
    private final static SnowflakeGenerator SNOWFLAKE = new SnowflakeGenerator(2, 0);

    /**
     * 初始化缓存 allCardMapListMap
     */
    public static void initData() {
        List<PokerPoolCfg> cfgList = GameDataManager.getPokerPoolCfgList();
        Map<Integer, Map<Integer, PokerCard>> mapHashMap = new HashMap<>();
        for (PokerPoolCfg cfg : cfgList) {
            Map<Integer, PokerCard> pokerCardMap = mapHashMap.computeIfAbsent(cfg.getPoolId(), (key) -> new HashMap<>());
            PokerCardUtils.EPokerHumanStr pokerHumanStrByHumanStr = PokerCardUtils.EPokerHumanStr.getPokerHumanStrByHumanStr(cfg.getPoints());
            PokerCardUtils.EPokerSuit suitByConfig = PokerCardUtils.getSuitByConfig(cfg.getSuit());
            if (Objects.nonNull(suitByConfig) && Objects.nonNull(pokerHumanStrByHumanStr)) {
                Card card = new Card(suitByConfig.getSuitId() - 1, pokerHumanStrByHumanStr.getPointId());
                pokerCardMap.put(cfg.getId(), new PokerCard(cfg.getId(), cfg.getSuitNum(), cfg.getPointsNum(), card.getValue()));
            } else {
                throw new RuntimeException("配置错误");
            }
        }
        allCardMapListMap = mapHashMap;
    }

    /**
     * 获取id
     */
    public static long getNextId() {
        return SNOWFLAKE.next();
    }

    /**
     * 获取配置id对应的PokerCard
     *
     * @param poolId PokerPool池id
     * @return 该池id下的所有牌
     */
    public static Map<Integer, PokerCard> getCardListMap(int poolId) {
        return allCardMapListMap.get(poolId);
    }

    /**
     * 获取poker每个阶段的执行时间
     */
    public static int getExecutionTime(BasePokerGameDataVo gameDataVo, PokerPhase phase) {
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        return roomCfg.getChess_stageOrder().getOrDefault(phase.getValue(), 0);
    }

    /**
     * 获取牌的客户端对应的id
     *
     * @param cardCfgId pokerPool配置表id
     * @param poolId    pokerPool池id
     */
    public static List<Integer> getClientId(List<Integer> cardCfgId, int poolId) {
        Map<Integer, PokerCard> cardMap = getCardListMap(poolId);
        return cardCfgId.stream().map(id -> cardMap.get(id).getClientId()).collect(Collectors.toList());
    }
}
