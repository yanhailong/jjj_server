package com.jjg.game.activity.dailylogin.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.dailylogin.dao.DailyLoginDao;
import com.jjg.game.activity.dailylogin.message.bean.DailyLoginActivityInfo;
import com.jjg.game.activity.dailylogin.message.bean.DailyLoginDetailInfo;
import com.jjg.game.activity.dailylogin.message.res.ResDailyLoginClaimRewards;
import com.jjg.game.activity.dailylogin.message.res.ResDailyLoginDetailInfo;
import com.jjg.game.activity.dailylogin.message.res.ResDailyLoginTypeInfo;
import com.jjg.game.activity.privilegecard.data.PlayerPrivilegeCard;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.DailyRewardsCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/9/3
 */
@Component
public class DailyLoginController extends BaseActivityController {

    private final Logger log = LoggerFactory.getLogger(DailyLoginController.class);
    private final DailyLoginDao dailyLoginDao;

    public DailyLoginController(DailyLoginDao dailyLoginDao) {
        this.dailyLoginDao = dailyLoginDao;
    }

    /**
     * 玩家加入每日签到活动
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动明细ID
     * @param times        加入次数（暂未使用）
     * @return 返回玩家特权卡活动详情
     */
    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        return null;
    }

    @Override
    public boolean addPlayerProgress(long playerId, ActivityData activityData, long progress, Object additionalParameters) {
        //获取配置信息
        long activityId = activityData.getId();
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        if (CollectionUtil.isEmpty(baseCfgBeanMap)) {
            return false;
        }
        long continuousLoginDay = dailyLoginDao.getContinuousLoginDay(activityId, playerId);
        long cumulativeLoginDay = dailyLoginDao.getCumulativeLoginDay(activityId, playerId);
        boolean change = false;
        String lockKey = playerActivityDao.getLockKey(playerId, activityId);
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
            for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                if (cfgBean instanceof DailyRewardsCfg cfg) {
                    //连续奖励
                    if ((cfg.getType() == ActivityConstant.DailyLogin.CONTINUE_TYPE && continuousLoginDay >= cfg.getDays()) ||
                            cfg.getType() == ActivityConstant.DailyLogin.CUMULATIVE_TYPE && cumulativeLoginDay >= cfg.getDays()) {
                        PlayerActivityData data = playerActivityData.computeIfAbsent(cfg.getId(), key -> new PlayerActivityData(activityId, activityData.getRound()));
                        //未领取的设置为领取
                        if (data.getClaimStatus() != ActivityConstant.ClaimStatus.CLAIMED) {
                            data.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
                            change = true;
                        }
                    }
                }
            }
            playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityId, playerActivityData);
        } catch (Exception e) {
            log.error("每日签到增加进度异常 playerId:{} activityId:{}", playerId, activityId, e);
        } finally {
            redisLock.unlock(lockKey);
        }
        return change;
    }

    /**
     * 玩家每日特权奖励
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动明细ID
     * @return 返回领取奖励结果
     */
    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        ResDailyLoginClaimRewards res = new ResDailyLoginClaimRewards(Code.SUCCESS);
        long playerId = player.getId();
        long activityId = activityData.getId();
        String lockKey = playerActivityDao.getLockKey(playerId, activityId);
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(detailId);

        if (!(baseCfgBean instanceof DailyRewardsCfg cfg)) {
            return res;
        }
        PlayerActivityData data = null;
        CommonResult<ItemOperationResult> addedItems = null;
        // 加锁，保证领取操作原子性
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            Map<Integer, PlayerActivityData> dataMap = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
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
            // 发放每日奖励
            addedItems = playerPackService.addItems(playerId, cfg.getGetItem(), "DailyLoginRewords");
            if (!addedItems.success()) {
                res.code = Code.UNKNOWN_ERROR;
                return res;
            }
            // 更新领取时间
            data.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED);
            playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityId, dataMap);
            dailyLoginDao.updateClaimTime(activityId, playerId);
            dailyLoginDao.addContinuousLoginDay(activityId, playerId);
            dailyLoginDao.addCumulativeLoginDay(activityId, playerId);
        } catch (Exception e) {
            log.error("领取每日签到异常 playerId:{} activityId:{} detailid:{}", playerId, activityId, detailId, e);
        } finally {
            redisLock.unlock(lockKey);
        }
        // 构建响应数据
        if (data != null) {
            if (addedItems != null && addedItems.success()) {
                //TODO 日志
            }
            res.activityId = activityId;
            res.detailId = detailId;
            res.infoList = ItemUtils.buildItemInfo(cfg.getGetItem());
            res.detailInfo = buildPlayerActivityDetail(activityId, cfg, data);
        }
        return res;
    }

    /**
     * 构建每日签到活动详情
     *
     * @param activityId  活动ID
     * @param baseCfgBean 活动配置
     * @param data        玩家特权卡数据
     * @return 返回特权卡详情信息
     */
    @Override
    public DailyLoginDetailInfo buildPlayerActivityDetail(long activityId, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (!(baseCfgBean instanceof DailyRewardsCfg cfg)) {
            return null;
        }
        DailyLoginDetailInfo info = new DailyLoginDetailInfo();
        info.activityId = activityId;
        info.detailId = baseCfgBean.getId();

        // 奖励信息
        info.rewardItems = ItemUtils.buildItemInfo(cfg.getGetItem());
        if (data != null) {
            info.claimStatus = data.getClaimStatus();
        }
        info.type = cfg.getType();
        return info;
    }

    /**
     * 获取玩家每日签到活动明细
     */
    @Override
    public AbstractResponse getPlayerActivityDetail(long playerId, ActivityData activityData, int detailId) {
        long activityId = activityData.getId();
        ResDailyLoginDetailInfo detailInfo = new ResDailyLoginDetailInfo(Code.SUCCESS);
        //活动数据
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        Map<Integer, PlayerPrivilegeCard> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);

        detailInfo.detailInfo = new ArrayList<>();
        detailInfo.detailInfo.add(buildPlayerActivityDetail(activityId, baseCfgBeanMap.get(detailId), playerActivityData.get(detailId)));

        return detailInfo;
    }

    /**
     * 构建活动类型信息
     */
    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(long playerId, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResDailyLoginTypeInfo cardTypeInfo = new ResDailyLoginTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cardTypeInfo;
        }
        cardTypeInfo.activityData = new ArrayList<>();
        for (Map.Entry<Long, List<BaseActivityDetailInfo>> entry : allDetailInfo.entrySet()) {
            DailyLoginActivityInfo activityInfo = new DailyLoginActivityInfo();

            activityInfo.detailInfos = new ArrayList<>();
            cardTypeInfo.activityData.add(activityInfo);

            for (BaseActivityDetailInfo baseActivityDetailInfo : entry.getValue()) {
                if (baseActivityDetailInfo instanceof DailyLoginDetailInfo info) {
                    activityInfo.detailInfos.add(info);
                }
            }
            activityInfo.cumulativeDay = dailyLoginDao.getCumulativeLoginDay(entry.getKey(), playerId);
        }
        return cardTypeInfo;
    }

    @Override
    public void checkPlayerDataAndReset(long playerId, ActivityData activityData) {
        super.checkPlayerDataAndReset(playerId, activityData);
        Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
        if (CollectionUtil.isEmpty(playerActivityData)) {
            return;
        }
        //1.连续全部领取完成清理
        //2.累计全部领取完成清理
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        //是否清理连续签到
        boolean clearContinuousDays = true;
        //是否清理累计签到
        boolean clearCumulativeDays = true;
        for (BaseCfgBean bean : baseCfgBeanMap.values()) {
            if (bean instanceof DailyRewardsCfg cfg) {
                if (cfg.getType() == ActivityConstant.DailyLogin.CONTINUE_TYPE) {
                    PlayerActivityData data = playerActivityData.get(cfg.getId());
                    if (data == null || data.getClaimStatus() != ActivityConstant.ClaimStatus.CLAIMED) {
                        clearContinuousDays = false;
                    }
                }
                if (cfg.getType() == ActivityConstant.DailyLogin.CUMULATIVE_TYPE) {
                    PlayerActivityData data = playerActivityData.get(cfg.getId());
                    if (data == null || data.getClaimStatus() != ActivityConstant.ClaimStatus.CLAIMED) {
                        clearCumulativeDays = false;
                    }
                }
                if (!clearCumulativeDays && !clearContinuousDays) {
                    break;
                }
            }
        }
        //获取上次领取时间
        long claimTime = dailyLoginDao.getClaimTime(activityData.getId(), playerId);
        long currentTimeMillis = System.currentTimeMillis();
        long calculated = TimeHelper.calculateDifference(ChronoUnit.DAYS, claimTime, currentTimeMillis);
        //3.未连续登陆清除连续登录数据
        if (!clearContinuousDays) {
            clearContinuousDays = calculated > 1;
        }
        Iterator<Map.Entry<Integer, PlayerActivityData>> iterator = playerActivityData.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, PlayerActivityData> entry = iterator.next();
            BaseCfgBean baseCfgBean = baseCfgBeanMap.get(entry.getKey());
            if (baseCfgBean instanceof DailyRewardsCfg cfg &&
                    ((cfg.getType() == ActivityConstant.DailyLogin.CONTINUE_TYPE && clearContinuousDays)
                            || (cfg.getType() == ActivityConstant.DailyLogin.CUMULATIVE_TYPE && clearCumulativeDays))) {
                iterator.remove();
            }
        }
        if (clearContinuousDays) {
            dailyLoginDao.delContinuousLoginDay(activityData.getId(), playerId);
        }
        if (clearCumulativeDays) {
            dailyLoginDao.delCumulativeLoginDay(activityData.getId(), playerId);
        }
        playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), playerActivityData);
    }

    @Override
    public List<BaseCfgBean> getDetailCfgBean() {
        return new ArrayList<>(GameDataManager.getDailyRewardsCfgList());
    }

    @Override
    public Class<DailyRewardsCfg> getDetailDataClass() {
        return DailyRewardsCfg.class;
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
}
