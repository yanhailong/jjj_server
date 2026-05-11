package com.jjg.game.ploy.logger;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.ploy.data.PlayerPloyGameData;
import com.jjg.game.ploy.games.airraid.data.AirRaidPlayerPloyGameData;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowPokerPloyGameData;
import com.jjg.game.ploy.games.luckypoker.data.LuckyPokerPlayerPloyGameData;
import com.jjg.game.ploy.games.luckypoker.utils.LuckyPokerUtils;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/3/19
 */
@Component
public class PloyLogger extends BaseLogger {
    /**
     * 通用信息
     *
     * @param gameData
     * @return
     */
    private JSONObject buildBaseInfo(PlayerPloyGameData gameData) {
        JSONObject json = new JSONObject();
        json.put("bet", gameData.getLastBet());
        json.put("betTime", gameData.getLastBetTime());
        if (gameData.getPloyBetDivideInfo() != null) {
            json.put("beforeMoney", gameData.getPloyBetDivideInfo().getPlayerBeforeMoney());
            json.put("afterMoney", gameData.getPloyBetDivideInfo().getPlayerAfterMoney());
        }
        json.put("win", gameData.getWin());
        return json;
    }

    /**
     * 鸿运扑克
     */
    public void luckpoker(Player player, LuckyPokerPlayerPloyGameData gameData) {
        try {
            JSONObject json = buildBaseInfo(gameData);
            json.put("firstCards", LuckyPokerUtils.card2Ids(gameData.getFirstCardList()));
            json.put("finalCards", LuckyPokerUtils.card2Ids(gameData.getFinalCardList()));
            json.put("winTimes", gameData.getWinTimes());
            sendLog("ploygame", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }


    /**
     * 高低扑克
     */
    public void highLowPoker(Player player, HighLowPokerPloyGameData gameData) {
        try {
            JSONObject json = buildBaseInfo(gameData);
            json.put("winTimes", gameData.getWinTimes());
            sendLog("ploygame", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 空袭
     */
    public void airRaid(Player player, AirRaidPlayerPloyGameData gameData,
                        long betAmount, int crashMultiplier, int cashOutMultiplier,
                        long winAmount, boolean cashedOut) {
        try {
            JSONObject json = buildBaseInfo(gameData);
            json.put("betAmount", betAmount);
            json.put("crashMultiplier", crashMultiplier);
            json.put("cashOutMultiplier", cashOutMultiplier);
            json.put("winAmount", winAmount);
            json.put("cashedOut", cashedOut);
            sendLog("ploygame", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
