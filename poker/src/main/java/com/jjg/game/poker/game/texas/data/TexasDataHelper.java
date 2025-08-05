package com.jjg.game.poker.game.texas.data;

import com.jjg.game.core.data.Card;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.sample.GameDataManager;
import com.jjg.game.poker.game.sample.bean.PokerPoolCfg;
import com.jjg.game.poker.game.sample.bean.TexasCfg;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.sample.bean.Room_ChessCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/8/2 15:20
 */
public class TexasDataHelper {
    private final static Logger log = LoggerFactory.getLogger(TexasDataHelper.class);
    //房间配置id->pokerPoolId->牌
    private static Map<Integer, Map<Integer, PokerCard>> cardMapListMap;

    /**
     * 初始化缓存
     */
    public static void initData() {
        log.info("开始加载德州游戏配置..");
        Map<Integer, Map<Integer, PokerCard>> map = new HashMap<>();
        for (TexasCfg texasCfg : GameDataManager.getTexasCfgList()) {
            List<PokerPoolCfg> cfgList = GameDataManager.getPokerPoolCfgList();
            Map<Integer, PokerCard> collect = cfgList.stream().filter(pokerPoolCfg -> texasCfg.getPokerPool() == pokerPoolCfg.getPoolId())
                    .collect(Collectors.toMap(PokerPoolCfg::getId, cfg -> {
                        PokerCardUtils.EPokerHumanStr pokerHumanStrByHumanStr = PokerCardUtils.EPokerHumanStr.getPokerHumanStrByHumanStr(cfg.getPoints());
                        PokerCardUtils.EPokerSuit suitByConfig = PokerCardUtils.getSuitByConfig(cfg.getSuit());
                        if (Objects.nonNull(suitByConfig) && Objects.nonNull(pokerHumanStrByHumanStr)) {
                            Card card = new Card(suitByConfig.getSuitId(), pokerHumanStrByHumanStr.getPointId());
                            return new PokerCard(cfg.getId(), cfg.getSuitNum(), cfg.getPointsNum(), card.getValue());
                        } else {
                            throw new RuntimeException("配置错误");
                        }
                    }));
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
        return roomCfg.getChess_stageOrder().getOrDefault(phase.getValue(), 0);
    }

    public static List<Integer> getClientId(List<Integer> cardCfgId, int cfgId) {
        Map<Integer, PokerCard> cardMap = getCardListMap(cfgId);
        return cardCfgId.stream().map(id -> cardMap.get(id).getClientId()).collect(Collectors.toList());
    }
}
