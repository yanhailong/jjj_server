package com.jjg.game.poker.game.common.data;

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
    //房间配置id->pokerPoolId->牌
    private static Map<Integer, Map<Integer, PokerCard>> cardMapListMap;
    /**
     * 初始化缓存 cardMapListMap
     *
     */
    public static Map<Integer, PokerCard> initCardMapListMap(int poolId) {
        List<PokerPoolCfg> cfgList = GameDataManager.getPokerPoolCfgList();
        return cfgList.stream().filter(pokerPoolCfg -> poolId == pokerPoolCfg.getPoolId())
                .collect(Collectors.toMap(PokerPoolCfg::getId, cfg -> {
                    PokerCardUtils.EPokerHumanStr pokerHumanStrByHumanStr = PokerCardUtils.EPokerHumanStr.getPokerHumanStrByHumanStr(cfg.getPoints());
                    PokerCardUtils.EPokerSuit suitByConfig = PokerCardUtils.getSuitByConfig(cfg.getSuit());
                    if (Objects.nonNull(suitByConfig) && Objects.nonNull(pokerHumanStrByHumanStr)) {
                        Card card = new Card(suitByConfig.getSuitId() - 1, pokerHumanStrByHumanStr.getPointId());
                        return new PokerCard(cfg.getId(), cfg.getSuitNum(), cfg.getPointsNum(), card.getValue());
                    } else {
                        throw new RuntimeException("配置错误");
                    }
                }));
    }

    public static Map<Integer, PokerCard> getCardListMap(int id) {
        return cardMapListMap.get(id);
    }

    public static void setCardMapListMap(Map<Integer, Map<Integer, PokerCard>> cardMapListMap) {
        PokerDataHelper.cardMapListMap = cardMapListMap;
    }

    public static int getExecutionTime(BasePokerGameDataVo gameDataVo, PokerPhase phase) {
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        return roomCfg.getChess_stageOrder().getOrDefault(phase.getValue(), 0);
    }

    public static List<Integer> getClientId(List<Integer> cardCfgId, int cfgId) {
        Map<Integer, PokerCard> cardMap = getCardListMap(cfgId);
        return cardCfgId.stream().map(id -> cardMap.get(id).getClientId()).collect(Collectors.toList());
    }
}
