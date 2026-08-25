package com.jjg.game.activity.adsReward;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.adsReward.message.bean.AdsRewardActivityInfo;
import com.jjg.game.activity.adsReward.message.bean.AdsRewardDetailInfo;
import com.jjg.game.activity.adsReward.message.res.ResAdsRewardClaimRewards;
import com.jjg.game.activity.adsReward.message.res.ResAdsRewardInfo;
import com.jjg.game.activity.adsReward.message.res.ResAdsRewardWatch;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.VideoRewardCfg;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频福利累计奖励活动。
 */
@Component
public class AdsRewardController extends BaseActivityController {
    private static final long DATA_EXPIRE_SECONDS = TimeHelper.DAY_SECOND * 2L;

    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        ResAdsRewardWatch res = new ResAdsRewardWatch(Code.SUCCESS);
        Map<Integer, VideoRewardCfg> cfgMap = getDetailCfgBean(activityData);
        if (times != 1 || activityData == null || CollectionUtil.isEmpty(activityData.getValue())
                || !activityData.getValue().contains(detailId)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        if (!validConfigs(activityData, cfgMap)) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }

        long playerId = player.getId();
        String lockKey = playerActivityDao.getLockKey(playerId, activityData.getId());
        boolean locked = false;
        try {
            locked = redisLock.tryLockWithDefaultTime(lockKey);
            if (!locked) {
                res.code = Code.FAIL;
                return res;
            }

            int day = TimeHelper.getDayNumerical();
            int dailyLimit = getDailyLimit(cfgMap);
            int watchCount = getWatchCount(playerId, activityData, day);
            if (watchCount >= dailyLimit) {
                res.code = Code.TODAY_CLIAM_LIMIT;
                res.activityInfo = buildActivityInfo(playerId, activityData, cfgMap, day, watchCount);
                return res;
            }

            watchCount = countDao.incrementWithoutExpireRefresh(
                    getCountFeature(activityData, day), String.valueOf(playerId), BigDecimal.ONE, DATA_EXPIRE_SECONDS).intValue();
            res.activityInfo = buildActivityInfo(playerId, activityData, cfgMap, day, watchCount);
            log.info("视频福利观看计数成功 playerId:{} activityId:{} detailId:{} watchCount:{} dailyLimit:{}",
                    playerId, activityData.getId(), detailId, watchCount, dailyLimit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            res.code = Code.FAIL;
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("视频福利观看计数异常 playerId:{} activityId:{} detailId:{}",
                    playerId, activityData.getId(), detailId, e);
        } finally {
            if (locked) {
                redisLock.tryUnlock(lockKey);
            }
        }
        return res;
    }

    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        ResAdsRewardClaimRewards res = new ResAdsRewardClaimRewards(Code.SUCCESS);
        Map<Integer, VideoRewardCfg> cfgMap = getDetailCfgBean(activityData);
        if (activityData == null || CollectionUtil.isEmpty(activityData.getValue())
                || !activityData.getValue().contains(detailId)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        VideoRewardCfg cfg = cfgMap.get(detailId);
        if (!validConfigs(activityData, cfgMap) || cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }

        Map<Integer, Long> rewards = toRewards(cfg);
        long playerId = player.getId();
        String lockKey = playerActivityDao.getLockKey(playerId, activityData.getId());
        boolean locked = false;
        try {
            locked = redisLock.tryLockWithDefaultTime(lockKey);
            if (!locked) {
                res.code = Code.FAIL;
                return res;
            }

            int day = TimeHelper.getDayNumerical();
            int watchCount = getWatchCount(playerId, activityData, day);
            if (watchCount < cfg.getCount()) {
                res.code = Code.ERROR_REQ;
                return res;
            }

            Map<Integer, PlayerActivityData> claimedData = getTodayClaimedData(playerId, activityData, day);
            if (isClaimed(claimedData.get(detailId), day)) {
                res.code = Code.REPEAT_OP;
                res.activityInfo = buildActivityInfo(activityData, cfgMap, claimedData, day, watchCount);
                return res;
            }

            CommonResult<ItemOperationResult> added = playerPackService.addItems(
                    playerId, rewards, AddType.ACTIVITY_ADS_REWARD);
            if (!added.success()) {
                res.code = added.code;
                return res;
            }

            PlayerActivityData data = new PlayerActivityData(activityData.getId(), day);
            data.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED);
            claimedData.put(detailId, data);
            playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), claimedData);

            res.infoList = ItemUtils.buildItemInfo(rewards);
            res.activityInfo = buildActivityInfo(activityData, cfgMap, claimedData, day, watchCount);
            log.info("视频福利领取成功 playerId:{} activityId:{} detailId:{} watchCount:{} rewards:{}",
                    playerId, activityData.getId(), detailId, watchCount, rewards);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            res.code = Code.FAIL;
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("视频福利领取异常 playerId:{} activityId:{} detailId:{}",
                    playerId, activityData.getId(), detailId, e);
        } finally {
            if (locked) {
                redisLock.tryUnlock(lockKey);
            }
        }
        return res;
    }

    @Override
    public AdsRewardDetailInfo buildPlayerActivityDetail(Player player, ActivityData activityData,
                                                          BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (!(baseCfgBean instanceof VideoRewardCfg cfg)) {
            return null;
        }
        int day = TimeHelper.getDayNumerical();
        int watchCount = getWatchCount(player.getId(), activityData, day);
        return buildDetail(activityData, cfg, data, day, watchCount);
    }

    @Override
    public AbstractResponse getPlayerActivityDetail(Player player, ActivityData activityData, int detailId) {
        ResAdsRewardInfo res = buildInfoResponse(player, activityData);
        if (res.code == Code.SUCCESS && res.activityInfo != null) {
            res.activityInfo.detailInfos.removeIf(detail -> detail.detailId != detailId);
        }
        return res;
    }

    @Override
    public AbstractResponse getPlayerActivityInfoByType(Player player, ActivityType activityType) {
        ResAdsRewardInfo res = new ResAdsRewardInfo(Code.SUCCESS);
        if (activityType != ActivityType.ADS_REWARD) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        ActivityData activityData = activityManager.getOpenActivityData(player, ActivityType.ADS_REWARD);
        if (activityData == null) {
            return res;
        }
        return buildInfoResponse(player, activityData);
    }

    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(Player player,
                                                           Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        return getPlayerActivityInfoByType(player, ActivityType.ADS_REWARD);
    }

    @Override
    public boolean hasRedDot(long playerId, ActivityData activityData) {
        if (!activityData.canRun()) {
            return false;
        }
        Map<Integer, VideoRewardCfg> cfgMap = getDetailCfgBean(activityData);
        if (!validConfigs(activityData, cfgMap)) {
            return false;
        }
        int day = TimeHelper.getDayNumerical();
        int watchCount = getWatchCount(playerId, activityData, day);
        Map<Integer, PlayerActivityData> claimedData = getTodayClaimedData(playerId, activityData, day);
        for (VideoRewardCfg cfg : cfgMap.values()) {
            if (watchCount >= cfg.getCount() && !isClaimed(claimedData.get(cfg.getId()), day)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map<Integer, VideoRewardCfg> getDetailCfgBean(ActivityData activityData) {
        if (activityData == null || CollectionUtil.isEmpty(activityData.getValue())) {
            return Map.of();
        }
        Map<Integer, VideoRewardCfg> allCfg = GameDataManager.getVideoRewardCfgMap();
        Map<Integer, VideoRewardCfg> result = new LinkedHashMap<>(activityData.getValue().size());
        for (Integer detailId : activityData.getValue()) {
            VideoRewardCfg cfg = allCfg.get(detailId);
            if (cfg != null) {
                result.put(detailId, cfg);
            }
        }
        return result;
    }

    private ResAdsRewardInfo buildInfoResponse(Player player, ActivityData activityData) {
        ResAdsRewardInfo res = new ResAdsRewardInfo(Code.SUCCESS);
        Map<Integer, VideoRewardCfg> cfgMap = getDetailCfgBean(activityData);
        if (!validConfigs(activityData, cfgMap)) {
            res.code = Code.SAMPLE_ERROR;
            log.error("视频福利配置错误 activityId:{} detailIds:{}", activityData.getId(), activityData.getValue());
            return res;
        }
        int day = TimeHelper.getDayNumerical();
        int watchCount = getWatchCount(player.getId(), activityData, day);
        res.activityInfo = buildActivityInfo(player.getId(), activityData, cfgMap, day, watchCount);
        return res;
    }

    private AdsRewardActivityInfo buildActivityInfo(long playerId, ActivityData activityData,
                                                     Map<Integer, VideoRewardCfg> cfgMap, int day, int watchCount) {
        Map<Integer, PlayerActivityData> claimedData = getTodayClaimedData(playerId, activityData, day);
        return buildActivityInfo(activityData, cfgMap, claimedData, day, watchCount);
    }

    private AdsRewardActivityInfo buildActivityInfo(ActivityData activityData, Map<Integer, VideoRewardCfg> cfgMap,
                                                     Map<Integer, PlayerActivityData> claimedData,
                                                     int day, int watchCount) {
        AdsRewardActivityInfo info = new AdsRewardActivityInfo();
        info.activityInfo = super.buildActivityInfo(activityData);
        info.startTime = activityData.getTimeStart();
        info.endTime = activityData.getTimeEnd();
        info.resetRemainTime = TimeHelper.getNextDayRemainTime();
        info.watchCount = watchCount;
        info.dailyLimit = getDailyLimit(cfgMap);
        info.completed = watchCount >= info.dailyLimit;
        info.detailInfos = buildDetails(activityData, cfgMap, claimedData, day, watchCount);
        return info;
    }

    private List<AdsRewardDetailInfo> buildDetails(ActivityData activityData, Map<Integer, VideoRewardCfg> cfgMap,
                                                    Map<Integer, PlayerActivityData> claimedData,
                                                    int day, int watchCount) {
        List<AdsRewardDetailInfo> details = new ArrayList<>(cfgMap.size());
        for (VideoRewardCfg cfg : cfgMap.values()) {
            details.add(buildDetail(activityData, cfg, claimedData.get(cfg.getId()), day, watchCount));
        }
        return details;
    }

    private AdsRewardDetailInfo buildDetail(ActivityData activityData, VideoRewardCfg cfg,
                                             PlayerActivityData data, int day, int watchCount) {
        AdsRewardDetailInfo info = new AdsRewardDetailInfo();
        info.activityId = activityData.getId();
        info.detailId = cfg.getId();
        info.requiredCount = cfg.getCount();
        info.rewardItems = ItemUtils.buildItemInfo(toRewards(cfg));
        if (isClaimed(data, day)) {
            info.claimStatus = ActivityConstant.ClaimStatus.CLAIMED;
        } else if (watchCount >= cfg.getCount()) {
            info.claimStatus = ActivityConstant.ClaimStatus.CAN_CLAIM;
        }
        return info;
    }

    private Map<Integer, PlayerActivityData> getTodayClaimedData(long playerId, ActivityData activityData, int day) {
        Map<Integer, PlayerActivityData> stored = playerActivityDao.getPlayerActivityData(
                playerId, activityData.getType(), activityData.getId());
        if (CollectionUtil.isEmpty(stored)) {
            return new HashMap<>();
        }
        Map<Integer, PlayerActivityData> todayData = new HashMap<>(stored.size());
        for (Map.Entry<Integer, PlayerActivityData> entry : stored.entrySet()) {
            if (isClaimed(entry.getValue(), day)) {
                todayData.put(entry.getKey(), entry.getValue());
            }
        }
        return todayData;
    }

    private int getWatchCount(long playerId, ActivityData activityData, int day) {
        return countDao.getCount(getCountFeature(activityData, day), String.valueOf(playerId)).intValue();
    }

    private String getCountFeature(ActivityData activityData, int day) {
        return "adsReward:%d:%d".formatted(activityData.getId(), day);
    }

    private int getDailyLimit(Map<Integer, VideoRewardCfg> cfgMap) {
        int max = 0;
        for (VideoRewardCfg cfg : cfgMap.values()) {
            max = Math.max(max, cfg.getCount());
        }
        return max;
    }

    private boolean validConfigs(ActivityData activityData, Map<Integer, VideoRewardCfg> cfgMap) {
        if (activityData == null || CollectionUtil.isEmpty(activityData.getValue())
                || cfgMap.size() != activityData.getValue().size()) {
            return false;
        }
        for (VideoRewardCfg cfg : cfgMap.values()) {
            if (cfg.getCount() <= 0 || !validRewards(cfg.getReward())) {
                return false;
            }
        }
        return true;
    }

    private boolean validRewards(Map<Integer, Integer> rewards) {
        if (CollectionUtil.isEmpty(rewards)) {
            return false;
        }
        for (Map.Entry<Integer, Integer> reward : rewards.entrySet()) {
            if (reward.getKey() == null || reward.getKey() <= 0
                    || reward.getValue() == null || reward.getValue() <= 0) {
                return false;
            }
        }
        return true;
    }

    private Map<Integer, Long> toRewards(VideoRewardCfg cfg) {
        Map<Integer, Long> rewards = new LinkedHashMap<>(cfg.getReward().size());
        cfg.getReward().forEach((itemId, count) -> rewards.put(itemId, count.longValue()));
        return rewards;
    }

    private boolean isClaimed(PlayerActivityData data, int day) {
        return data != null && data.getRound() == day
                && data.getClaimStatus() == ActivityConstant.ClaimStatus.CLAIMED;
    }
}
