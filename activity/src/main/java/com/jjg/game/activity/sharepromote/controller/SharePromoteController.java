package com.jjg.game.activity.sharepromote.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.ActivityBuilder;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.sharepromote.message.bean.SharePromoteActivityInfo;
import com.jjg.game.activity.sharepromote.message.bean.SharePromoteDetailInfo;
import com.jjg.game.activity.sharepromote.message.res.ResSharePromoteClaimRewards;
import com.jjg.game.activity.sharepromote.message.res.ResSharePromoteDetailInfo;
import com.jjg.game.activity.sharepromote.message.res.ResSharePromoteTypeInfo;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.PrivilegeCardCfg;
import com.jjg.game.sampledata.bean.SharePromoteCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/9/3 17:43
 */
@Component
public class SharePromoteController extends BaseActivityController {
    private final Logger log = LoggerFactory.getLogger(SharePromoteController.class);


    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        return null;
    }

    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        ResSharePromoteClaimRewards res = new ResSharePromoteClaimRewards(Code.SUCCESS);
        long playerId = player.getId();
        String lockKey = playerActivityDao.getLockKey(playerId, activityData.getId());
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(detailId);
        if (baseCfgBean instanceof PrivilegeCardCfg cfg) {
            PlayerActivityData data = null;
            CommonResult<Long> addedItems;
            redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
            try {
                //领取奖励
                Map<Integer, PlayerActivityData> dataMap = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
                if (CollectionUtil.isEmpty(dataMap)) {
                    res.code = Code.PARAM_ERROR;
                    return res;
                }
                data = dataMap.get(detailId);
                if (data == null) {
                    res.code = Code.PARAM_ERROR;
                    return res;
                }
                if (data.getClaimStatus() != ActivityConstant.ClaimStatus.CAN_CLAIM) {
                    res.code = Code.REPEAT_OP;
                    return res;
                }
                addedItems = playerPackService.addItems(playerId, cfg.getDayRebate(), "SharePromoteRewards");
                if (!addedItems.success()) {
                    res.code = Code.UNKNOWN_ERROR;
                    return res;
                }
                //修改活动数据
                data.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED);
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), dataMap);
            } catch (Exception e) {
                log.error("领取推广分享异常 playerId:{} activityId:{}", playerId, activityData.getId(), e);
            } finally {
                redisLock.unlock(lockKey);
            }
            if (data != null) {
                //添加日志
//                if (addedItems != null && addedItems.success()) {
//                    activityLogger.sendPrivilegeCardRewardsLog(player, activityData, detailId, addedItems.data, cfg.getDayRebate());
//                }
                res.activityId = activityData.getId();
                res.detailId = detailId;
                res.infoList = ItemUtils.buildItemInfo(cfg.getDayRebate());
                res.detailInfo = buildPlayerActivityDetail(activityData.getId(), cfg, data);

            }
        }
        //发送响应
        return res;
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
        ResSharePromoteDetailInfo detailInfo = new ResSharePromoteDetailInfo(Code.SUCCESS);
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
        detailInfo.detailInfo = new ArrayList<>();
        detailInfo.detailInfo.add(buildPlayerActivityDetail(activityId, baseCfgBeanMap.get(detailId), playerActivityData.get(detailId)));
        return detailInfo;
    }

    @Override
    public SharePromoteDetailInfo buildPlayerActivityDetail(long activityId, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (baseCfgBean instanceof SharePromoteCfg cfg) {
            SharePromoteDetailInfo info = new SharePromoteDetailInfo();
            info.activityId = activityId;
            info.detailId = baseCfgBean.getId();
            //奖励信息
            info.rewardItems = ItemUtils.buildItemInfo(cfg.getGetitem());
            if (data != null) {
                info.claimStatus = data.getClaimStatus();
            }
            return info;
        }
        return null;
    }

    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(long playerId, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResSharePromoteTypeInfo cardTypeInfo = new ResSharePromoteTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cardTypeInfo;
        }
        cardTypeInfo.activityData = new ArrayList<>();
        for (List<BaseActivityDetailInfo> baseActivityDetailInfos : allDetailInfo.values()) {
            SharePromoteActivityInfo detailInfos = new SharePromoteActivityInfo();
            detailInfos.detailInfos = new ArrayList<>();
            cardTypeInfo.activityData.add(detailInfos);
            for (BaseActivityDetailInfo baseActivityDetailInfo : baseActivityDetailInfos) {
                if (baseActivityDetailInfo instanceof SharePromoteDetailInfo info) {
                    detailInfos.detailInfos.add(info);
                }
            }
        }
        return cardTypeInfo;
    }

    @Override
    public ActivityInfo buildActivityInfo(long playerId, ActivityData activityData) {
        Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
        int claimStatus = 0;
        if (CollectionUtil.isNotEmpty(playerActivityData)) {
            for (PlayerActivityData privilegeCard : playerActivityData.values()) {
                if (privilegeCard.getClaimStatus() == ActivityConstant.ClaimStatus.CAN_CLAIM) {
                    claimStatus = ActivityConstant.ClaimStatus.CAN_CLAIM;
                    break;
                }
            }
        }
        return ActivityBuilder.buildActivityInfo(activityData, claimStatus);
    }


    @Override
    public List<BaseCfgBean> getDetailCfgBean() {
        return new ArrayList<>(GameDataManager.getSharePromoteCfgList());
    }


    @Override
    public Class<SharePromoteCfg> getDetailDataClass() {
        return SharePromoteCfg.class;
    }
}
