package com.jjg.game.hall.logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.LuckyTreasure;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.hall.minigame.constant.MinigameConstant;
import org.springframework.stereotype.Component;

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
            result.put("logTime", System.currentTimeMillis());
            result.put("log", jsonObject);
            sendLog("minigameLog", null, result);
        } catch (Exception e) {
            log.error("幸运夺宝开奖日志记录失败!", e);
        }

    }
}
