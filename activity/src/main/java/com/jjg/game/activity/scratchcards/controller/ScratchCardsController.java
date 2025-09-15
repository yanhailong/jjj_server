package com.jjg.game.activity.scratchcards.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.ActivityBuilder;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.common.message.res.ResActivityBuyGift;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.scratchcards.message.bean.ScratchCardsActivity;
import com.jjg.game.activity.scratchcards.message.bean.ScratchCardsDetailInfo;
import com.jjg.game.activity.scratchcards.message.res.ResScratchCardsDetailInfo;
import com.jjg.game.activity.scratchcards.message.res.ResScratchCardsJoinActivity;
import com.jjg.game.activity.scratchcards.message.res.ResScratchCardsTypeInfo;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
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

/**
 * @author lm
 * @date 2025/9/3 17:43
 */
@Component
public class ScratchCardsController extends BaseActivityController {
    private final Logger log = LoggerFactory.getLogger(ScratchCardsController.class);

    @Override
    public AbstractResponse joinActivity(long playerId, ActivityData activityData, int detailId, int times) {
        ResScratchCardsJoinActivity res = new ResScratchCardsJoinActivity(Code.SUCCESS);
        if (times <= 0) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        res.activityId = activityData.getId();
        res.detailId = detailId;
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        WeightRandom<ScratchCardsCfg> random = new WeightRandom<>();
        for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
            if (cfgBean instanceof ScratchCardsCfg cfg && cfg.getType() == 1) {
                random.add(cfg, cfg.getWeight());
            }
        }
        //判断消耗
        Item costItem = getCostItem();
        costItem.setItemCount(costItem.getItemCount() * times);
        CommonResult<Void> removedItem = playerPackService.removeItem(playerId, costItem, "ScratchCardsJoin");
        if (!removedItem.success()) {
            res.code = removedItem.code;
            return res;
        }
        ScratchCardsCfg max7 = null;
        //奖励列表
        Map<Integer, Long> rewards = new HashMap<>();
        //循环获奖
        for (int i = 0; i < times; i++) {
            ScratchCardsCfg rewardCfg = random.next();
            if (CollectionUtil.isNotEmpty(rewardCfg.getGetitem())) {
                for (Map.Entry<Integer, Long> entry : rewardCfg.getGetitem().entrySet()) {
                    rewards.merge(entry.getKey(), entry.getValue(), Long::sum);
                }
            }
            if (max7 == null || max7.getIconNum() < rewardCfg.getIconNum()) {
                max7 = rewardCfg;
            }
        }
        if (CollectionUtil.isNotEmpty(rewards)) {
            CommonResult<Void> commonResult = playerPackService.addItems(playerId, rewards, "ScratchCardsJoin");
            if (!commonResult.success()) {
                log.error("刮刮乐添加道具失败 playerId={}", playerId);
            }
            res.numOf7 = max7 == null ? 0 : max7.getIconNum();
            res.infoList = ItemUtils.buildItemInfo(rewards);
        }
        return res;
    }

    public Item getCostItem() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.ScratchCards.SCRATCH_CARDS_COST_ITEM);
        String[] itemInfo = StringUtils.split(globalConfigCfg.getValue(), "_");
        if (itemInfo.length == 2) {
            return new Item(Integer.parseInt(itemInfo[0]), Long.parseLong(itemInfo[1]));
        }
        return null;
    }

    @Override
    public AbstractResponse claimActivityRewards(long playerId, ActivityData activityData, int detailId) {
        return null;
    }

    @Override
    public void onActivityEnd(ActivityData activityData) {

    }

    @Override
    public void onActivityStart(ActivityData activityData) {

    }

    @Override
    public int updateActivity(String jsonData) {
        return 0;
    }

    @Override
    public AbstractResponse getPlayerActivityDetail(long playerId, ActivityData activityData, int detailId) {
        long activityId = activityData.getId();
        ResScratchCardsDetailInfo detailInfo = new ResScratchCardsDetailInfo(Code.SUCCESS);
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        detailInfo.detailInfo = new ArrayList<>();
        ScratchCardsDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(activityId, baseCfgBeanMap.get(detailId), null);
        detailInfo.detailInfo.add(baseActivityDetailInfo);
        return detailInfo;
    }

    @Override
    public ScratchCardsDetailInfo buildPlayerActivityDetail(long activityId, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (baseCfgBean instanceof ScratchCardsCfg cfg) {
            ScratchCardsDetailInfo info = new ScratchCardsDetailInfo();
            info.activityId = activityId;
            info.detailId = baseCfgBean.getId();
            info.type = cfg.getType();
            info.buyPrice = cfg.getCost();
            //奖励信息
            info.rewardItems = ItemUtils.buildItemInfo(cfg.getGetitem());
            info.numOf7 = cfg.getIconNum();
            return info;
        }
        return null;
    }

    @Override
    public void buyActivityGift(long playerId, ActivityData activityData, int giftId) {
        ResActivityBuyGift res = new ResActivityBuyGift(Code.SUCCESS);
        res.activityId = activityData.getId();
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(giftId);
        if (baseCfgBean instanceof ScratchCardsCfg cfg && cfg.getType() == 2) {
            CommonResult<Void> addItems = playerPackService.addItems(playerId, cfg.getGetitem(), "ScratchCardsGift");
            if (!addItems.success()) {
                log.error("刮刮乐购买礼包自动领奖失败 playerId:{} activityData:{}", playerId, activityData);
                res.code = Code.UNKNOWN_ERROR;
                activityManager.sendToPlayer(playerId, res);
                return;
            }
            res.itemInfos = ItemUtils.buildItemInfo(cfg.getGetitem());
            activityManager.sendToPlayer(playerId, res);
        }
    }

    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(long playerId, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResScratchCardsTypeInfo cardTypeInfo = new ResScratchCardsTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cardTypeInfo;
        }
        cardTypeInfo.activityData = new ArrayList<>();
        for (List<BaseActivityDetailInfo> baseActivityDetailInfos : allDetailInfo.values()) {
            ScratchCardsActivity ScratchCardsType = new ScratchCardsActivity();
            ScratchCardsType.detailInfos = new ArrayList<>();
            cardTypeInfo.activityData.add(ScratchCardsType);
            for (BaseActivityDetailInfo baseActivityDetailInfo : baseActivityDetailInfos) {
                if (baseActivityDetailInfo instanceof ScratchCardsDetailInfo info) {
                    ScratchCardsType.detailInfos.add(info);
                }
            }
        }
        return cardTypeInfo;
    }

    @Override
    public ActivityInfo buildActivityInfo(long playerId, ActivityData activityData) {
        return ActivityBuilder.buildActivityInfo(activityData, ActivityConstant.ClaimStatus.CAN_CLAIM);
    }


    @Override
    public List<BaseCfgBean> getDetailCfgBean() {
        return new ArrayList<>(GameDataManager.getScratchCardsCfgList());
    }


    @Override
    public Class<ScratchCardsCfg> getDetailDataClass() {
        return ScratchCardsCfg.class;
    }
}
