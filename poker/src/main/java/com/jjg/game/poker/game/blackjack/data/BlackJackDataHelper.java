package com.jjg.game.poker.game.blackjack.data;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BlackjackCfg;
import com.jjg.game.sampledata.bean.ChessJackStrategyCfg;
import com.jjg.game.sampledata.bean.PokerPoolCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jjg.game.poker.game.blackjack.constant.BlackJackConstant.Common.PERFECT_POINT;

/**
 * @author lm
 * @date 2025/8/6 10:19
 */
public class BlackJackDataHelper extends PokerDataHelper {
    private BlackJackDataHelper() {
    }
    //牌型是否有A->牌值->配置表
    private static Map<Boolean, Map<Integer, ChessJackStrategyCfg>> robotActionMap;
    /**
     * 初始化机器人策略
     */
    public static void initData() {
        Map<Boolean, Map<Integer, ChessJackStrategyCfg>> tempMap = new HashMap<>();
        List<ChessJackStrategyCfg> chessTexasStrategyCfgList = GameDataManager.getChessJackStrategyCfgList();
        for (ChessJackStrategyCfg cfg : chessTexasStrategyCfgList) {
            Map<Integer, ChessJackStrategyCfg> cfgMap = tempMap.computeIfAbsent(cfg.getType() == 22, k -> new HashMap<>());
            cfgMap.put(cfg.getValue(), cfg);
        }
        robotActionMap = tempMap;
    }

    public static Map<Boolean, Map<Integer, ChessJackStrategyCfg>> getRobotActionMap() {
        return robotActionMap;
    }

    public static BlackjackCfg getBlackjackCfg(BlackJackGameDataVo gameDataVo) {
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        return GameDataManager.getBlackjackCfg(roomCfg.getId());
    }

    public static int getPoolId(BlackJackGameDataVo gameDataVo) {
        return getBlackjackCfg(gameDataVo).getPoolId();
    }

    public static int getClientCardId(BlackJackGameDataVo gameDataVo, int cfgCardId) {
        return getCardListMap(getBlackjackCfg(gameDataVo).getPoolId()).get(cfgCardId).getClientId();
    }

    public static long getGetWinValue(long betValue, long param) {
        return betValue * (100 + param) / 100;
    }

    public static int getTotalPoint(List<Integer> cfgCardId) {
        Pair<Boolean, Integer> result = getTotalPointInfo(cfgCardId);
        return result.getFirst() && (result.getSecond() + 10 <= PERFECT_POINT) ? result.getSecond() + 10 : result.getSecond();
    }

    public static Pair<Boolean, Integer> getTotalPointInfo(List<Integer> cfgCardId) {
        int totalPoint = 0;
        boolean hasA = false;
        for (Integer cfgId : cfgCardId) {
            PokerPoolCfg pokerPoolCfg = GameDataManager.getPokerPoolCfg(cfgId);
            totalPoint += pokerPoolCfg.getPointsNum();
            if (!hasA && pokerPoolCfg.getPointsNum() == 1) {
                hasA = true;
            }
        }
        return Pair.newPair(hasA, totalPoint);
    }
    public static List<Integer> getShowTotalPoint(List<Integer> cfgCardId) {
        Pair<Boolean, Integer> totalPointInfo = getTotalPointInfo(cfgCardId);
        boolean hasA = totalPointInfo.getFirst();
        int totalPoint = totalPointInfo.getSecond();
        if (hasA) {
            return (totalPoint + 10 < PERFECT_POINT) ? List.of(totalPoint, totalPoint + 10) : List.of(totalPoint + 10 == PERFECT_POINT ? totalPoint + 10 : totalPoint);
        } else {
            return List.of(totalPoint);
        }
    }

    public static int getCfgPoint(int cfgCardId) {
        return GameDataManager.getPokerPoolCfg(cfgCardId).getPointsNum();
    }
}
