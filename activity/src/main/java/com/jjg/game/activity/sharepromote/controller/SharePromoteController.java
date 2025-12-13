package com.jjg.game.activity.sharepromote.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.activitylog.data.SharePromoteWeekRank;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.ClaimRewardsResult;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.sharepromote.dao.SharePromoteDao;
import com.jjg.game.activity.sharepromote.data.SharePromotePlayerData;
import com.jjg.game.activity.sharepromote.message.bean.*;
import com.jjg.game.activity.sharepromote.message.req.ReqSharePromoteBindPlayer;
import com.jjg.game.activity.sharepromote.message.req.ReqSharePromoteSelfRankInfo;
import com.jjg.game.activity.sharepromote.message.req.ReqSharePromoteWeekRankInfo;
import com.jjg.game.activity.sharepromote.message.res.*;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.core.utils.RedisUtils;
import com.jjg.game.core.utils.TipUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.SharePromoteCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/9/3 17:43
 */
@Component
public class SharePromoteController extends BaseActivityController {
    private final Logger log = LoggerFactory.getLogger(SharePromoteController.class);
    private final SharePromoteDao sharePromoteDao;
    private final MailService mailService;
    private final CountDao countDao;

    public SharePromoteController(SharePromoteDao sharePromoteDao, MailService mailService, CountDao countDao) {
        this.sharePromoteDao = sharePromoteDao;
        this.mailService = mailService;
        this.countDao = countDao;
    }

    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        return null;
    }

    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        ResSharePromoteClaimRewards res = new ResSharePromoteClaimRewards(Code.SUCCESS);
        long playerId = player.getId();
        //获取活动详情数据
        Map<Integer, SharePromoteCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        SharePromoteCfg cfg = baseCfgBeanMap.get(detailId);
        if (cfg == null || CollectionUtil.isEmpty(cfg.getGetitem())) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        ClaimRewardsResult claimRewardsResult = claimActivityRewards(playerId, activityData, detailId, AddType.ACTIVITY_SHARE_PROMOTE, cfg.getGetitem());
        if (claimRewardsResult != null) {
            //发送日志
            Long add = cfg.getGetitem().getOrDefault(ItemUtils.getGoldItemId(), 0L);
            if (add > 0) {
                activityLogger.sendSharePromoteAddRewards(player, activityData, 0, 4, 0, 0, add, 0,
                        claimRewardsResult.itemOperationResult().getGoldNum(), 0);
            }
            res.infoList = ItemUtils.buildItemInfo(cfg.getGetitem());
            res.detailInfo = buildPlayerActivityDetail(player, activityData, cfg, claimRewardsResult.playerActivityData());
            SharePromotePlayerData playerInfoData = sharePromoteDao.getPlayerInfoData(playerId);
            addRecord(playerInfoData, add);
            sharePromoteDao.savePlayerInfoData(playerId, playerInfoData);
            sharePromoteDao.addPlayerIncome(playerId, add);
        }
        return res;
    }

    @Override
    public boolean addPlayerProgress(Player player, ActivityData activityData, long progress, long activityTargetKey, Object additionalParameters) {
        //这个活动不主动刷新
        long playerId = player.getId();
        // 获取playerId绑定的玩家信息
        String bindInfo = sharePromoteDao.getBindInfo(playerId);
        if (StringUtils.isEmpty(bindInfo)) {
            return false;
        }
        String[] bindInfoArr = StringUtils.split(bindInfo, "_");
        if (bindInfoArr.length != 2) {
            return false;
        }
        //被绑定的玩家id
        long beneficiaryPlayerId = Long.parseLong(bindInfoArr[0]);
        //计算收益率
        int proportion = getPlayerProportion(beneficiaryPlayerId, activityData);
        if (proportion == 0) {
            return false;
        }
        //获取被绑定玩家的推广分享数据
        SharePromotePlayerData playerInfoData = sharePromoteDao.getPlayerInfoData(beneficiaryPlayerId);
        if (playerInfoData == null) {
            return false;
        }
        countDao.incrBy(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted("sharePromote"), String.valueOf(playerId), RedisUtils.fromLong(progress));
        BigDecimal magnification = BigDecimal.ONE;
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(53);
        if (globalConfigCfg != null) {
            magnification = BigDecimal.valueOf(globalConfigCfg.getIntValue());
        }
        //计算本次添加的进度
        long addValue = RedisUtils.fromLong(progress)
                .multiply(BigDecimal.valueOf(proportion))
                .multiply(magnification)
                .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN)
                .longValue();
        if (addValue > 0) {
            //添加充值计数
            sharePromoteDao.addPlayerIncome(playerId, beneficiaryPlayerId, addValue);
            Player beneficiaryPlayer = corePlayerService.get(beneficiaryPlayerId);
            //发送日志
            activityLogger.sendSharePromoteAddRewards(beneficiaryPlayer, activityData, playerId, 1,
                    addValue, 0, progress, 0);
            activityLogger.sendSharePromoteSubordinateRecharge(player, activityData, beneficiaryPlayerId, progress, addValue);
        }
        return false;
    }

    @Override
    public AbstractResponse getPlayerActivityDetail(Player player, ActivityData activityData, int detailId) {
        long activityId = activityData.getId();
        ResSharePromoteDetailInfo detailInfo = new ResSharePromoteDetailInfo(Code.SUCCESS);
        Map<Integer, SharePromoteCfg> baseCfgBeanMap = getDetailCfgBean(activityData);

        Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(player.getId(), activityData.getType(), activityId);
        detailInfo.detailInfo = new ArrayList<>();
        detailInfo.detailInfo.add(buildPlayerActivityDetail(player, activityData, baseCfgBeanMap.get(detailId), playerActivityData.get(detailId)));
        return detailInfo;
    }

    @Override
    public SharePromoteDetailInfo buildPlayerActivityDetail(Player player, ActivityData activityData, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (baseCfgBean instanceof SharePromoteCfg cfg && cfg.getType() != ActivityConstant.SharePromote.RANK_REWARDS) {
            SharePromoteDetailInfo info = new SharePromoteDetailInfo();
            info.activityId = activityData.getId();
            info.detailId = cfg.getId();
            info.needNum = cfg.getCondition();
            info.proportion = cfg.getProportion();
            //奖励信息
            info.rewardItems = ItemUtils.buildItemInfo(cfg.getGetitem());
            if (data != null) {
                //领奖状态
                info.claimStatus = data.getClaimStatus();
            }
            return info;
        }
        return null;
    }

    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(Player player, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResSharePromoteTypeInfo typeInfo = new ResSharePromoteTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return typeInfo;
        }
        for (List<BaseActivityDetailInfo> baseActivityDetailInfos : allDetailInfo.values()) {
            SharePromoteActivityInfo detailInfos = new SharePromoteActivityInfo();
            detailInfos.detailInfos = new ArrayList<>();
            typeInfo.activityData = detailInfos;
            for (BaseActivityDetailInfo baseActivityDetailInfo : baseActivityDetailInfos) {
                if (baseActivityDetailInfo instanceof SharePromoteDetailInfo info) {
                    detailInfos.detailInfos.add(info);
                }
            }
            SharePromotePlayerData playerInfoData = sharePromoteDao.getPlayerInfoData(player.getId());
            if (playerInfoData != null) {
                Map<Long, Long> notClaimedPlayerIds = playerInfoData.getNotClaimedPlayerIds();
                if (CollectionUtil.isNotEmpty(notClaimedPlayerIds)) {
                    detailInfos.bindPlayerInfos = new ArrayList<>();
                    List<Player> players = corePlayerService.multiGetPlayer(notClaimedPlayerIds.keySet());
                    for (Player oldPlayer : players) {
                        SharePromoteBindPlayerInfo playerInfo = new SharePromoteBindPlayerInfo();
                        playerInfo.headImgId = oldPlayer.getHeadImgId();
                        playerInfo.headFrameId = oldPlayer.getHeadFrameId();
                        playerInfo.level = oldPlayer.getLevel();
                        playerInfo.nickname = oldPlayer.getNickName();
                        playerInfo.invitationTime = notClaimedPlayerIds.getOrDefault(oldPlayer.getId(), 0L);
                        detailInfos.bindPlayerInfos.add(playerInfo);
                    }
                }
            }
            detailInfos.progress = sharePromoteDao.getBindCount(player.getId());
        }
        return typeInfo;
    }

    @Override
    public Map<Integer, SharePromoteCfg> getDetailCfgBean(ActivityData activityData) {
        return GameDataManager.getSharePromoteCfgList()
                .stream()
                .filter(cfg -> activityData.getValue().contains(cfg.getId()))
                .collect(Collectors.toMap(BaseCfgBean::getId, cfg -> cfg));
    }

    /**
     * 请求绑定玩家
     *
     * @param playerController 玩家控制器
     * @param activityData     活动数据
     * @param req              请求
     * @return 响应
     */
    public AbstractResponse reqSharePromoteBindPlayer(PlayerController playerController, ActivityData activityData, ReqSharePromoteBindPlayer req) {
        ResSharePromoteBindPlayer res = new ResSharePromoteBindPlayer(Code.SUCCESS);
        long playerId = playerController.playerId();
        //多次输入错误邀请码判断
        int playerCodeErrorPrint = sharePromoteDao.getPlayerCodeErrorPrint(playerId);
        if (playerCodeErrorPrint >= ActivityConstant.SharePromote.MAX_ERROR_TIMES) {
            TipUtils.sendToastTip(playerId, 62044);
            return null;
        }
        //获取玩家的推广分享数据
        SharePromotePlayerData playerInfoData = sharePromoteDao.getPlayerInfoData(playerId);
        if (playerInfoData == null) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        if (StringUtils.isEmpty(req.invitationCode)) {
            res.code = Code.CODE_ERROR;
            return res;
        }
        //绑定玩家
        CommonResult<Long> result = sharePromoteDao.bindPlayer(playerId, req.invitationCode);
        res.code = result.code;
        //绑定之前的收益率
        int bindBefore = getPlayerProportion(playerId, activityData);
        if (res.code == Code.SUCCESS) {
            //修改玩家数据
            String lock = sharePromoteDao.getLock(playerId);
            boolean save = false;
            boolean isLock = false;
            try {
                isLock = redisLock.tryLockWithDefaultTime(lock);
                if (!isLock) {
                    res.code = Code.FAIL;
                    log.error("获取锁失败 lockKey:{} playerId:{} activityId:{} invitationCode:{} ", lock, playerId, activityData.getId(), req.invitationCode);
                    return res;
                }
                playerInfoData = sharePromoteDao.getPlayerInfoData(playerId);
                playerInfoData.setBindCount(playerInfoData.getBindCount() + 1);
                if (playerInfoData.getNotClaimedPlayerIds() == null) {
                    playerInfoData.setNotClaimedPlayerIds(new HashMap<>());
                }
                playerInfoData.getNotClaimedPlayerIds().put(result.data, System.currentTimeMillis());
                sharePromoteDao.savePlayerInfoData(playerId, playerInfoData);
                save = true;
            } catch (Exception e) {
                log.error("推广分享绑定成功 修改数据异常 playerId:{} code:{}", playerId, req.invitationCode);
            } finally {
                if (isLock) {
                    redisLock.tryUnlock(lock);
                }
            }
            if (save) {
                //发送日志
                activityLogger.sendSharePromoteAddRewards(playerController.getPlayer(), activityData, result.data, 2,
                        0, 1, 0, 0, 0, 1);
                //修改活动状态
                checkActivityStatus(playerController.getPlayer(), activityData, playerInfoData.getBindCount(), bindBefore);
            }
            res.bindNum = playerInfoData.getBindCount();
        }
        return res;
    }

    public void checkActivityStatus(Player player, ActivityData activityData, int bindNum, int bindBefore) {
        Map<Long, ActivityData> longActivityDataMap = activityManager.getActivityTypeData().get(ActivityType.SHARE_PROMOTE);
        if (CollectionUtil.isEmpty(longActivityDataMap)) {
            return;
        }
        ActivityData data = longActivityDataMap.values().iterator().next();
        Map<Integer, SharePromoteCfg> beanMap = getDetailCfgBean(data);
        if (CollectionUtil.isEmpty(beanMap)) {
            return;
        }
        boolean changed = false;
        long playerId = player.getId();
        String lockKey = playerActivityDao.getLockKey(playerId, data.getId());
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(lockKey);
            if (!lock) {
                log.error("获取锁失败 lockKey:{} playerId:{} activityId:{} bindNum:{} bindBefore:{}", lockKey, playerId, activityData.getId(), bindNum, bindBefore);
                return;
            }
            Map<Integer, PlayerActivityData> playerActivityDataMap = playerActivityDao.getPlayerActivityData(playerId, data.getType(), data.getId());
            for (SharePromoteCfg cfg : beanMap.values()) {
                if (cfg.getType() != ActivityConstant.SharePromote.RANK_REWARDS && cfg.getCondition() <= bindNum) {
                    if (playerActivityDataMap.containsKey(cfg.getId())) {
                        continue;
                    }
                    PlayerActivityData playerActivityData = new PlayerActivityData(data.getId(), data.getRound());
                    playerActivityData.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
                    playerActivityDataMap.put(cfg.getId(), playerActivityData);
                    changed = true;
                    //发送日志
                    Long addValue = cfg.getGetitem().get(ItemUtils.getGoldItemId());
                    if (addValue > 0) {
                        activityLogger.sendSharePromoteAddRewards(player, activityData, 0, 5, addValue, 0, 0, getPlayerProportion(playerId, activityData) - bindBefore);
                    }
                }
            }
            if (changed) {
                playerActivityDao.savePlayerActivityData(playerId, data.getType(), data.getId(), playerActivityDataMap);
            }
        } catch (Exception e) {
            log.error("推广分享绑定玩家成功 检查玩家进度异常 playerId:{}", playerId, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(lockKey);
            }
        }
    }

    /**
     * 请求领取收益奖励
     *
     * @param playerController 玩家控制器
     * @param activityData     活动数据
     * @return 响应
     */
    public AbstractResponse reqSharePromoteClaimProfitReward(PlayerController playerController, ActivityData activityData) {
        ResSharePromoteClaimProfitReward res = new ResSharePromoteClaimProfitReward(Code.SUCCESS);
        long playerId = playerController.playerId();
        //获取玩家收益
        long playerIncome = sharePromoteDao.getPlayerIncome(playerId);
        //获取玩家推广分享数据
        SharePromotePlayerData playerInfoData = sharePromoteDao.getPlayerInfoData(playerId);
        if (playerIncome > 0 && playerInfoData != null) {
            String lock = sharePromoteDao.getLock(playerId);
            int goldItemId = ItemUtils.getGoldItemId();
            boolean isLock = false;
            try {
                isLock = redisLock.tryLockWithDefaultTime(lock);
                if (!isLock) {
                    log.error("获取锁失败 lockKey:{} playerId:{} activityId:{}", lock, playerId, activityData.getId());
                    res.code = Code.FAIL;
                    return res;
                }
                playerIncome = sharePromoteDao.getPlayerIncome(playerId);
                if (playerIncome <= 0) {
                    return res;
                }
                //删除记录
                sharePromoteDao.delPlayerIncome(playerId);
                //发放奖励
                CommonResult<ItemOperationResult> addedItem = playerPackService.addItem(playerId, goldItemId, playerIncome, AddType.ACTIVITY_SHARE_PROMOTE_REWARDS);
                if (!addedItem.success()) {
                    log.error("玩家领取收益时方法奖励失败 playerId:{} playerIncome:{}", playerId, playerIncome);
                }
                //添加领取记录
                playerInfoData = sharePromoteDao.getPlayerInfoData(playerId);
                //添加记录
                addRecord(playerInfoData, playerIncome);
                //回存玩家推广分享数据
                sharePromoteDao.savePlayerInfoData(playerId, playerInfoData);
                //更新排行榜
                sharePromoteDao.updateRankScore(playerId, playerIncome);
                sharePromoteDao.addPlayerIncome(playerId, playerIncome);
                if (addedItem.data != null) {
                    //添加日志
                    activityLogger.sendSharePromoteAddRewards(playerController.getPlayer(), activityData, 0, 3, 0, 0, playerIncome,
                            0, addedItem.data.getGoldNum(), 0);
                }
            } catch (Exception e) {
                log.error("玩家领取收益奖励异常 playerId={}", playerId, e);
                res.code = Code.EXCEPTION;
            } finally {
                if (isLock) {
                    redisLock.tryUnlock(lock);
                }
            }
            res.itemInfo = ItemUtils.buildItemInfo(goldItemId, playerIncome);
            res.recodes = buildRecords(playerInfoData.getHistory());
        }
        return res;
    }

    /**
     * 添加记录
     *
     * @param playerInfoData 推广分享玩家数据
     * @param playerIncome   本次收入
     */
    private void addRecord(SharePromotePlayerData playerInfoData, long playerIncome) {
        if (playerInfoData.getHistory() == null) {
            playerInfoData.setHistory(new ArrayList<>());
        }
        //达到记录上限移除第一个
        if (playerInfoData.getHistory().size() >= ActivityConstant.SharePromote.MAX_RECODE_NUM) {
            playerInfoData.getHistory().removeFirst();
        }
        playerInfoData.getHistory().add(getRecord(playerIncome));
    }

    /**
     * 获取记录
     *
     * @param value 记录值
     * @return 包含时间戳的记录值
     */
    private String getRecord(long value) {
        return String.format("%d_%d", value, System.currentTimeMillis());
    }

    /**
     * 请求推广分享总览数据
     */
    public AbstractResponse reqSharePromoteGlobalInfo(PlayerController playerController, ActivityData data) {
        ResSharePromoteGlobalInfo res = new ResSharePromoteGlobalInfo(Code.SUCCESS);
        long playerId = playerController.playerId();
        res.earningsRatio = getPlayerProportion(playerId, data);
        res.yesterdayIncome = sharePromoteDao.getYesterdayIncome(playerId);
        res.historyIncome = sharePromoteDao.getPlayerHistoryIncome(playerId);
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(63);
        if (globalConfigCfg != null) {
            res.QrCode = globalConfigCfg.getValue();
        }
        SharePromotePlayerData playerInfoData = sharePromoteDao.getPlayerInfoData(playerId);
        //玩家数据为null时初始化一个
        if (playerInfoData == null) {
            String lockKey = sharePromoteDao.getLock(playerId);
            boolean lock = false;
            try {
                lock = redisLock.tryLockWithDefaultTime(lockKey);
                if (!lock) {
                    res.code = Code.FAIL;
                    log.error("获取锁失败 lockKey:{} playerId:{} activityId:{}", lockKey, playerId, data.getId());
                    return res;
                }
                playerInfoData = sharePromoteDao.getPlayerInfoData(playerId);
                if (playerInfoData == null) {
                    playerInfoData = new SharePromotePlayerData();
                    playerInfoData.setCode(sharePromoteDao.generateUniqueCode(playerId));
                }
                sharePromoteDao.savePlayerInfoData(playerId, playerInfoData);
            } catch (Exception e) {
                log.error("创建玩家推广分享数据失败 playerId={}", playerId, e);
            } finally {
                if (lock) {
                    redisLock.tryUnlock(lockKey);
                }
            }
        }
        if (playerInfoData != null) {
            res.sharePlayerNum = playerInfoData.getBindCount();
            res.invitationCode = playerInfoData.getCode();
            res.getProfitReward = sharePromoteDao.getPlayerIncome(playerId);
            List<String> history = playerInfoData.getHistory();
            res.recodes = buildRecords(history);

        }
        return res;
    }

    /**
     * 构建记录信息
     *
     * @param history 历史信息
     */
    private List<SharePromoteRewardsRecode> buildRecords(List<String> history) {
        if (CollectionUtil.isNotEmpty(history)) {
            List<SharePromoteRewardsRecode> list = new ArrayList<>(history.size());
            //解析记录信息
            for (String record : history) {
                String[] split = StringUtils.split(record, "_");
                if (split.length != 2) {
                    continue;
                }
                SharePromoteRewardsRecode recode = new SharePromoteRewardsRecode();
                recode.getNum = Integer.parseInt(split[0]);
                recode.getTime = Long.parseLong(split[1]);
                list.add(recode);
            }
            return list;
        }
        return List.of();
    }

    /**
     * 获取玩家收益
     *
     * @param playerId     玩家id
     * @param activityData 活动数据
     * @return 玩家总收益率
     */
    private int getPlayerProportion(long playerId, ActivityData activityData) {
        long bindCount = sharePromoteDao.getBindCount(playerId);
        Map<Integer, SharePromoteCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        int maxProportion = 0;
        if (CollectionUtil.isEmpty(baseCfgBeanMap)) {
            return 0;
        }
        for (SharePromoteCfg cfg : baseCfgBeanMap.values()) {
            if (bindCount >= cfg.getCondition() && maxProportion < cfg.getProportion()) {
                maxProportion = cfg.getProportion();
            }
        }
        return maxProportion;
    }


    /**
     * 请求推广分享旗下贡献排行榜
     */
    public AbstractResponse reqSharePromoteSelfRankInfo(PlayerController playerController, ReqSharePromoteSelfRankInfo req) {
        ResSharePromoteSelfRankInfo res = new ResSharePromoteSelfRankInfo(Code.SUCCESS);
        //玩家id->分数,是否还有数据
        Pair<Map<Long, Double>, Boolean> playerIncomeRankPair = sharePromoteDao.getAllIncomeRank(playerController.playerId(), req.startIndex, Math.min(ActivityConstant.SharePromote.MAX_SIZE, req.size));
        Map<Long, Double> playerIncomeRank = playerIncomeRankPair.getFirst();
        if (CollectionUtil.isNotEmpty(playerIncomeRank)) {
            res.rankInfoList = new ArrayList<>(playerIncomeRank.size());
            //获取排行榜玩家信息数据
            Map<Long, Player> playerMap = corePlayerService.multiGetPlayerMap(playerIncomeRank.keySet());
            Map<String, BigDecimal> counts = countDao.getCounts(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted("sharePromote"),
                    playerIncomeRank.keySet().stream().map(String::valueOf).toList());
            for (Map.Entry<Long, Double> entry : playerIncomeRank.entrySet()) {
                Player player = playerMap.get(entry.getKey());
                if (player == null) {
                    continue;
                }
                SharePromoteSelfRankInfo rankInfo = new SharePromoteSelfRankInfo();
                //构建排行榜玩家基本信息
                buildSharePromoteRankInfo(player, rankInfo, entry.getValue().longValue());
                rankInfo.totalRecharge = counts.get(String.valueOf(entry.getKey())).toPlainString();
                res.rankInfoList.add(rankInfo);
            }
            res.startIndex = req.startIndex;
            res.hasNext = playerIncomeRankPair.getSecond();
        }
        return res;
    }

    /**
     * 请求推广分享收益排行榜
     */
    public AbstractResponse reqSharePromoteWeekRankInfo(ActivityData data, ReqSharePromoteWeekRankInfo req) {
        ResSharePromoteWeekRankInfo res = new ResSharePromoteWeekRankInfo(Code.SUCCESS);
        //玩家id->分数,是否还有数据
        Pair<Map<Long, Double>, Boolean> playerIncomeRankPair = sharePromoteDao.getAllIncomeRank(req.startIndex, Math.min(ActivityConstant.SharePromote.MAX_SIZE, req.size));
        Map<Long, Double> playerIncomeRank = playerIncomeRankPair.getFirst();
        if (CollectionUtil.isNotEmpty(playerIncomeRank)) {
            res.rankInfoList = new ArrayList<>(playerIncomeRank.size());
            //奖励信息
            Map<Integer, SharePromoteCfg> beanMap = getDetailCfgBean(data);
            List<Pair<SharePromoteCfg, Pair<Integer, Integer>>> rewardPairList = getRewardPairList(beanMap);
            //排行信息
            int baseRank = req.startIndex;
            Map<Long, Player> playerMap = corePlayerService.multiGetPlayerMap(playerIncomeRank.keySet());
            for (Map.Entry<Long, Double> entry : playerIncomeRank.entrySet()) {
                baseRank++;
                Player player = playerMap.get(entry.getKey());
                if (player == null) {
                    continue;
                }
                SharePromoteWeekRankInfo rankInfo = new SharePromoteWeekRankInfo();
                //构建排行榜玩家基本信息
                buildSharePromoteRankInfo(player, rankInfo, entry.getValue().longValue());
                //构建奖励信息
                rankInfo.itemInfos = ItemUtils.buildItemInfo(getRankRewards(rewardPairList, baseRank));
                res.rankInfoList.add(rankInfo);
            }
            res.startIndex = req.startIndex;
            res.hasNext = playerIncomeRankPair.getSecond();
        }
        //计算剩余时间
        res.remainTime = TimeHelper.getNextWeekdayEnd(DayOfWeek.MONDAY) - System.currentTimeMillis();
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
        rankInfo.totalScore = totalScore;
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
            Map<Integer, SharePromoteCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
            List<Pair<SharePromoteCfg, Pair<Integer, Integer>>> rankRewardPair = getRewardPairList(baseCfgBeanMap);
            //发送排行榜奖励
            Pair<Map<Long, Double>, Boolean> playerIncomeRank = sharePromoteDao.getAllIncomeRank(0, 100);
            //取出数据后直接删除
            sharePromoteDao.deleteRank();
            if (CollectionUtil.isNotEmpty(playerIncomeRank.getFirst())) {
                //日志
                List<SharePromoteWeekRank> logList = new ArrayList<>(playerIncomeRank.getFirst().size());
                Map<Long, Player> playerMap = corePlayerService.multiGetPlayerMap(playerIncomeRank.getFirst().keySet());
                int i = 1;
                for (Map.Entry<Long, Double> entry : playerIncomeRank.getFirst().entrySet()) {
                    SharePromoteWeekRank rankInfo = new SharePromoteWeekRank();
                    Long playerId = entry.getKey();
                    rankInfo.setRank(i);
                    //邮件参数构建
                    List<LanguageParamData> params = new ArrayList<>(1);
                    params.add(new LanguageParamData(0, String.valueOf(i)));
                    //构建奖励
                    Map<Integer, Long> rankRewards = getRankRewards(rankRewardPair, i++);
                    List<Item> getItems = ItemUtils.buildItems(rankRewards);
                    //发送奖励邮件
                    mailService.addCfgMail(playerId, ActivityConstant.SharePromote.MAIL_ID, getItems, params);
                    Player player = playerMap.get(playerId);
                    if (player == null) {
                        continue;
                    }
                    rankInfo.setPlayerId(playerId);
                    rankInfo.setName(player.getNickName());
                    rankInfo.setTotalScore(entry.getValue().longValue());
                    rankInfo.setRewards(rankRewards);
                    logList.add(rankInfo);
                }
                activityLogger.sendSharePromoteRankRewards(activityData, logList);
            }
            log.info("推广分享周榜奖励发放完成 总发放人数 num:{}", playerIncomeRank.getFirst().size());
        }
    }

    /**
     * 获取奖励信息
     *
     * @param baseCfgBeanMap 配置信息
     * @return 奖励信息
     */
    private List<Pair<SharePromoteCfg, Pair<Integer, Integer>>> getRewardPairList(Map<Integer, SharePromoteCfg> baseCfgBeanMap) {
        List<Pair<SharePromoteCfg, Pair<Integer, Integer>>> rankRewardPair = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(baseCfgBeanMap)) {
            for (SharePromoteCfg cfg : baseCfgBeanMap.values()) {
                if (cfg.getType() == ActivityConstant.SharePromote.RANK_REWARDS) {
                    rankRewardPair.add(Pair.newPair(cfg, new Pair<>(cfg.getRanking().getFirst(), cfg.getRanking().getLast())));
                }
            }
        }
        rankRewardPair.sort(Comparator.comparingInt(p -> p.getSecond().getFirst()));
        return rankRewardPair;
    }

    /**
     * 获取rank奖励
     *
     * @param rewards 奖励名词 最大名称->最小名次
     * @param rank    排名
     * @return 奖励信息
     */
    public Map<Integer, Long> getRankRewards(List<Pair<SharePromoteCfg, Pair<Integer, Integer>>> rewards, int rank) {
        for (Pair<SharePromoteCfg, Pair<Integer, Integer>> reward : rewards) {
            Pair<Integer, Integer> rankLimit = reward.getSecond();
            SharePromoteCfg cfg = reward.getFirst();
            if (rank > rankLimit.getSecond()) {
                continue;
            }
            if (rank >= rankLimit.getFirst()) {
                return cfg.getGetitem();
            }
        }
        return Map.of();
    }

    /**
     * 请求领取绑定玩家奖励
     *
     * @param playerController 玩家控制器
     * @param activityData     活动数据
     */
    public AbstractResponse reqSharePromoteClaimBindRewards(PlayerController playerController, ActivityData activityData) {
        ResSharePromoteClaimBindRewards res = new ResSharePromoteClaimBindRewards(Code.SUCCESS);
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(62);
        if (globalConfigCfg == null || StringUtils.isEmpty(globalConfigCfg.getValue())) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        String[] rewardsCfg = StringUtils.split(globalConfigCfg.getValue(), "_");
        if (rewardsCfg.length != 2) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        int itemId = Integer.parseInt(rewardsCfg[0]);
        long count = Long.parseLong(rewardsCfg[1]);
        //获取玩家信息
        long playerId = playerController.playerId();
        SharePromotePlayerData playerInfoData;
        long totalRewards = 0;
        String lock = sharePromoteDao.getLock(playerId);
        boolean isLock = false;
        try {
            isLock = redisLock.tryLockWithDefaultTime(lock);
            if (!isLock) {
                res.code = Code.FAIL;
                log.error("获取锁失败 lockKey:{} playerId:{} activityId:{} ", lock, playerId, activityData.getId());
                return res;
            }
            playerInfoData = sharePromoteDao.getPlayerInfoData(playerId);
            if (playerInfoData == null || CollectionUtil.isEmpty(playerInfoData.getNotClaimedPlayerIds())) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            totalRewards = playerInfoData.getNotClaimedPlayerIds().size() * count;
            //清除数据
            playerInfoData.getNotClaimedPlayerIds().clear();
            //发送奖励
            CommonResult<ItemOperationResult> result = playerPackService.addItem(playerId, itemId, totalRewards, AddType.ACTIVITY_SHARE_PROMOTE_BIND_REWARDS);
            if (!result.success()) {
                log.error("领取推广分享绑定玩家奖励失败 playerId:{} count:{}", playerId, count);
            }
            activityLogger.sendSharePromoteAddRewards(playerController.getPlayer(), activityData, 0, 8, 0, 0,
                    totalRewards, 0, result.data.getGoldNum(), 0);
            //添加记录
            addRecord(playerInfoData, totalRewards);
            sharePromoteDao.savePlayerInfoData(playerId, playerInfoData);
        } catch (Exception e) {
            log.error("创建玩家推广分享数据失败 playerId={}", playerId, e);
        } finally {
            if (isLock) {
                redisLock.tryUnlock(lock);
            }
        }
        sharePromoteDao.addPlayerIncome(playerId, totalRewards);
        res.infoList = ItemUtils.buildItemInfo(itemId, totalRewards);
        return res;
    }
}
