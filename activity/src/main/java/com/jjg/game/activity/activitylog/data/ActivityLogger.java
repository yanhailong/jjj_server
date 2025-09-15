package com.jjg.game.activity.activitylog.data;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.piggybank.data.PiggyBankData;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.core.utils.ItemUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 活动日志
 *
 * @author lm
 * @date 2025/9/15 11:38
 */
@Component
public class ActivityLogger extends BaseLogger {

    private final String TOPIC = "Activity";

    /**
     * 每日奖金道具参加获得日志记录
     */
    public void sendPrivilegeCardJoinLog(Player player, ActivityData activityData, int detailId, long itemOperationId, Map<Integer, Long> rewards) {
        JSONObject json = buildBaseInfo(activityData, detailId, itemOperationId);
        json.put("operation", "join");
        //奖励
        json.put("rewards", JSON.toJSONString(rewards));
        sendLog(TOPIC, player, json);
    }

    /**
     * 每日奖金道具领取日志记录
     */
    public void sendPrivilegeCardRewardsLog(Player player, ActivityData activityData, int detailId, long itemOperationId, Map<Integer, Long> rewards) {
        JSONObject json = buildBaseInfo(activityData, detailId, itemOperationId);
        json.put("operation", "rewards");
        //奖励
        json.put("rewards", JSON.toJSONString(rewards));
        sendLog(TOPIC, player, json);
    }

    /**
     * 摇钱树参加日志记录
     */
    public void sendCashCowJoinLog(Player player, ActivityData activityData, int detailId, int cashCowType,
                                   Map<Integer, Long> cost, long itemOperationId, long removeId,
                                   long rewards) {
        JSONObject json = buildBaseInfo(activityData, detailId, itemOperationId);
        json.put("operation", "join");
        json.put("removeOperationId", removeId);
        json.put("cashCowType", cashCowType);
        json.put("rewards", rewards);
        json.put("cost", JSON.toJSONString(cost));
        sendLog(TOPIC, player, json);
    }

    /**
     * 摇钱树领取奖励日志记录
     */
    public void sendCashCowRewards(Player player, ActivityData activityData, int detailId, long itemOperationId, long progress, Map<Integer, Long> rewards) {
        JSONObject json = buildBaseInfo(activityData, detailId, itemOperationId);
        json.put("operation", "rewards");
        json.put("rewards", JSON.toJSONString(rewards));
        json.put("currentNum", progress);
        sendLog(TOPIC, player, json);
    }

    /**
     * 摇钱树领取免费道具记录
     */
    public void sendCashCowFreeRewards(Player player, long activityDataId, long itemOperationId, Item reward) {
        JSONObject json = new JSONObject();
        json.put("activityId", activityDataId);
        json.put("operationId", itemOperationId);
        json.put("operation", "freeRewards");
        json.put("rewards", JSON.toJSONString(Map.of(reward.getId(), reward.getItemCount())));
        sendLog(TOPIC, player, json);
    }

    /**
     * 储钱罐参加活动记录
     */
    public void sendPiggyBankJoin(Player player, ActivityData activityData, PiggyBankData piggyBankData,
                                  int type, int detailId) {
        JSONObject json = buildBaseInfo(activityData, detailId, 0);
        json.put("piggyBank", type);
        json.put("buyTime", piggyBankData.getBuyTime());
        sendLog(TOPIC, player, json);
    }

    /**
     * 储钱罐领取道具记录
     */
    public void sendPiggyBankRewards(Player player, ActivityData activityData, PiggyBankData piggyBankData,
                                     int type, int detailId, long itemOperationId, Map<Integer, Long> rewards) {
        JSONObject json = buildBaseInfo(activityData, detailId, itemOperationId);
        json.put("piggyBank", type);
        json.put("rewards", JSON.toJSONString(rewards));
        json.put("buyTime", piggyBankData.getBuyTime());
        json.put("fullTime", piggyBankData.getFullTime());
        if (rewards.containsKey(ItemUtils.getGoldItemId())) {
            json.put("currentNum", player.getGold());
        } else {
            json.put("currentNum", player.getDiamond());
        }
        sendLog(TOPIC, player, json);
    }


    /**
     * 刮刮乐参加活动记录
     */
    public void sendScratchCardsJoin(Player player, ActivityData activityData, Item totalCost,
                                     int times, long removeOperationId, long itemOperationId, Map<Integer, Long> totalRewards,
                                     List<ScratchCardsResult> scratchCardsResults) {
        JSONObject json = buildBaseInfo(activityData, 0, itemOperationId);
        json.put("operation", "join");
        json.put("rewards", JSON.toJSONString(totalRewards));
        json.put("cost", JSON.toJSONString(Map.of(totalCost.getId(), totalCost.getItemCount())));
        json.put("removeOperationId", removeOperationId);
        json.put("times", times);
        JSONArray detail = new JSONArray();
        for (ScratchCardsResult result : scratchCardsResults) {
            detail.add(JSON.toJSONString(result));
        }
        json.put("detail", JSON.toJSONString(detail));
        if (totalRewards.containsKey(ItemUtils.getGoldItemId())) {
            json.put("currentNum", player.getGold());
        } else {
            json.put("currentNum", player.getDiamond());
        }
        sendLog(TOPIC, player, json);
    }


    /**
     * 礼包获得日志
     */
    public void sendActivityGift(Player player, ActivityData activityData, long itemOperationId, Map<Integer, Long> rewards, int detailId) {
        JSONObject json = buildBaseInfo(activityData, detailId, itemOperationId);
        json.put("rewards", JSON.toJSONString(rewards));
        json.put("operation", "gift");
        if (rewards.containsKey(ItemUtils.getGoldItemId())) {
            json.put("currentNum", player.getGold());
        } else {
            json.put("currentNum", player.getDiamond());
        }
        sendLog(TOPIC, player, json);
    }


    public JSONObject buildBaseInfo(ActivityData activityData, int detailId, long itemOperationId) {
        JSONObject json = new JSONObject();
        json.put("activityId", activityData.getId());
        json.put("type", activityData.getType().getType());
        json.put("detailId", detailId);
        json.put("round", activityData.getRound());
        json.put("operationId", itemOperationId);
        return json;
    }
}
