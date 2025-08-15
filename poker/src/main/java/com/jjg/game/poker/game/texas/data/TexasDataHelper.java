package com.jjg.game.poker.game.texas.data;

import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import com.jjg.game.sampledata.bean.TexasCfg;

/**
 * 处理配置相关的逻辑
 * @author lm
 * @date 2025/8/2 15:20
 */
public class TexasDataHelper extends PokerDataHelper {
    private TexasDataHelper() {
    }


    /**
     * 获取德州扑克默认带入金币
     */
    public static long getDefaultCoinsNum(TexasGameDataVo gameDataVo) {
        return getTexasCfg(gameDataVo).getCoinsNum();
    }

    /**
     * 获取德州扑克的pokerPool池id
     */
    public static int getPoolId(TexasGameDataVo gameDataVo) {
        return getTexasCfg(gameDataVo).getPokerPool();
    }

    /**
     * 获取德州扑克配置
     */
    public static TexasCfg getTexasCfg(TexasGameDataVo texasGameDataVo) {
        Room_ChessCfg roomCfg = texasGameDataVo.getRoomCfg();
        return GameDataManager.getTexasCfg(roomCfg.getId());
    }

    public static int getClientCardId(TexasGameDataVo gameDataVo, int cfgCardId) {
        return getCardListMap(getTexasCfg(gameDataVo).getPokerPool()).get(cfgCardId).getClientId();
    }
}
