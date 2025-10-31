package com.jjg.game.core.base.drop;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 道具掉落日志logger
 *
 * @author 2CL
 */
@Component
public class DropItemLogger extends BaseLogger {

    // 道具掉落topic
    private final String DROP_ITEM_TOPIC = "drop_item";

    /**
     * 记录道具掉落日志
     *
     * @param player player
     * @param result 道具操作记录
     */
    public void recordDropItem(Player player, AddType addType, String desc, int gameCfgId, Map<Integer, Long> itemList,
                               ItemOperationResult result) {
        try {
            JSONObject data = new JSONObject();
            data.put("addType", addType.getValue());
            data.put("desc", desc);
            data.put("gameCfgId", gameCfgId);
            data.put("itemList", JSONObject.toJSONString(itemList, SerializerFeature.WriteNonStringKeyAsString));
            data.put("itemChangeBefore", JSONObject.toJSONString(result.getChangeBeforeItemNum(), SerializerFeature.WriteNonStringKeyAsString));
            data.put("itemChangeAfter", JSONObject.toJSONString(result.getChangeEndItemNum(), SerializerFeature.WriteNonStringKeyAsString));
            sendLog(DROP_ITEM_TOPIC, player, data);
        } catch (Exception e) {
            log.error("recordDropItem", e);
        }
    }


}
