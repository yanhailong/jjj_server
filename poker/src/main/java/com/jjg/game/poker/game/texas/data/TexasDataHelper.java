package com.jjg.game.poker.game.texas.data;

import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.sample.GameDataManager;
import com.jjg.game.poker.game.sample.bean.TexasCfg;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.sample.bean.Room_ChessCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lm
 * @date 2025/8/2 15:20
 */
public class TexasDataHelper extends PokerDataHelper {
    private final static Logger log = LoggerFactory.getLogger(TexasDataHelper.class);

    /**
     * 初始化缓存
     */
    public static void initData() {
        log.info("开始加载德州游戏配置..");
        Map<Integer, Map<Integer, PokerCard>> map = new HashMap<>();
        for (TexasCfg texasCfg : GameDataManager.getTexasCfgList()) {
            map.put(texasCfg.getId(), initCardMapListMap(texasCfg.getPokerPool()));
        }
        setCardMapListMap(map);
    }

    public static long getDefaultCoinsNum(TexasGameDataVo gameDataVo) {
        return getTexasCfg(gameDataVo).getCoinsNum();
    }

    public static TexasCfg getTexasCfg(TexasGameDataVo texasGameDataVo) {
        Room_ChessCfg roomCfg = texasGameDataVo.getRoomCfg();
        return GameDataManager.getTexasCfg(roomCfg.getId());
    }
}
