package com.jjg.game.dollarexpress.logger;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.dollarexpress.data.GameRunInfo;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/10 18:08
 */
@Component
public class DollarExpressLogger extends BaseLogger {
    /**
     * 游戏开奖结果
     * @param player
     */
    public void gameResult(Player player, GameRunInfo gameRunInfo){
        try{
            JSONObject json = new JSONObject();
            json.put("logType","dollarExpressResult");
            json.put("allWin",9999);
            json.put("afterGold",player.getGold());

            sendLog(player,json);
        }catch (Exception e){
            log.error("",e);
        }
    }
}
