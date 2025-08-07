package com.jjg.game.poker.game.texas.data;

import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.sample.GameDataManager;
import com.jjg.game.poker.game.sample.bean.TexasCfg;
import com.jjg.game.poker.game.texas.message.bean.TexasHistory;
import com.jjg.game.poker.game.texas.message.bean.TexasHistoryRoundInfo;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

/**
 * @author lm
 * @date 2025/8/2 15:20
 */
public class TexasDataHelper extends PokerDataHelper {
    private TexasDataHelper(){}


    public static long getDefaultCoinsNum(TexasGameDataVo gameDataVo) {
        return getTexasCfg(gameDataVo).getCoinsNum();
    }

    public static int getPoolId(TexasGameDataVo gameDataVo) {
        return getTexasCfg(gameDataVo).getPokerPool();
    }

    public static TexasCfg getTexasCfg(TexasGameDataVo texasGameDataVo) {
        Room_ChessCfg roomCfg = texasGameDataVo.getRoomCfg();
        return GameDataManager.getTexasCfg(roomCfg.getId());
    }

    public static TexasHistoryRoundInfo getHistoryRoundInfo(TexasGameDataVo texasGameDataVo) {
        TexasSaveHistory texasHistory = texasGameDataVo.getTexasHistory();
        return texasHistory.getTexasHistoryRoundInfos().get(texasGameDataVo.getRound()-1);
    }
}
