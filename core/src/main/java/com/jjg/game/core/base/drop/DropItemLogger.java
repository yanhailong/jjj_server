package com.jjg.game.core.base.drop;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import org.springframework.stereotype.Component;

/**
 * 道具掉落日志logger
 *
 * @author 2CL
 */
@Component
public class DropItemLogger extends BaseLogger {

    // 道具掉落topic
    private static final String DROP_ITEM_TOPIC = "drop_item";

    /**
     * 记录道具掉落日志
     *
     * @param player              player
     * @param activityId          活动ID
     * @param itemOperationResult 道具操作记录
     */
    public void recordDropItem(Player player, long activityId, int gameCfgId, ItemOperationResult itemOperationResult) {
        JSONObject data = new JSONObject();
        data.put("activityId", activityId);
        data.put("gameCfgId", gameCfgId);
        data.put("itemChangeBefore", itemOperationResult.getChangeBeforeItemNum());
        data.put("itemChangeAfter", itemOperationResult.getChangeEndItemNum());
        sendLog(DROP_ITEM_TOPIC, player, data);
    }
}
