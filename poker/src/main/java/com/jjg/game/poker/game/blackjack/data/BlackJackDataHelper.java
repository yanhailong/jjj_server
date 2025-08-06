package com.jjg.game.poker.game.blackjack.data;

import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.sample.GameDataManager;
import com.jjg.game.poker.game.sample.bean.BlackjackCfg;
import com.jjg.game.poker.game.sample.bean.PokerPoolCfg;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

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


    public static BlackjackCfg getBlackjackCfg(BlackJackGameDataVo gameDataVo) {
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        return GameDataManager.getBlackjackCfg(roomCfg.getId());
    }

    public static int getPoolId(BlackJackGameDataVo gameDataVo) {
        return getBlackjackCfg(gameDataVo).getPoolId();
    }

    public static int getClientCardId(BlackJackGameDataVo gameDataVo,int cfgCardId) {
        return getCardListMap(getBlackjackCfg(gameDataVo).getPoolId()).get(cfgCardId).getClientId();
    }

    public static int getTotalPoint(List<Integer> cfgCardId) {
        int totalPoint = 0;
        int maxTotalPoint = 0;
        for (Integer cfgId : cfgCardId) {
            PokerPoolCfg pokerPoolCfg = GameDataManager.getPokerPoolCfg(cfgId);
            totalPoint += pokerPoolCfg.getPointsNum();
            if (pokerPoolCfg.getPointsNum() == 1) {
                maxTotalPoint += pokerPoolCfg.getPointsNum() + 10;
            } else {
                maxTotalPoint += pokerPoolCfg.getPointsNum();
            }
        }
        return maxTotalPoint > PERFECT_POINT ? totalPoint : Math.max(totalPoint, maxTotalPoint);
    }
}
