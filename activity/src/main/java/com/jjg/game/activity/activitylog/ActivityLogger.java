package com.jjg.game.activity.activitylog;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.activity.activitylog.data.ScratchCardsResult;
import com.jjg.game.activity.activitylog.data.SharePromoteWeekRank;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.piggybank.data.PiggyBankData;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.sampledata.bean.FirstpaymentCfg;
import com.jjg.game.sampledata.bean.PlayerLevelPackCfg;
import com.jjg.game.sampledata.bean.PrivilegeCardCfg;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            json.put("rewards", objectMapper.writeValueAsString(rewards));
            if (result != null) {
                json.put("rewardsItemNum", objectMapper.writeValueAsString(result));
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
            json.put("rewards", objectMapper.writeValueAsString(rewards));
            json.put("rewardsItemNum", objectMapper.writeValueAsString(result));
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
            json.put("rewardsItemNum", objectMapper.writeValueAsString(rewardsAfter));
            json.put("cost", objectMapper.writeValueAsString(cost));
            json.put("costItemNum", objectMapper.writeValueAsString(costAfter));
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
            json.put("rewards", objectMapper.writeValueAsString(rewards));
            json.put("rewardsItemNum", objectMapper.writeValueAsString(result));
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
            json.put("rewards", objectMapper.writeValueAsString((Map.of(reward.getId(), reward.getItemCount()))));
            json.put("rewardsItemNum", objectMapper.writeValueAsString(result));
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
            json.put("rewards", objectMapper.writeValueAsString(rewards));
            json.put("buyTime", piggyBankData.getBuyTime());
            json.put("fullTime", piggyBankData.getFullTime());
            json.put("rewardsItemNum", objectMapper.writeValueAsString(result));
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
            json.put("rewards", objectMapper.writeValueAsString(totalRewards));
            json.put("rewardsItemNum", objectMapper.writeValueAsString(addResult));
            json.put("cost", objectMapper.writeValueAsString(Map.of(totalCost.getId(), totalCost.getItemCount())));
            json.put("costItemNum", objectMapper.writeValueAsString(costAfter));
            json.put("times", times);
            json.put("detail", objectMapper.writeValueAsString(scratchCardsResults));
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendScratchCardsJoin error", e);
        }
    }


    /**
     * 礼包获得日志
     */
    public void sendActivityGift(Player player, ActivityData activityData, ItemOperationResult result, Map<Integer, Long> rewards, BigDecimal cost, int detailId) {
        try {
            JSONObject json = buildBaseInfo(activityData, detailId);
            json.put("rewards", objectMapper.writeValueAsString(rewards));
            json.put("cost", cost.toPlainString());
            json.put("operation", "gift");
            json.put("rewardsItemNum", objectMapper.writeValueAsString(result));
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
    public void sendSharePromoteSubordinateRecharge(Player player, ActivityData activityData, long superiorId, long rechargeAmount, long totalAdd) {
        try {
            JSONObject json = buildBaseInfo(activityData, 0);
            json.put("rechargeAmount", rechargeAmount);
            json.put("superiorId", superiorId);
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
     */
    public void sendSharePromoteAddRewards(Player player, ActivityData activityData, int logType, long totalGoldAdd, int addBindNum
            , long addGold, int sharingRatio, long remainGold, int bindType) {
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
            if (bindType > 0) {
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
        sendSharePromoteAddRewards(player, activityData, type, totalGoldAdd, addBindNum, addGold, sharingRatio, 0, 0);
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
            json.put("rankInfos", objectMapper.writeValueAsString(logList));
            json.put("logType", 7);
            sendLog(TOPIC, null, json);
        } catch (Exception e) {
            log.error("sendSharePromoteRankRewards error:", e);
        }
    }

    /**
     * 每日签到奖励领取日志
     *
     * @param player       玩家数据
     * @param activityData 活动数据
     * @param detailId     详情ID
     * @param signType     签到类型
     * @param rewards      奖励
     * @param result       领取奖励后的道具数量
     */
    public void sendDailyLoginRewards(Player player, ActivityData activityData, int detailId, long signType,
                                      Map<Integer, Long> rewards, ItemOperationResult result) {
        try {
            JSONObject json = buildBaseInfo(activityData, detailId);
            json.put("signType", signType);
            json.put("rewards", objectMapper.writeValueAsString(rewards));
            json.put("result", objectMapper.writeValueAsString(result));
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendDailyLoginRewards error:", e);
        }
    }

    /**
     * 首充领取奖励
     *
     * @param player       玩家数据
     * @param activityData 活动数据
     * @param cfg          配置表数据
     * @param data         奖励结果
     * @param rewards      奖励
     */
    public void sendFirstPaymentJoinLog(Player player, ActivityData activityData, FirstpaymentCfg cfg, ItemOperationResult data, Map<Integer, Long> rewards) {
        try {
            JSONObject json = buildBaseInfo(activityData, cfg.getId());
            json.put("rewards", objectMapper.writeValueAsString(rewards));
            json.put("money", cfg.getMoney().toPlainString());
            json.put("remain", objectMapper.writeValueAsString(data));
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendDailyLoginRewards error:", e);
        }
    }

    /**
     * 等级礼包购买日志
     *
     * @param player 玩家数据
     * @param cfg    等级礼包配置
     */
    public void sendLevelPackBuyLog(Player player, PlayerLevelPackCfg cfg) {
        try {
            JSONObject json = new JSONObject();
            json.put("operator", 1);
            json.put("type", -1);
            json.put("level", cfg.getPlayerlevel());
            json.put("money", cfg.getPay().toPlainString());
            json.put("rewards", objectMapper.writeValueAsString(cfg.getLevelRewards()));
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendDailyLoginRewards error:", e);
        }
    }

    /**
     * 等级礼包领取日志
     *
     * @param player 玩家数据
     * @param data   道具领取后日志
     * @param cfg    等级礼包配置
     */
    public void sendLevelPackClaimLog(Player player, ItemOperationResult data, PlayerLevelPackCfg cfg) {
        try {
            JSONObject json = new JSONObject();
            json.put("operator", 2);
            json.put("type", -1);
            json.put("level", cfg.getPlayerlevel());
            json.put("money", cfg.getPay().toPlainString());
            json.put("remain", objectMapper.writeValueAsString(data));
            json.put("rewards", objectMapper.writeValueAsString(cfg.getLevelRewards()));
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendDailyLoginRewards error:", e);
        }
    }


    /**
     * 官方派奖日志
     *
     * @param player          玩家数据
     * @param activityData    活动数据
     * @param pointsType      积分类型 1 增加 2 减少
     * @param turntableType   转盘类型
     * @param deductPoints    消耗积分
     * @param remainingPoints 剩余积分
     * @param jackPotGold     奖池剩余金币
     * @param remain          玩家剩余金币
     * @param rouletteReward  转盘奖励
     */
    public void sendOfficialAwardsLog(Player player, ActivityData activityData, int pointsType, int turntableType, int deductPoints, int remainingPoints, long jackPotGold,
                                      ItemOperationResult remain, Map<Integer, Long> rouletteReward) {
        try {
            JSONObject json = buildBaseInfo(activityData, 0);
            json.put("pointsType", pointsType);
            json.put("deductPoints", deductPoints);
            json.put("remainingPoints", remainingPoints);
            if (pointsType == 2) {
                json.put("turntableType", turntableType);
                json.put("jackPotGold", jackPotGold);
                json.put("rouletteReward", objectMapper.writeValueAsString(rouletteReward));
                json.put("remain", objectMapper.writeValueAsString(remain));
            }
            sendLog(TOPIC, player, json);
        } catch (Exception e) {
            log.error("sendLevelPackClaimLog error:", e);
        }
    }
}
