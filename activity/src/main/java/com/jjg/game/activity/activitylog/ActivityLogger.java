package com.jjg.game.activity.activitylog;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.activity.activitylog.data.ScratchCardsResult;
import com.jjg.game.activity.activitylog.data.SharePromoteWeekRank;
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
        try {
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
        } catch (Exception e) {
            log.error("sendPrivilegeCardJoinLog error:", e);
        }
    }

    /**
     * 每日奖金道具领取日志记录
     */
    public void sendPrivilegeCardRewardsLog(Player player, ActivityData activityData, PrivilegeCardCfg cfg, long remain, ItemOperationResult result, Map<Integer, Long> rewards) {
        try {
            JSONObject json = buildBaseInfo(activityData, cfg.getId());
            json.put("operation", "rewards");
            json.put("subType", cfg.getType());
            json.put("remainingDays", remain);
            //奖励
            json.put("rewards", JSON.toJSONString(rewards));
            json.put("rewardsItemNum", JSON.toJSONString(result));
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendPrivilegeCardRewardsLog error:", e);
        }
    }

    /**
     * 摇钱树参加日志记录
     */
    public void sendCashCowJoinLog(Player player, ActivityData activityData, int detailId, int cashCowType,
                                   Map<Integer, Long> cost, ItemOperationResult costAfter,
                                   long get, ItemOperationResult rewardsAfter) {
        try {
            JSONObject json = buildBaseInfo(activityData, detailId);
            json.put("operation", "join");
            json.put("cashCowType", cashCowType);
            json.put("rewards", get);
            json.put("rewardsItemNum", JSON.toJSONString(rewardsAfter));
            json.put("cost", JSON.toJSONString(cost));
            json.put("costItemNum", JSON.toJSONString(costAfter));
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendCashCowJoinLog error:", e);
        }
    }

    /**
     * 摇钱树领取奖励日志记录
     */
    public void sendCashCowRewards(Player player, ActivityData activityData, int detailId,
                                   ItemOperationResult result,
                                   long progress, Map<Integer, Long> rewards) {
        try {
            JSONObject json = buildBaseInfo(activityData, detailId);
            json.put("operation", "rewards");
            json.put("rewards", JSON.toJSONString(rewards));
            json.put("rewardsItemNum", JSON.toJSONString(result));
            json.put("currentNum", progress);
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendCashCowRewards error", e);
        }
    }

    /**
     * 摇钱树领取免费道具记录
     */
    public void sendCashCowFreeRewards(Player player, ActivityData activityData, ItemOperationResult result, Item reward) {
        try {
            JSONObject json = buildBaseInfo(activityData, 0);
            json.put("operation", "freeRewards");
            json.put("rewards", JSON.toJSONString(Map.of(reward.getId(), reward.getItemCount())));
            json.put("rewardsItemNum", JSON.toJSONString(result));
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendCashCowFreeRewards error:", e);
        }
    }

    /**
     * 储钱罐参加活动记录
     */
    public void sendPiggyBankJoin(Player player, ActivityData activityData, PiggyBankData piggyBankData,
                                  int type, int detailId) {
        try {
            JSONObject json = buildBaseInfo(activityData, detailId);
            json.put("piggyBank", type);
            json.put("buyTime", piggyBankData.getBuyTime());
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendPiggyBankJoin error:", e);
        }
    }

    /**
     * 储钱罐领取道具记录
     */
    public void sendPiggyBankRewards(Player player, ActivityData activityData, PiggyBankData piggyBankData,
                                     int type, int detailId, ItemOperationResult result, Map<Integer, Long> rewards) {
        try {
            JSONObject json = buildBaseInfo(activityData, detailId);
            json.put("piggyBank", type);
            json.put("rewards", JSON.toJSONString(rewards));
            json.put("buyTime", piggyBankData.getBuyTime());
            json.put("fullTime", piggyBankData.getFullTime());
            json.put("rewardsItemNum", JSON.toJSONString(result));
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendPiggyBankRewards error:", e);
        }
    }


    /**
     * 刮刮乐参加活动记录
     */
    public void sendScratchCardsJoin(Player player, ActivityData activityData, Item totalCost,
                                     int times, ItemOperationResult costAfter, ItemOperationResult addResult, Map<Integer, Long> totalRewards,
                                     List<ScratchCardsResult> scratchCardsResults) {
        try {
            JSONObject json = buildBaseInfo(activityData, 0);
            json.put("operation", "join");
            json.put("rewards", JSON.toJSONString(totalRewards));
            json.put("rewardsItemNum", JSON.toJSONString(addResult));
            json.put("cost", JSON.toJSONString(Map.of(totalCost.getId(), totalCost.getItemCount())));
            json.put("costItemNum", JSON.toJSONString(costAfter));
            json.put("times", times);
            json.put("detail", JSON.toJSONString(scratchCardsResults));
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendScratchCardsJoin error", e);
        }
    }


    /**
     * 礼包获得日志
     */
    public void sendActivityGift(Player player, ActivityData activityData, ItemOperationResult result, Map<Integer, Long> rewards, int detailId) {
        try {
            JSONObject json = buildBaseInfo(activityData, detailId);
            json.put("rewards", JSON.toJSONString(rewards));
            json.put("operation", "gift");
            json.put("rewardsItemNum", JSON.toJSONString(result));
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendActivityGift error:", e);
        }
    }


    public JSONObject buildBaseInfo(ActivityData activityData, int detailId) {
        JSONObject json = new JSONObject();
        json.put("activityId", activityData.getId());
        json.put("type", activityData.getType().getType());
        json.put("detailId", detailId);
        json.put("round", activityData.getRound());
        return json;
    }

    /**
     * 分享推广充值下级日志
     *
     * @param player         玩家信息
     * @param activityData   活动数据
     * @param rechargeAmount 充值金额
     * @param totalAdd       增加的金币
     */
    public void sendSharePromoteSubordinateRecharge(Player player, ActivityData activityData, long rechargeAmount, long totalAdd) {
        try {
            JSONObject json = buildBaseInfo(activityData, 0);
            json.put("rechargeAmount", rechargeAmount);
            json.put("totalAdd", totalAdd);
            json.put("logType", 6);
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendSharePromoteSubordinateRecharge error:", e);
        }
    }


    /**
     * 分享推广奖励增加日志
     *
     * @param player       玩家数据
     * @param activityData 活动数据
     * @param logType      类型 (1.下级充值 2绑定玩家 3.分享收益领取 4.人数收益领取 5.人数变化 6.绑定下级充值 7.周榜)
     * @param totalGoldAdd 总收益增加
     * @param addBindNum   绑定人数增加
     * @param addGold      领取金币增加
     * @param sharingRatio 当前分享比例
     * @param remainGold   账户余数
     * @param superiorId   上级id
     */
    public void sendSharePromoteAddRewards(Player player, ActivityData activityData, int logType, long totalGoldAdd, int addBindNum
            , long addGold, int sharingRatio, long remainGold, long superiorId, int bindType) {
        try {
            JSONObject json = buildBaseInfo(activityData, 0);
            json.put("totalAdd", totalGoldAdd);
            json.put("addGold", addGold);
            json.put("addBindNum", addBindNum);
            json.put("sharingRatio", sharingRatio);
            json.put("logType", logType);
            if (remainGold > 0) {
                json.put("remainGold", remainGold);
            }
            if (superiorId > 0) {
                json.put("superiorId", superiorId);
            }
            if(bindType > 0) {
                json.put("bindType", bindType);
            }
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendSharePromoteAddRewards: ", e);
        }
    }

    /**
     * 分享推广奖励增加日志
     *
     * @param player       玩家数据
     * @param activityData 活动数据
     * @param type         类型 (1.下级充值 2绑定玩家 3.分享收益领取 4.人数收益领取 5.人数变化 6.绑定下级充值 7.周榜)
     * @param totalGoldAdd 总收益增加
     * @param addBindNum   绑定人数增加
     * @param addGold      领取金币增加
     * @param sharingRatio 当前分享比例
     */
    public void sendSharePromoteAddRewards(Player player, ActivityData activityData, int type, long totalGoldAdd, int addBindNum
            , long addGold, int sharingRatio) {
        sendSharePromoteAddRewards(player, activityData, type, totalGoldAdd, addBindNum, addGold, sharingRatio, 0, 0,0);
    }

    /**
     * 推广分享周榜日志
     *
     * @param activityData 活动数据
     * @param logList      排行榜日志
     */
    public void sendSharePromoteRankRewards(ActivityData activityData, List<SharePromoteWeekRank> logList) {
        try {
            JSONObject json = buildBaseInfo(activityData, 0);
            json.put("rankInfos", JSON.toJSONString(logList));
            json.put("logType", 7);
            sendLog(TOPIC, null, json);
        } catch (Exception e) {
            log.error("sendSharePromoteRankRewards error:", e);
        }
    }
}
