package com.jjg.game.activity.privilegecard.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.ActivityBuilder;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.privilegecard.data.PlayerPrivilegeCard;
import com.jjg.game.activity.privilegecard.message.bean.PrivilegeCardDetailInfo;
import com.jjg.game.activity.privilegecard.message.bean.PrivilegeCardType;
import com.jjg.game.activity.privilegecard.message.res.ResPrivilegeCardClaimRewards;
import com.jjg.game.activity.privilegecard.message.res.ResPrivilegeCardDetailInfo;
import com.jjg.game.activity.privilegecard.message.res.ResPrivilegeCardTypeInfo;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.PrivilegeCardCfg;
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
public class PrivilegeCardController extends BaseActivityController {
    private final Logger log = LoggerFactory.getLogger(PrivilegeCardController.class);
    //redis 锁
    private final RedisLock lock;

    public PrivilegeCardController(RedisLock lock) {
        this.lock = lock;
    }

    public int getClaimStatus(PlayerPrivilegeCard data, long timeMillis) {
        //判断是否过期
        if (data.getEndTime() != -1 && data.getEndTime() < timeMillis) {
            return ActivityConstant.ClaimStatus.NOT_CLAIM;
        }
        //判断今天是否领取
        if (TimeHelper.inSameDay(data.getLastClaimTime(), timeMillis)) {
            return ActivityConstant.ClaimStatus.CLAIMED;
        }
        return ActivityConstant.ClaimStatus.CAN_CLAIM;
    }

    @Override
    public void joinActivity(long playerId, ActivityData activityData, int detailId) {
        if (!activityData.getValue().contains(detailId)) {
            log.error("玩家参加活动失败 已经不存在该活动playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
            return;
        }
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(detailId);
        if (baseCfgBean instanceof PrivilegeCardCfg cfg) {
            try {
                long timeMillis = System.currentTimeMillis();
                Map<Integer, PlayerPrivilegeCard> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
                PlayerPrivilegeCard privilegeCard = playerActivityData.computeIfAbsent(detailId, key -> new PlayerPrivilegeCard(activityData.getId()));
                if (privilegeCard.getEndTime() > timeMillis) {
                    log.error("玩家参加活动失败 玩家已经参加过 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
                    return;
                }
                privilegeCard.setBuyTime(timeMillis);
                if (cfg.getDays() == -1) {
                    privilegeCard.setEndTime(-1);
                } else {
                    privilegeCard.setEndTime(timeMillis + (long) cfg.getDays() * TimeHelper.ONE_DAY_OF_MILLIS);
                }
                if (CollectionUtil.isNotEmpty(cfg.getGetItem())) {
                    //购买奖励
                    CommonResult<Void> addedItems = playerPackService.addItems(playerId, cfg.getGetItem(), "privilegeCardBuy");
                    if (!addedItems.success()) {
                        log.error("购买每日奖金时 添加道具失败 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
                    }
                }
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), playerActivityData);
            } catch (Exception e) {
                log.error("购买每日奖金 出现异常 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId, e);
            }
        } else {
            log.error("玩家参加活动失败 活动配置为空playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
        }
    }

    @Override
    public AbstractResponse claimActivityRewards(long playerId, ActivityData activityData, int detailId) {
        ResPrivilegeCardClaimRewards res = new ResPrivilegeCardClaimRewards(Code.SUCCESS);
        String lockKey = playerActivityDao.getLockKey(playerId, activityData.getId());
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(detailId);
        if (baseCfgBean instanceof PrivilegeCardCfg cfg) {
            PlayerPrivilegeCard data = null;
            lock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
            try {
                //领取奖励
                Map<Integer, PlayerPrivilegeCard> dataMap = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
                if (CollectionUtil.isEmpty(dataMap)) {
                    return res;
                }
                data = dataMap.get(detailId);
                if (data == null) {
                    return res;
                }
                long timeMillis = System.currentTimeMillis();
                int claimStatus = getClaimStatus(data, timeMillis);
                if (claimStatus != ActivityConstant.ClaimStatus.CAN_CLAIM) {
                    res.code = Code.REPEAT_OP;
                    return res;
                }
                CommonResult<Void> addedItems = playerPackService.addItems(playerId, cfg.getDayRebate(), "活动");
                if (!addedItems.success()) {
                    res.code = Code.UNKNOWN_ERROR;
                    return res;
                }
                //修改活动数据
                data.setLastClaimTime(timeMillis);
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), dataMap);
            } catch (Exception e) {
                log.error("领取每日奖金异常 playerId:{} activityId:{}", playerId, activityData.getId(), e);
            } finally {
                lock.unlock(lockKey);
            }
            if (data != null) {
                res.activityId = activityData.getId();
                res.detailId = detailId;
                res.infoList = ItemUtils.buildItemInfo(cfg.getDayRebate());
                BaseActivityDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(activityData.getId(), cfg, data);
                if (baseActivityDetailInfo instanceof PrivilegeCardDetailInfo info) {
                    res.detailInfo = info;
                }
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
    public AbstractResponse getPlayerActivityDetail(long playerId, long activityId, int detailId) {
        ResPrivilegeCardDetailInfo detailInfo = new ResPrivilegeCardDetailInfo(Code.SUCCESS);
        ActivityData data = activityManager.getActivityData().get(activityId);
        if (data == null || detailId != -1 && !data.getValue().contains(detailId)) {
            return detailInfo;
        }
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        if (CollectionUtil.isEmpty(baseCfgBeanMap) || detailId != -1 && !baseCfgBeanMap.containsKey(detailId)) {
            return detailInfo;
        }
        Map<Integer, PlayerPrivilegeCard> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, data.getType(), activityId);
        detailInfo.detailInfo = new ArrayList<>();
        if (detailId == -1) {
            for (Integer id : data.getValue()) {
                BaseActivityDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(activityId, baseCfgBeanMap.get(id), playerActivityData.get(id));
                if (baseActivityDetailInfo instanceof PrivilegeCardDetailInfo cardDetailInfo) {
                    detailInfo.detailInfo.add(cardDetailInfo);
                }
            }
        } else {
            BaseActivityDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(activityId, baseCfgBeanMap.get(detailId), playerActivityData.get(detailId));
            if (baseActivityDetailInfo instanceof PrivilegeCardDetailInfo cardDetailInfo) {
                detailInfo.detailInfo.add(cardDetailInfo);
            }
        }
        return detailInfo;
    }

    @Override
    public BaseActivityDetailInfo buildPlayerActivityDetail(long activityId, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (baseCfgBean instanceof PrivilegeCardCfg cfg) {
            PrivilegeCardDetailInfo info = new PrivilegeCardDetailInfo();
            info.activityId = activityId;
            info.detailId = baseCfgBean.getId();
            info.rechargePrice = cfg.getPurchasecost();
            Map<Integer, Long> totalGetHashMap = new HashMap<>(cfg.getTotalRebate());
            totalGetHashMap.putAll(cfg.getGetItem());
            info.totalGet = ItemUtils.buildItemInfo(totalGetHashMap);
            //奖励信息
            info.rewardItems = ItemUtils.buildItemInfo(cfg.getDayRebate());
            info.days = cfg.getDays();
            long timeMillis = System.currentTimeMillis();
            if (data instanceof PlayerPrivilegeCard privilegeCard) {
                info.claimStatus = getClaimStatus(privilegeCard, timeMillis);
                info.remainTime = privilegeCard.getEndTime() - timeMillis;
            }
            return info;
        }
        return null;
    }

    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(List<List<BaseActivityDetailInfo>> allDetailInfo) {
        ResPrivilegeCardTypeInfo cardTypeInfo = new ResPrivilegeCardTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cardTypeInfo;
        }
        cardTypeInfo.activityData = new ArrayList<>();
        for (List<BaseActivityDetailInfo> baseActivityDetailInfos : allDetailInfo) {
            PrivilegeCardType privilegeCardType = new PrivilegeCardType();
            privilegeCardType.detailInfos = new ArrayList<>();
            cardTypeInfo.activityData.add(privilegeCardType);
            for (BaseActivityDetailInfo baseActivityDetailInfo : baseActivityDetailInfos) {
                if (baseActivityDetailInfo instanceof PrivilegeCardDetailInfo info) {
                    privilegeCardType.detailInfos.add(info);
                }
            }
        }
        return cardTypeInfo;
    }

    @Override
    public ActivityInfo buildActivityInfo(long playerId, ActivityData activityData) {
        Map<Integer, PlayerPrivilegeCard> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
        int claimStatus = 0;
        if (CollectionUtil.isNotEmpty(playerActivityData)) {
            long timeMillis = System.currentTimeMillis();
            for (PlayerPrivilegeCard privilegeCard : playerActivityData.values()) {
                int status = getClaimStatus(privilegeCard, timeMillis);
                if (status == ActivityConstant.ClaimStatus.CAN_CLAIM) {
                    claimStatus = ActivityConstant.ClaimStatus.CAN_CLAIM;
                    break;
                }
            }
        }
        return ActivityBuilder.buildActivityInfo(activityData, claimStatus);
    }


    @Override
    public List<BaseCfgBean> getDetailCfgBean() {
        return new ArrayList<>(GameDataManager.getPrivilegeCardCfgList());
    }


    @Override
    public Class<PrivilegeCardCfg> getDetailDataClass() {
        return PrivilegeCardCfg.class;
    }
}
