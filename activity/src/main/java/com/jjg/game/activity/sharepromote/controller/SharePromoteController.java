package com.jjg.game.activity.sharepromote.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.ActivityBuilder;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.sharepromote.dao.SharePromoteDao;
import com.jjg.game.activity.sharepromote.data.SharePromotePlayerData;
import com.jjg.game.activity.sharepromote.message.bean.*;
import com.jjg.game.activity.sharepromote.message.req.*;
import com.jjg.game.activity.sharepromote.message.res.*;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.PrivilegeCardCfg;
import com.jjg.game.sampledata.bean.SharePromoteCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/9/3 17:43
 */
@Component
public class SharePromoteController extends BaseActivityController {
    private final Logger log = LoggerFactory.getLogger(SharePromoteController.class);
    private final SharePromoteDao sharePromoteDao;
    private final MailService mailService;

    public SharePromoteController(SharePromoteDao sharePromoteDao, MailService mailService) {
        this.sharePromoteDao = sharePromoteDao;
        this.mailService = mailService;
    }

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
            CommonResult<ItemOperationResult> addedItems;
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
        ResSharePromoteTypeInfo typeInfo = new ResSharePromoteTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return typeInfo;
        }
        typeInfo.activityData = new ArrayList<>();
        for (List<BaseActivityDetailInfo> baseActivityDetailInfos : allDetailInfo.values()) {
            SharePromoteActivityInfo detailInfos = new SharePromoteActivityInfo();
            detailInfos.detailInfos = new ArrayList<>();
            typeInfo.activityData.add(detailInfos);
            for (BaseActivityDetailInfo baseActivityDetailInfo : baseActivityDetailInfos) {
                if (baseActivityDetailInfo instanceof SharePromoteDetailInfo info) {
                    detailInfos.detailInfos.add(info);
                }
            }
            typeInfo.getProfitReward = sharePromoteDao.getPlayerIncome(playerId);
        }
        return typeInfo;
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

    public AbstractResponse reqSharePromoteBindPlayer(PlayerController playerController, ReqSharePromoteBindPlayer req) {
        ResSharePromoteBindPlayer resSharePromoteBindPlayer = new ResSharePromoteBindPlayer(Code.SUCCESS);
        resSharePromoteBindPlayer.code = sharePromoteDao.bindPlayer(playerController.playerId(), req.invitationCode);
        return resSharePromoteBindPlayer;
    }

    public AbstractResponse reqSharePromoteClaimProfitReward(PlayerController playerController, ReqSharePromoteClaimProfitReward req) {
        ResSharePromoteClaimProfitReward res = new ResSharePromoteClaimProfitReward(Code.SUCCESS);
        long playerId = playerController.playerId();
        long playerIncome = sharePromoteDao.getPlayerIncome(playerId);
        SharePromotePlayerData playerInfoData = sharePromoteDao.getPlayerInfoData(playerId);
        if (playerIncome > 0 && playerInfoData != null) {
            String lock = sharePromoteDao.getLock(playerId);
            redisLock.lock(lock);
            int goldItemId = ItemUtils.getGoldItemId();
            try {
                playerIncome = sharePromoteDao.getPlayerIncome(playerId);
                if (playerIncome <= 0) {
                    return res;
                }
                //删除记录
                sharePromoteDao.delPlayerIncome(playerIncome);
                //发放奖励
                CommonResult<ItemOperationResult> addedItem = playerPackService.addItem(playerId, goldItemId, playerIncome, "sharePromoteClaimProfitReward");
                if (!addedItem.success()) {
                    log.error("玩家领取收益时方法奖励失败 playerId:{} playerIncome:{}", playerId, playerIncome);
                }
                //添加领取记录
                playerInfoData = sharePromoteDao.getPlayerInfoData(playerId);
                playerInfoData.getHistory().add(getRecord(playerIncome));
                sharePromoteDao.savePlayerInfoData(playerId, playerInfoData);
                //更新排行榜
                sharePromoteDao.updateRankScore(playerId, playerIncome);
            } catch (Exception e) {
                log.error("玩家领取收益奖励异常 playerId={}", playerId, e);
                res.code = Code.EXCEPTION;
            } finally {
                redisLock.unlock(lock);
            }
            res.itemInfo = ItemUtils.buildItemInfo(goldItemId, playerIncome);
        }
        return res;
    }

    private String getRecord(long value) {
        return String.format("%d_%d", value, System.currentTimeMillis());
    }

    public AbstractResponse reqSharePromoteGlobalInfo(PlayerController playerController, ReqSharePromoteGlobalInfo req) {
        ResSharePromoteGlobalInfo res = new ResSharePromoteGlobalInfo(Code.SUCCESS);
        res.activityId = req.activityId;
        long playerId = playerController.playerId();
        res.earningsRatio = getPlayerProportion(playerId, req.activityId);
        res.yesterdayIncome = sharePromoteDao.getYesterdayIncome(playerId);
        res.historyIncome = sharePromoteDao.getPlayerHistoryIncome(playerId);
        SharePromotePlayerData playerInfoData = sharePromoteDao.getPlayerInfoData(playerId);
        if (playerInfoData == null) {
            String lock = sharePromoteDao.getLock(playerId);
            redisLock.lock(lock);
            try {
                playerInfoData = sharePromoteDao.getPlayerInfoData(playerId);
                if (playerInfoData == null) {
                    playerInfoData = new SharePromotePlayerData();
                    playerInfoData.setCode(sharePromoteDao.generateUniqueCode(playerId));
                }
                sharePromoteDao.savePlayerInfoData(playerId, playerInfoData);
            } catch (Exception e) {
                log.error("创建玩家推广分享数据失败 playerId={}", playerId, e);
            } finally {
                redisLock.unlock(lock);
            }
        }
        if (playerInfoData != null) {
            res.sharePlayerNum = playerInfoData.getBindCount();
            res.invitationCode = playerInfoData.getCode();
            List<String> history = playerInfoData.getHistory();
            if (CollectionUtil.isNotEmpty(history)) {
                res.recodes = new ArrayList<>(history.size());
                for (String record : history) {
                    String[] split = StringUtils.split(record, "_");
                    if (split.length != 2) {
                        continue;
                    }
                    SharePromoteRewardsRecode recode = new SharePromoteRewardsRecode();
                    recode.getNum = split[0];
                    recode.getTime = split[1];
                    res.recodes.add(recode);
                }
            }

        }
        return res;
    }

    /**
     * 获取玩家收益
     *
     * @param playerId   玩家id
     * @param activityId 活动id
     * @return 玩家总收益率
     */
    private int getPlayerProportion(long playerId, long activityId) {
        long bindCount = sharePromoteDao.getBindCount(playerId);
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        int maxProportion = 0;
        for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
            if (cfgBean instanceof SharePromoteCfg cfg) {
                if (bindCount >= cfg.getCondition() && maxProportion < cfg.getProportion()) {
                    maxProportion = cfg.getProportion();
                }
            }
        }
        return maxProportion;
    }

    public AbstractResponse reqSharePromoteSelfRankInfo(PlayerController playerController, ReqSharePromoteSelfRankInfo req) {
        ResSharePromoteSelfRankInfo res = new ResSharePromoteSelfRankInfo(Code.SUCCESS);
        //玩家id 分数
        Pair<Map<Long, Double>, Boolean> playerIncomeRankPair = sharePromoteDao.getPlayerIncomeRank(playerController.playerId(), req.startIndex, Math.min(ActivityConstant.SharePromote.MAX_SIZE, req.startIndex + req.size));
        Map<Long, Double> playerIncomeRank = playerIncomeRankPair.getFirst();
        if (CollectionUtil.isNotEmpty(playerIncomeRank)) {
            res.rankInfoList = new ArrayList<>(playerIncomeRank.size());
            Map<Long, Player> playerMap = corePlayerService.multiGetPlayerMap(playerIncomeRank.keySet());
            for (Map.Entry<Long, Double> entry : playerIncomeRank.entrySet()) {
                Player player = playerMap.get(entry.getKey());
                if (player == null) {
                    continue;
                }
                SharePromoteSelfRankInfo rankInfo = new SharePromoteSelfRankInfo();
                buildSharePromoteRankInfo(player, rankInfo, entry.getValue().longValue());
                rankInfo.totalRecharge = 0;
                res.rankInfoList.add(rankInfo);
            }
            res.startIndex = req.startIndex;
            res.hasNext = playerIncomeRankPair.getSecond();
        }
        return res;
    }

    public AbstractResponse reqSharePromoteWeekRankInfo(PlayerController playerController, ReqSharePromoteWeekRankInfo req) {
        ResSharePromoteWeekRankInfo res = new ResSharePromoteWeekRankInfo(Code.SUCCESS);
        //玩家id 分数
        Pair<Map<Long, Double>, Boolean> playerIncomeRankPair = sharePromoteDao.getPlayerIncomeRank(req.startIndex, Math.min(ActivityConstant.SharePromote.MAX_SIZE, req.startIndex + req.size));
        Map<Long, Double> playerIncomeRank = playerIncomeRankPair.getFirst();
        if (CollectionUtil.isNotEmpty(playerIncomeRank)) {
            res.rankInfoList = new ArrayList<>(playerIncomeRank.size());
            Map<Long, Player> playerMap = corePlayerService.multiGetPlayerMap(playerIncomeRank.keySet());
            for (Map.Entry<Long, Double> entry : playerIncomeRank.entrySet()) {
                Player player = playerMap.get(entry.getKey());
                if (player == null) {
                    continue;
                }
                SharePromoteWeekRankInfo rankInfo = new SharePromoteWeekRankInfo();
                buildSharePromoteRankInfo(player, rankInfo, entry.getValue().longValue());
                res.rankInfoList.add(rankInfo);
            }
            res.startIndex = req.startIndex;
            res.hasNext = playerIncomeRankPair.getSecond();
        }
        res.remainTime = TimeHelper.getNextWeekdayEnd(DayOfWeek.MONDAY) - System.currentTimeMillis();
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(req.activityId);
        if (CollectionUtil.isNotEmpty(baseCfgBeanMap)) {
            res.rankRewardsInfoList = new ArrayList<>();
            for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                if (cfgBean instanceof SharePromoteCfg cfg && cfg.getType() == 2) {
                    SharePromoteRankRewardsInfo rewardsInfo = new SharePromoteRankRewardsInfo();
                    if (CollectionUtil.isNotEmpty(cfg.getRanking())) {
                        rewardsInfo.minRank = cfg.getRanking().getFirst();
                        rewardsInfo.maxRank = cfg.getRanking().getLast();
                    }
                    rewardsInfo.itemInfos = ItemUtils.buildItemInfo(cfg.getGetitem());
                    res.rankRewardsInfoList.add(rewardsInfo);
                }
            }
        }
        return res;
    }


    /**
     * 构建分享推广排行基本信息
     */
    public void buildSharePromoteRankInfo(Player player, SharePromoteRankInfo rankInfo, long totalScore) {
        rankInfo.headFrameId = player.getHeadFrameId();
        rankInfo.headImgId = player.getHeadImgId();
        rankInfo.titleId = player.getTitleId();
        rankInfo.level = player.getLevel();
        rankInfo.nickname = player.getNickName();
        rankInfo.nationalId = player.getNationalId();
    }

    @Scheduled(cron = "0 0 0 * * MON")
    public void sendRankRewards() {
        if (activityManager.isExecutionNode()) {
            Map<Long, ActivityData> dataMap = activityManager.getActivityTypeData().get(ActivityType.SHARE_PROMOTE);
            if (CollectionUtil.isEmpty(dataMap)) {
                return;
            }
            List<ActivityData> data = dataMap.values().stream().filter(ActivityData::canRun).toList();
            if (data.size() != 1) {
                log.error("存在多个开启的推广分享活动 size:{}", dataMap.size());
            }
            ActivityData activityData = data.getFirst();
            Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
            List<Pair<SharePromoteCfg, Pair<Integer, Integer>>> rankRewardPair = new ArrayList<>();
            if (CollectionUtil.isNotEmpty(baseCfgBeanMap)) {
                for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                    if (cfgBean instanceof SharePromoteCfg cfg && cfg.getType() == 2) {
                        rankRewardPair.add(Pair.newPair(cfg, new Pair<>(cfg.getRanking().getFirst(), cfg.getRanking().getLast())));
                    }
                }
            }
            rankRewardPair.sort(Comparator.comparingInt(p -> p.getSecond().getFirst()));
            //发送排行榜奖励
            Pair<Map<Long, Double>, Boolean> playerIncomeRank = sharePromoteDao.getPlayerIncomeRank(0, 100);
            if (CollectionUtil.isNotEmpty(playerIncomeRank.getFirst())) {
                int i = 1;
                for (Long playerId : playerIncomeRank.getFirst().keySet()) {
                    mailService.addCfgMail(playerId, 1, ItemUtils.buildItems(getRankRewards(rankRewardPair, i++)));
                }
            }
            log.info("推广分享周榜奖励发放完成 总发放人数 num:{}", playerIncomeRank.getFirst().size());
        }
    }


    public Map<Integer, Long> getRankRewards(List<Pair<SharePromoteCfg, Pair<Integer, Integer>>> rewards, int rank) {
        for (Pair<SharePromoteCfg, Pair<Integer, Integer>> reward : rewards) {
            Pair<Integer, Integer> rankLimit = reward.getSecond();
            SharePromoteCfg cfg = reward.getFirst();
            if (rank > rankLimit.getFirst()) {
                continue;
            }
            if (rank < rankLimit.getSecond()) {
                return cfg.getGetitem();
            }
        }
        return Map.of();
    }
}
