package com.jjg.game.activity.activitylog;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.activity.activitylog.data.ScratchCardsResult;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.piggybank.data.PiggyBankData;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.sampledata.bean.PrivilegeCardCfg;
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

    private final String TOPIC = "activity";

    /**
     * 每日奖金道具参加获得日志记录
     */
    public void sendPrivilegeCardJoinLog(Player player, ActivityData activityData, PrivilegeCardCfg cfg, ItemOperationResult result, Map<Integer, Long> rewards) {
        JSONObject json = buildBaseInfo(activityData, cfg.getId());
        json.put("operation", "join");
        json.put("subType", cfg.getType());
        json.put("price", cfg.getPurchasecost());
        //奖励
        json.put("rewards", JSON.toJSONString(rewards));
        if (result != null) {
            json.put("rewardsItemNum", JSON.toJSONString(result));
        }
        sendLog(TOPIC, player, json);
    }

    /**
     * 每日奖金道具领取日志记录
     */
    public void sendPrivilegeCardRewardsLog(Player player, ActivityData activityData, PrivilegeCardCfg cfg, long remain, ItemOperationResult result, Map<Integer, Long> rewards) {
        JSONObject json = buildBaseInfo(activityData, cfg.getId());
        json.put("operation", "rewards");
        json.put("subType", cfg.getType());
        json.put("remainingDays", remain);
        //奖励
        json.put("rewards", JSON.toJSONString(rewards));
        json.put("rewardsItemNum", JSON.toJSONString(result));
        sendLog(TOPIC, player, json);
    }

    /**
     * 摇钱树参加日志记录
     */
    public void sendCashCowJoinLog(Player player, ActivityData activityData, int detailId, int cashCowType,
                                   Map<Integer, Long> cost, ItemOperationResult costAfter,
                                   long get, ItemOperationResult rewardsAfter) {
        JSONObject json = buildBaseInfo(activityData, detailId);
        json.put("operation", "join");
        json.put("cashCowType", cashCowType);
        json.put("rewards", get);
        json.put("rewardsItemNum", JSON.toJSONString(rewardsAfter));
        json.put("cost", JSON.toJSONString(cost));
        json.put("costItemNum", JSON.toJSONString(costAfter));
        sendLog(TOPIC, player, json);
    }

    /**
     * 摇钱树领取奖励日志记录
     */
    public void sendCashCowRewards(Player player, ActivityData activityData, int detailId,
                                   ItemOperationResult result,
                                   long progress, Map<Integer, Long> rewards) {
        JSONObject json = buildBaseInfo(activityData, detailId);
        json.put("operation", "rewards");
        json.put("rewards", JSON.toJSONString(rewards));
        json.put("rewardsItemNum", JSON.toJSONString(result));
        json.put("currentNum", progress);
        sendLog(TOPIC, player, json);
    }

    /**
     * 摇钱树领取免费道具记录
     */
    public void sendCashCowFreeRewards(Player player, ActivityData activityData, ItemOperationResult result, Item reward) {
        JSONObject json = buildBaseInfo(activityData, 0);
        json.put("operation", "freeRewards");
        json.put("rewards", JSON.toJSONString(Map.of(reward.getId(), reward.getItemCount())));
        json.put("rewardsItemNum", JSON.toJSONString(result));
        sendLog(TOPIC, player, json);
    }

    /**
     * 储钱罐参加活动记录
     */
    public void sendPiggyBankJoin(Player player, ActivityData activityData, PiggyBankData piggyBankData,
                                  int type, int detailId) {
        JSONObject json = buildBaseInfo(activityData, detailId);
        json.put("piggyBank", type);
        json.put("buyTime", piggyBankData.getBuyTime());
        sendLog(TOPIC, player, json);
    }

    /**
     * 储钱罐领取道具记录
     */
    public void sendPiggyBankRewards(Player player, ActivityData activityData, PiggyBankData piggyBankData,
                                     int type, int detailId, ItemOperationResult result, Map<Integer, Long> rewards) {
        JSONObject json = buildBaseInfo(activityData, detailId);
        json.put("piggyBank", type);
        json.put("rewards", JSON.toJSONString(rewards));
        json.put("buyTime", piggyBankData.getBuyTime());
        json.put("fullTime", piggyBankData.getFullTime());
        json.put("rewardsItemNum", JSON.toJSONString(result));
        sendLog(TOPIC, player, json);
    }


    /**
     * 刮刮乐参加活动记录
     */
    public void sendScratchCardsJoin(Player player, ActivityData activityData, Item totalCost,
                                     int times, ItemOperationResult costAfter, ItemOperationResult addResult, Map<Integer, Long> totalRewards,
                                     List<ScratchCardsResult> scratchCardsResults) {
        JSONObject json = buildBaseInfo(activityData, 0);
        json.put("operation", "join");
        json.put("rewards", JSON.toJSONString(totalRewards));
        json.put("rewardsItemNum", JSON.toJSONString(addResult));
        json.put("cost", JSON.toJSONString(Map.of(totalCost.getId(), totalCost.getItemCount())));
        json.put("costItemNum", JSON.toJSONString(costAfter));
        json.put("times", times);
        json.put("detail", JSON.toJSONString(scratchCardsResults));
        sendLog(TOPIC, player, json);
    }


    /**
     * 礼包获得日志
     */
    public void sendActivityGift(Player player, ActivityData activityData, ItemOperationResult result, Map<Integer, Long> rewards, int detailId) {
        JSONObject json = buildBaseInfo(activityData, detailId);
        json.put("rewards", JSON.toJSONString(rewards));
        json.put("operation", "gift");
        json.put("rewardsItemNum", JSON.toJSONString(result));
        sendLog(TOPIC, player, json);
    }


    public JSONObject buildBaseInfo(ActivityData activityData, int detailId) {
        JSONObject json = new JSONObject();
        json.put("activityId", activityData.getId());
        json.put("type", activityData.getType().getType());
        json.put("detailId", detailId);
        json.put("round", activityData.getRound());
        return json;
    }
}
