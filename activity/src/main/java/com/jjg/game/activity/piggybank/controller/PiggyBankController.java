package com.jjg.game.activity.piggybank.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.ActivityBuilder;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.piggybank.data.PiggyBankData;
import com.jjg.game.activity.piggybank.message.bean.PiggyBankActivityInfo;
import com.jjg.game.activity.piggybank.message.bean.PiggyBankDetailInfo;
import com.jjg.game.activity.piggybank.message.res.ResPiggyBankActivityInfos;
import com.jjg.game.activity.piggybank.message.res.ResPiggyBankClaimRewards;
import com.jjg.game.activity.piggybank.message.res.ResPiggyBankDetailInfo;
import com.jjg.game.activity.privilegecard.data.PlayerPrivilegeCard;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.PiggyBankCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/9/3 17:43
 */
@Component
public class PiggyBankController extends BaseActivityController {
    private final Logger log = LoggerFactory.getLogger(PiggyBankController.class);
    private final MailService mailService;

    public PiggyBankController(MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    public AbstractResponse joinActivity(long playerId, ActivityData activityData, int detailId) {
        ResPiggyBankDetailInfo res = null;
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(detailId);
        if (baseCfgBean instanceof PiggyBankCfg cfg) {
            long timeMillis = System.currentTimeMillis();
            String lockKey = playerActivityDao.getLockKey(playerId, activityData.getId());
            redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
            PiggyBankData piggyBankData = null;
            try {
                Map<Integer, PiggyBankData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
                piggyBankData = playerActivityData.computeIfAbsent(detailId, key -> new PiggyBankData(activityData.getId(), activityData.getRound()));
                if (piggyBankData.getBuyTime() > 0) {
                    log.error("玩家参加活动失败 玩家已经参加过 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
                    return res;
                }
                piggyBankData.setBuyTime(timeMillis);
                if (piggyBankData.getProgress() >= cfg.getFullup()) {
                    piggyBankData.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
                    piggyBankData.setFullTime(System.currentTimeMillis());
                }
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), playerActivityData);
            } catch (Exception e) {
                log.error("玩家参加活动失败 出现异常,playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
            } finally {
                redisLock.unlock(lockKey);
            }
            res = new ResPiggyBankDetailInfo(Code.SUCCESS);
            res.detailInfo = new ArrayList<>();
            res.detailInfo.add(buildPlayerActivityDetail(activityData.getId(), cfg, piggyBankData));

        } else {
            log.error("玩家参加活动失败 活动配置为空playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
        }
        return res;
    }

    @Override
    public boolean addPlayerProgress(long playerId, ActivityData activityData, long progress, Object additionalParameters) {
        if (additionalParameters instanceof Integer itemId && !itemId.equals(ItemUtils.getGoldItemId())) {
            return false;
        }
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.PiggyBank.INCOME_PER_TEN_THOUSAND);
        //基础值
        BigDecimal baseAdd = BigDecimal.valueOf(progress).multiply(BigDecimal.valueOf(globalConfigCfg.getIntValue())).divide(BigDecimal.valueOf(10000), RoundingMode.DOWN);
        long activityId = activityData.getId();
        //添加玩家进度
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        boolean canClaim = false;
        String lockKey = playerActivityDao.getLockKey(playerId, activityId);
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            Map<Integer, PiggyBankData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
            for (Map.Entry<Integer, BaseCfgBean> entry : baseCfgBeanMap.entrySet()) {
                BaseCfgBean cfgBean = entry.getValue();
                if (cfgBean instanceof PiggyBankCfg cfg) {
                    PiggyBankData piggyBankData = playerActivityData.computeIfAbsent(entry.getKey(), key -> new PiggyBankData(activityData.getId(), activityData.getRound()));
                    if (piggyBankData.getProgress() > cfg.getFullup()) {
                        continue;
                    }
                    long addValue = baseAdd.multiply(BigDecimal.valueOf(cfg.getWeight())).divide(BigDecimal.valueOf(10000), RoundingMode.DOWN).longValue();
                    piggyBankData.setProgress(Math.min(cfg.getFullup(), piggyBankData.getProgress() + addValue));
                    if (piggyBankData.getProgress() >= cfg.getFullup() && piggyBankData.getBuyTime() > 0) {
                        piggyBankData.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
                        piggyBankData.setFullTime(System.currentTimeMillis());
                        canClaim = true;
                    }
                }
            }
            if (!playerActivityData.isEmpty()) {
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityId, playerActivityData);
            }
        } catch (Exception e) {
            log.error("储钱罐添加玩家进度异常 playerId:{}  activityId:{} detailId:{}", playerId, activityData.getId(), activityId, e);
        } finally {
            redisLock.unlock(lockKey);
        }
        return canClaim;
    }

    @Override
    public AbstractResponse claimActivityRewards(long playerId, ActivityData activityData, int detailId) {
        ResPiggyBankClaimRewards res = new ResPiggyBankClaimRewards(Code.SUCCESS);
        String lockKey = playerActivityDao.getLockKey(playerId, activityData.getId());
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(detailId);
        if (baseCfgBean instanceof PiggyBankCfg cfg) {
            PiggyBankData data = null;
            redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
            try {
                //领取奖励
                Map<Integer, PiggyBankData> dataMap = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
                data = dataMap.get(detailId);
                if (data == null) {
                    res.code = Code.PARAM_ERROR;
                    return res;
                }
                if (data.getClaimStatus() != ActivityConstant.ClaimStatus.CAN_CLAIM) {
                    res.code = Code.ERROR_REQ;
                    return res;
                }
                CommonResult<Void> addedItems = playerPackService.addItems(playerId, cfg.getGetitem(), "privilegeCardRewords");
                if (!addedItems.success()) {
                    res.code = Code.UNKNOWN_ERROR;
                    return res;
                }
                //修改活动数据
                resetPiggyBankData(data);
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), dataMap);
            } catch (Exception e) {
                log.error("领取每日奖金异常 playerId:{} activityId:{}", playerId, activityData.getId(), e);
            } finally {
                redisLock.unlock(lockKey);
            }
            if (data != null) {
                res.activityId = activityData.getId();
                res.detailId = detailId;
                res.infoList = ItemUtils.buildItemInfo(cfg.getGetitem());
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
        ResPiggyBankDetailInfo detailInfo = new ResPiggyBankDetailInfo(Code.SUCCESS);
        ActivityData data = activityManager.getActivityData().get(activityId);
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        Map<Integer, PlayerPrivilegeCard> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, data.getType(), activityId);
        detailInfo.detailInfo = new ArrayList<>();
        PiggyBankDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(activityId, baseCfgBeanMap.get(detailId), playerActivityData.get(detailId));
        detailInfo.detailInfo.add(baseActivityDetailInfo);
        return detailInfo;
    }

    @Override
    public PiggyBankDetailInfo buildPlayerActivityDetail(long activityId, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (baseCfgBean instanceof PiggyBankCfg cfg) {
            PiggyBankDetailInfo info = new PiggyBankDetailInfo();
            info.activityId = activityId;
            info.detailId = baseCfgBean.getId();
            info.rechargePrice = cfg.getPay();
            //奖励信息
            info.rewardItems = ItemUtils.buildItemInfo(cfg.getGetitem());
            if (data instanceof PiggyBankData piggyBankData) {
                info.claimStatus = piggyBankData.getClaimStatus();
                info.progress = piggyBankData.getProgress();
            }
            return info;
        }
        return null;
    }

    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(long playerId, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResPiggyBankActivityInfos activityInfos = new ResPiggyBankActivityInfos(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return activityInfos;
        }
        activityInfos.activityData = new ArrayList<>();
        for (List<BaseActivityDetailInfo> baseActivityDetailInfos : allDetailInfo.values()) {
            PiggyBankActivityInfo activityInfo = new PiggyBankActivityInfo();
            activityInfo.detailInfos = new ArrayList<>();
            activityInfos.activityData.add(activityInfo);
            for (BaseActivityDetailInfo baseActivityDetailInfo : baseActivityDetailInfos) {
                if (baseActivityDetailInfo instanceof PiggyBankDetailInfo info) {
                    activityInfo.detailInfos.add(info);
                }
            }
        }
        return activityInfos;
    }

    @Override
    public ActivityInfo buildActivityInfo(long playerId, ActivityData activityData) {
        Map<Integer, PiggyBankData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
        int claimStatus = 0;
        if (CollectionUtil.isNotEmpty(playerActivityData)) {
            for (PiggyBankData data : playerActivityData.values()) {
                if (data.getClaimStatus() == ActivityConstant.ClaimStatus.CAN_CLAIM) {
                    claimStatus = ActivityConstant.ClaimStatus.CAN_CLAIM;
                    break;
                }
            }
        }
        return ActivityBuilder.buildActivityInfo(activityData, claimStatus);
    }

    @Override
    public void checkPlayerDataAndReset(long playerId, ActivityData activityData) {
        long timeMillis = System.currentTimeMillis();
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        String lockKey = playerActivityDao.getLockKey(playerId, activityData.getId());
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            Map<Integer, PiggyBankData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
            for (Map.Entry<Integer, PiggyBankData> piggyBankDataEntry : playerActivityData.entrySet()) {
                PiggyBankData piggyBankData = piggyBankDataEntry.getValue();
                if (piggyBankData.getClaimStatus() != ActivityConstant.ClaimStatus.CAN_CLAIM) {
                    continue;
                }
                BaseCfgBean baseCfgBean = baseCfgBeanMap.get(piggyBankDataEntry.getKey());
                if (baseCfgBean instanceof PiggyBankCfg cfg) {
                    //进行重置发奖
                    if (piggyBankData.getFullTime() + (long) TimeHelper.ONE_DAY_OF_MILLIS * cfg.getResetime() >= timeMillis) {
                        mailService.addCfgMail(playerId, ActivityConstant.PiggyBank.MAIL_ID, ItemUtils.buildItems(cfg.getGetitem()));
                        resetPiggyBankData(piggyBankData);
                    }
                }
            }
        } catch (Exception e) {
            log.error("储钱罐超时自动发奖失败 playerId:{} activityId:{}", playerId, activityData.getId(), e);
        } finally {
            redisLock.unlock(lockKey);
        }
        super.checkPlayerDataAndReset(playerId, activityData);
    }

    /**
     * 重置玩家储钱罐数据
     */
    private void resetPiggyBankData(PiggyBankData piggyBankData) {
        piggyBankData.setFullTime(0);
        piggyBankData.setProgress(0);
        piggyBankData.setBuyTime(0);
        piggyBankData.setClaimStatus(ActivityConstant.ClaimStatus.NOT_CLAIM);
    }

    @Override
    public List<BaseCfgBean> getDetailCfgBean() {
        return new ArrayList<>(GameDataManager.getPiggyBankCfgList());
    }


    @Override
    public Class<PiggyBankCfg> getDetailDataClass() {
        return PiggyBankCfg.class;
    }
}
