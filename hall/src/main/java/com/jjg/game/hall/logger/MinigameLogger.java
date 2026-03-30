package com.jjg.game.hall.logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.LuckyTreasure;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.hall.minigame.constant.MinigameConstant;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 小游戏日志记录
 */
@Component
public class MinigameLogger extends BaseLogger {

    /**
     * 记录开奖日志
     *
     * @param luckyTreasure 夺宝所有数据
     */
    public void finish(LuckyTreasure luckyTreasure) {
        try {
            String json = JSON.toJSONString(luckyTreasure);
            JSONObject jsonObject = JSON.parseObject(json);
            JSONObject result = new JSONObject();
            result.put("gameId", MinigameConstant.GameId.LUCKY_TREASURE);
            result.put("log", jsonObject);
            sendLog("minigameLog", null, result);
        } catch (Exception e) {
            log.error("小游戏日志记录失败!", e);
        }

    }

    /**
     * 幸运夺宝的购买日志
     *
     * @param player
     * @param luckyTreasure
     * @param consumeMap
     * @param buyCount
     * @param remainCount
     */
    public void luckyTreasureBuy(Player player, LuckyTreasure luckyTreasure, Map<Integer, Long> consumeMap, int buyCount, int remainCount) {
        try {
            JSONObject result = new JSONObject();
            //期数
            result.put("issueNumber", luckyTreasure.getIssueNumber());
            //配置id
            result.put("cfgId", luckyTreasure.getConfig().getId());
            //配置名称
            result.put("cfgName", luckyTreasure.getConfig().getName());
            //消耗的道具
            result.put("consume", ItemUtils.itemMapToJsonArray(consumeMap));
            //购买份数
            result.put("buyCount", buyCount);
            //剩余份数
            result.put("remainCount", remainCount);
            sendLog("luckyTreasureBuy", player, result);
        } catch (Exception e) {
            log.error("幸运夺宝的购买日志记录失败!", e);
        }
    }
}
