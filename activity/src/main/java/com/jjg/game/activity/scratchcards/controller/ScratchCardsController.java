package com.jjg.game.activity.scratchcards.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.activitylog.data.ScratchCardsResult;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.common.message.res.ResActivityBuyGift;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.scratchcards.message.bean.ScratchCardsActivity;
import com.jjg.game.activity.scratchcards.message.bean.ScratchCardsDetailInfo;
import com.jjg.game.activity.scratchcards.message.bean.ScratchCardsRewardsInfo;
import com.jjg.game.activity.scratchcards.message.res.ResScratchCardsDetailInfo;
import com.jjg.game.activity.scratchcards.message.res.ResScratchCardsJoinActivity;
import com.jjg.game.activity.scratchcards.message.res.ResScratchCardsTypeInfo;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.ScratchCardsCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 刮刮乐活动控制器
 * 负责玩家参与刮刮乐活动、购买礼包、获取奖励、获取活动信息等逻辑
 * <p>
 * 核心职责：
 * 1. 玩家参与活动并消耗道具
 * 2. 根据权重随机分配奖励
 * 3. 玩家购买礼包奖励发放
 * 4. 构建前端所需的活动数据
 * 5. 日志记录
 * </p>
 *
 * @author lm
 * @date 2025/9/3
 */
@Component
public class ScratchCardsController extends BaseActivityController {
    private final Logger log = LoggerFactory.getLogger(ScratchCardsController.class);

    /**
     * 玩家参与刮刮乐活动
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动明细ID
     * @param times        参与次数
     * @return 参与活动结果响应
     */
    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        ResScratchCardsJoinActivity res = new ResScratchCardsJoinActivity(Code.SUCCESS);

        // 校验参与次数
        if (times <= 0) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        res.activityId = activityData.getId();
        res.detailId = detailId;
        long playerId = player.getId();

        // 获取活动明细配置
        Map<Integer, ScratchCardsCfg> baseCfgBeanMap = getDetailCfgBean(activityData);

        // 构建权重随机器，只选择 type = ActivityConstant.ScratchCards.REWARDS_TYPE 的刮刮乐奖项
        WeightRandom<ScratchCardsCfg> random = new WeightRandom<>();
        for (ScratchCardsCfg cfg : baseCfgBeanMap.values()) {
            if (cfg.getType() == ActivityConstant.ScratchCards.REWARDS_TYPE) {
                random.add(cfg, cfg.getWeight());
            }
        }

        // 获取消耗道具，并计算总消耗
        Item costItem = getCostItem();
        costItem.setItemCount(costItem.getItemCount() * times);

        // 扣除玩家道具
        CommonResult<ItemOperationResult> removedItem = playerPackService.removeItem(playerId, costItem, "ScratchCardsJoin");
        if (!removedItem.success()) {
            res.code = removedItem.code;
            return res;
        }

        // 初始化奖励记录
        Map<Integer, Long> rewards = new HashMap<>(); // 奖励总表
        List<ScratchCardsResult> scratchCardsResults = new ArrayList<>(); // 每次抽奖结果
        //奖励分类结果
        Map<Integer, Map<Integer, Long>> rewardsClassification = new HashMap<>();
        //触发次数map
        Map<Integer, Integer> timesMap = new HashMap<>();
        // 循环执行抽奖
        for (int i = 0; i < times; i++) {
            ScratchCardsCfg rewardCfg = random.next();

            // 累加奖励道具
            if (CollectionUtil.isNotEmpty(rewardCfg.getGetitem())) {
                for (Map.Entry<Integer, Long> entry : rewardCfg.getGetitem().entrySet()) {
                    rewards.merge(entry.getKey(), entry.getValue(), Long::sum);
                }
            }

            // 构建每次刮刮乐结果
            ScratchCardsResult scratchCardsResult = new ScratchCardsResult(rewardCfg.getIconNum(), rewardCfg.getGetitem(), rewardCfg.getId());
            scratchCardsResults.add(scratchCardsResult);
            //构建分类结果
            Map<Integer, Long> map = rewardsClassification.get(rewardCfg.getIconNum());
            if (map == null && CollectionUtil.isNotEmpty(rewardCfg.getGetitem())) {
                HashMap<Integer, Long> temp = new HashMap<>();
                rewardsClassification.put(rewardCfg.getIconNum(), temp);
                rewardCfg.getGetitem().forEach((key, value) -> temp.merge(key, value, Long::sum));
            }
            timesMap.compute(rewardCfg.getIconNum(), (k, hasTimes) -> hasTimes == null ? 1 : hasTimes + 1);
        }

        // 发放奖励道具
        CommonResult<ItemOperationResult> commonResult;
        if (CollectionUtil.isNotEmpty(rewards)) {
            res.rewardsInfo = new ArrayList<>();
            //构建记录
            for (Map.Entry<Integer, Map<Integer, Long>> entry : rewardsClassification.entrySet()) {
                ScratchCardsRewardsInfo info = new ScratchCardsRewardsInfo();
                info.numOf7 = entry.getKey();
                info.infoList = ItemUtils.buildItemInfo(entry.getValue());
                info.times = timesMap.get(entry.getKey());
                res.rewardsInfo.add(info);
            }
            commonResult = playerPackService.addItems(playerId, rewards, "ScratchCardsJoin");
            if (!commonResult.success()) {
                log.error("刮刮乐添加道具失败 playerId={}", playerId);
                return res;
            }
            // 记录日志
            activityLogger.sendScratchCardsJoin(player, activityData, costItem, times, removedItem.data, commonResult.data, rewards, scratchCardsResults);
        }
        return res;
    }

    /**
     * 获取刮刮乐活动消耗的道具
     *
     * @return 消耗道具对象
     */
    public Item getCostItem() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.ScratchCards.SCRATCH_CARDS_COST_ITEM);
        String[] itemInfo = StringUtils.split(globalConfigCfg.getValue(), "_");
        if (itemInfo.length == 2) {
            return new Item(Integer.parseInt(itemInfo[0]), Long.parseLong(itemInfo[1]));
        }
        return null;
    }

    /**
     * 刮刮乐活动奖励领取接口（暂未实现）
     */
    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        return null;
    }

    /**
     * 获取玩家刮刮乐活动明细
     */
    @Override
    public AbstractResponse getPlayerActivityDetail(long playerId, ActivityData activityData, int detailId) {
        long activityId = activityData.getId();
        ResScratchCardsDetailInfo detailInfo = new ResScratchCardsDetailInfo(Code.SUCCESS);
        Map<Integer, ScratchCardsCfg> baseCfgBeanMap = getDetailCfgBean(activityData);

        detailInfo.detailInfo = new ArrayList<>();
        ScratchCardsDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(activityData, baseCfgBeanMap.get(detailId), null);
        detailInfo.detailInfo.add(baseActivityDetailInfo);
        return detailInfo;
    }

    /**
     * 构建玩家刮刮乐活动明细信息
     */
    @Override
    public ScratchCardsDetailInfo buildPlayerActivityDetail(ActivityData activityData, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (baseCfgBean instanceof ScratchCardsCfg cfg) {
            ScratchCardsDetailInfo info = new ScratchCardsDetailInfo();
            info.activityId = activityData.getId();
            info.detailId = cfg.getId();
            info.type = cfg.getType();
            info.buyPrice = cfg.getCost().doubleValue();
            // 奖励信息
            info.rewardItems = ItemUtils.buildItemInfo(cfg.getGetitem());
            info.numOf7 = cfg.getIconNum();
            return info;
        }
        return null;
    }

    /**
     * 玩家购买刮刮乐礼包
     */
    @Override
    public void buyActivityGift(Player player, ActivityData activityData, int giftId) {
        ResActivityBuyGift res = new ResActivityBuyGift(Code.SUCCESS);
        res.activityId = activityData.getId();
        long playerId = player.getId();
        Map<Integer, ScratchCardsCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        ScratchCardsCfg cfg = baseCfgBeanMap.get(giftId);
        if (cfg.getType() == ActivityConstant.ScratchCards.GIFT_TYPE) {
            CommonResult<ItemOperationResult> addItems = playerPackService.addItems(playerId, cfg.getGetitem(), "ScratchCardsGift");
            if (!addItems.success()) {
                log.error("刮刮乐购买礼包自动领奖失败 playerId:{} activityData:{}", playerId, activityData);
                res.code = Code.UNKNOWN_ERROR;
                activityManager.sendToPlayer(playerId, res);
                return;
            }
            res.itemInfos = ItemUtils.buildItemInfo(cfg.getGetitem());
            // 日志记录
            activityLogger.sendActivityGift(player, activityData, addItems.data, cfg.getGetitem(), giftId);
            activityManager.sendToPlayer(playerId, res);
        }
    }

    /**
     * 获取玩家刮刮乐活动类型信息（前端展示）
     */
    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(long playerId, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResScratchCardsTypeInfo cardTypeInfo = new ResScratchCardsTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cardTypeInfo;
        }
        cardTypeInfo.activityData = new ArrayList<>();
        for (List<BaseActivityDetailInfo> baseActivityDetailInfos : allDetailInfo.values()) {
            ScratchCardsActivity scratchCardsType = new ScratchCardsActivity();
            scratchCardsType.detailInfos = new ArrayList<>();
            cardTypeInfo.activityData.add(scratchCardsType);
            for (BaseActivityDetailInfo baseActivityDetailInfo : baseActivityDetailInfos) {
                if (baseActivityDetailInfo instanceof ScratchCardsDetailInfo info) {
                    scratchCardsType.detailInfos.add(info);
                }
            }
            // 获取剩余次数
            Item costItem = getCostItem();
            PlayerPack playerPack = playerPackService.getFromAllDB(playerId);
            if (playerPack != null) {
                scratchCardsType.remainTimes = playerPack.getItemCount(costItem.getId());
            }
        }
        return cardTypeInfo;
    }

    @Override
    public Map<Integer, ScratchCardsCfg> getDetailCfgBean(ActivityData activityData) {
        return GameDataManager.getScratchCardsCfgList()
                .stream()
                .filter(cfg -> activityData.getValue().contains(cfg.getId()))
                .collect(Collectors.toMap(BaseCfgBean::getId, cfg -> cfg));
    }

}
