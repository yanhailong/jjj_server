package com.jjg.game.poker.game.texas.data;

import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ChessTexasStrategyCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import com.jjg.game.sampledata.bean.TexasCfg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处理配置相关的逻辑
 *
 * @author lm
 * @date 2025/8/2 15:20
 */
public class TexasDataHelper extends PokerDataHelper {
    private TexasDataHelper() {
    }

    //牌型id->牌值->配置表
    private static Map<Integer, Map<Integer, ChessTexasStrategyCfg>> robotActionMap;

    /**
     * 初始化机器人策略
     */
    public static void intiData() {
        Map<Integer, Map<Integer, ChessTexasStrategyCfg>> tempMap = new HashMap<>();
        List<ChessTexasStrategyCfg> chessTexasStrategyCfgList = GameDataManager.getChessTexasStrategyCfgList();
        for (ChessTexasStrategyCfg cfg : chessTexasStrategyCfgList) {
            Map<Integer, ChessTexasStrategyCfg> cfgMap = tempMap.computeIfAbsent(cfg.getType(), k -> new HashMap<>());
            cfgMap.put(cfg.getValue(), cfg);
        }
        robotActionMap = tempMap;
    }

    public static ChessTexasStrategyCfg getRobotActionCfg(int cardType, int cardValue) {
        Map<Integer, ChessTexasStrategyCfg> cfgMap = robotActionMap.get(cardType);
        if (cfgMap == null) {
            return null;
        }
        return cfgMap.get(cardValue);
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
