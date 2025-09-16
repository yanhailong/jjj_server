package com.jjg.game.activity.cashcow.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import com.jjg.game.activity.cashcow.dao.CashCowDao;
import com.jjg.game.activity.cashcow.data.CashCowPlayerActivityData;
import com.jjg.game.activity.cashcow.data.CashCowRecordData;
import com.jjg.game.activity.cashcow.message.bean.CashCowActivityInfo;
import com.jjg.game.activity.cashcow.message.bean.CashCowDetailInfo;
import com.jjg.game.activity.cashcow.message.bean.CashCowShowRecord;
import com.jjg.game.activity.cashcow.message.req.ReqCashCowFreeRewards;
import com.jjg.game.activity.cashcow.message.req.ReqCashCowRecord;
import com.jjg.game.activity.cashcow.message.req.ReqCashCowTotalPool;
import com.jjg.game.activity.cashcow.message.res.*;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.ActivityBuilder;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.listener.IGameClusterLeaderListener;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.CashcowCfg;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.RobotCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 摇钱树
 *
 * @author lm
 * @date 2025/9/3 17:43
 */
@Component
public class CashCowController extends BaseActivityController implements TimerListener<String>, IGameClusterLeaderListener, ConfigExcelChangeListener {
    private final Logger log = LoggerFactory.getLogger(CashCowController.class);
    private final CashCowDao cashCowDao;
    private final TimerCenter timerCenter;
    //活动id->detailId-> 添加定时器的参数
    private final Map<Long, Map<Integer, Long>> timerMap;
    private long lastRobotAddTime;
    private final String TIMER_KEY = "cashCow";
    private volatile TimerEvent<String> timerEvent = null;
    private final ReentrantLock reentrantLock = new ReentrantLock();

    public CashCowController(CashCowDao cashCowDao, TimerCenter timerCenter) {
        this.cashCowDao = cashCowDao;
        this.timerCenter = timerCenter;
        timerMap = new ConcurrentHashMap<>();
    }


    @Override
    public void activityLoadCompleted(ActivityData activityData) {
        long activityId = activityData.getId();
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        if (CollectionUtil.isEmpty(baseCfgBeanMap)) {
            return;
        }
        if (activityManager.isExecutionNode()) {
            addTimerEvent();
            LocalDateTime now = LocalDateTime.now();
            for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                if (cfgBean instanceof CashcowCfg cfg) {
                    addRobotTimer(cfg, now, activityId, false);
                }
            }
        }
    }

    /**
     * 添加轮询定时器
     */
    public void addTimerEvent() {
        if (timerEvent == null) {
            reentrantLock.lock();
            if (timerEvent == null) {
                //添加定时器
                timerEvent = new TimerEvent<>(this, TIMER_KEY, TimeHelper.ONE_SECOND_OF_MILLIS);
                timerCenter.add(timerEvent);
            }
            reentrantLock.unlock();
        }
    }

    @Override
    public void addActivityProgress(ActivityData activityData, long progress, Object additionalParameters) {
        if (notAddProgress(additionalParameters)) return;
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.CashCow.CASH_COW_ADD_POOL_PROPORTION);
        long realProgress = BigDecimal.valueOf(progress).multiply(BigDecimal.valueOf(globalConfigCfg.getIntValue())).divide(BigDecimal.valueOf(10000), RoundingMode.FLOOR).longValue();
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
            if (cfgBean instanceof CashcowCfg cfg && cfg.getType() != 4) {
                cashCowDao.addActivityPool(activityData.getId(), cfgBean.getId(), realProgress);
            }
        }
    }

    private static boolean notAddProgress(Object additionalParameters) {
        return additionalParameters instanceof Integer itemId && !itemId.equals(ItemUtils.getGoldItemId());
    }

    @Override
    public boolean addPlayerProgress(long playerId, ActivityData data, long progress, Object additionalParameters) {
        if (notAddProgress(additionalParameters)) {
            return false;
        }
        long added = cashCowDao.addPlayerActivityProgress(playerId, data.getId(), progress);
        String lockKey = playerActivityDao.getLockKey(playerId, data.getId());
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(data.getId());
        boolean canClaim = false;
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            Map<Integer, CashCowPlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, data.getType(), data.getId());
            for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                if (cfgBean instanceof CashcowCfg cfg && cfg.getType() == 4) {
                    CashCowPlayerActivityData activityData = playerActivityData.computeIfAbsent(cfg.getId(), key -> new CashCowPlayerActivityData(data.getId(), data.getRound()));
                    if (activityData.getClaimStatus() == ActivityConstant.ClaimStatus.CLAIMED) {
                        continue;
                    }
                    if (added >= cfg.getCondition()) {
                        activityData.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
                        canClaim = true;
                    }
                }
            }
            playerActivityDao.savePlayerActivityData(playerId, data.getType(), data.getId(), playerActivityData);
        } catch (Exception e) {
            log.error("摇钱树增加玩家个人进度失败 playerId:{} addVelue:{}", playerId, progress);
            throw new RuntimeException(e);
        } finally {
            redisLock.unlock(lockKey);
        }
        return canClaim;
    }

    @Override
    public AbstractResponse joinActivity(Player oldPlayer, ActivityData activityData, int detailId, int times) {
        ResCashCowJoin res = new ResCashCowJoin(Code.SUCCESS);
        long playerId = oldPlayer.getId();
        long activityId = activityData.getId();
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(detailId);
        if (baseCfgBean instanceof CashcowCfg cfg) {
            if (cfg.getType() == 4) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            try {
                List<List<Integer>> weight = cfg.getWeight();
                if (CollectionUtil.isEmpty(weight) || cfg.getDistribution() <= 0) {
                    res.code = Code.SAMPLE_ERROR;
                    return res;
                }
                for (List<Integer> list : weight) {
                    if (list.size() < 3) {
                        res.code = Code.SAMPLE_ERROR;
                        return res;
                    }
                }
                Player player = corePlayerService.get(playerId);
                CommonResult<ItemOperationResult> addedItem = null;
                CommonResult<ItemOperationResult> removed = null;
                long get = 0;
                //加锁
                String lockKey = playerActivityDao.getLockKey(playerId, activityId);
                redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
                try {
                    Map<Integer, CashCowPlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
                    CashCowPlayerActivityData data = playerActivityData.computeIfAbsent(detailId, key -> new CashCowPlayerActivityData(activityId, activityData.getRound())
                    );
                    //扣除消耗
                    removed = playerPackService.removeItems(player, cfg.getNeedItem(), "cashcow");
                    if (!removed.success()) {
                        res.code = removed.code;
                        return res;
                    }
                    int joinTimes = data.getJoinTimes();
                    for (List<Integer> list : weight) {
                        //判断次数范围
                        if (list.getFirst() <= joinTimes && joinTimes < list.get(1)) {
                            //万分比概率
                            Integer probability = list.getLast();
                            if (RandomUtil.randomInt(10000) < probability) {
                                //获奖
                                get = cashCowDao.reduceActivityPool(activityId, detailId, cfg.getDistribution());
                                if (get > 0) {
                                    //发奖
                                    addedItem = playerPackService.addItem(playerId, ItemUtils.getGoldItemId(), get, "CashCowJoin");
                                    //添加记录
                                    CashCowRecordData cashCowRecordData = new CashCowRecordData(activityData.getRound(), System.currentTimeMillis(), player.getNickName(), cfg.getType(), get);
                                    cashCowDao.savePlayerRecordActivity(playerId, activityId, cashCowRecordData);
                                }
                            }
                            break;
                        }
                    }
                    data.setJoinTimes(data.getJoinTimes() + 1);
                    playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityId, playerActivityData);
                } catch (Exception e) {
                    log.error("玩家参加摇钱树加锁后出现异常 playerId:{} activityId:{} detailId:{}", playerId, activityId, detailId, e);
                    res.code = Code.EXCEPTION;
                } finally {
                    redisLock.unlock(lockKey);
                }
                //记录日志
                if (addedItem != null) {
                    activityLogger.sendCashCowJoinLog(player, activityData, detailId
                            , cfg.getType(), cfg.getNeedItem(), removed.data, get, addedItem.data);
                }
                //构建返回消息
                res.activityId = activityId;
                res.detailId = detailId;
                res.num = get;
                res.pool = cashCowDao.getActivityPool(activityId);
                res.totalPool = cashCowDao.getActivityPool(activityId);
            } catch (Exception e) {
                log.error("玩家参加摇钱树  出现异常 playerId:{} activityId:{} detailId:{}", playerId, activityId, detailId, e);
            }
        } else {
            log.error("玩家参加摇钱树 活动配置为空playerId:{} activityId:{} detailId:{}", playerId, activityId, detailId);
        }
        return res;
    }

    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        ResCashCowClaimRewards res = new ResCashCowClaimRewards(Code.SUCCESS);
        long activityId = activityData.getId();
        long playerId = player.getId();
        String lockKey = playerActivityDao.getLockKey(playerId, activityId);
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(detailId);
        if (baseCfgBean instanceof CashcowCfg cfg) {
            if (cfg.getType() != 4) {
                //非领奖类型禁止领奖
                res.code = Code.PARAM_ERROR;
                return res;
            }
            long activityProgress = cashCowDao.getPlayerActivityProgress(playerId, activityId);
            if (activityProgress < cfg.getCondition()) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            CashCowPlayerActivityData data = null;
            CommonResult<ItemOperationResult> addedItems = null;
            boolean send = false;
            redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
            try {
                //领取奖励
                Map<Integer, CashCowPlayerActivityData> dataMap = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
                if (CollectionUtil.isEmpty(dataMap)) {
                    res.code = Code.PARAM_ERROR;
                    return res;
                }
                data = dataMap.computeIfAbsent(detailId, key -> new CashCowPlayerActivityData(activityId, activityData.getRound()));
                if (data.getClaimStatus() == ActivityConstant.ClaimStatus.CLAIMED) {
                    res.code = Code.REPEAT_OP;
                    return res;
                }
                addedItems = playerPackService.addItems(playerId, cfg.getRewards(), "CashCowRewords");
                if (!addedItems.success()) {
                    res.code = Code.UNKNOWN_ERROR;
                    return res;
                }
                //修改活动数据
                data.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED);
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityId, dataMap);
                send = true;
            } catch (Exception e) {
                log.error("领取摇钱树累计奖励异常 playerId:{} activityId:{}", playerId, activityId, e);
            } finally {
                redisLock.unlock(lockKey);
            }
            if (send) {
                //记录日志
                if (addedItems.success()) {
                    activityLogger.sendCashCowRewards(player, activityData, detailId, addedItems.data, activityProgress, cfg.getRewards());
                }
                res.activityId = activityId;
                res.detailId = detailId;
                res.infoList = ItemUtils.buildItemInfo(cfg.getRewards());
                BaseActivityDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(activityId, cfg, data);
                if (baseActivityDetailInfo instanceof CashCowDetailInfo info) {
                    res.detailInfo = info;
                }
            }
        }
        //发送响应
        return res;
    }

    @Override
    public void onActivityEnd(ActivityData activityData) {
        //设置活动状态
        activityData.setStatus(ActivityConstant.ActivityStatus.ENDED);
        timerMap.clear();
    }

    @Override
    public void onActivityStart(ActivityData activityData) {
        long activityId = activityData.getId();
        if (activityData.getStatus() == ActivityConstant.ActivityStatus.RUNNING) {
            log.error("摇钱树开始失败 活动正在进行中 activityId:{} ", activityId);
            return;
        }
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        //初始化摇钱树
        if (activityData.getRound() == 0) {
            activityData.setRound(activityData.getId());
            if (CollectionUtil.isNotEmpty(baseCfgBeanMap)) {
                for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                    if (cfgBean instanceof CashcowCfg cfg && cfg.getType() != 4) {
                        cashCowDao.setActivityPool(activityId, cfg.getId(), cfg.getInitialprizepool());
                    }
                }
            } else {
                log.error("摇钱树初始化奖池失败 未找到配置");
            }
        } else {
            activityData.addRound();
            //将上一轮的加作为底池
            GlobalConfigCfg configCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.CashCow.CASH_COW_ADD_NEXT_ROUND_PROPORTION);
            for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                if (cfgBean instanceof CashcowCfg cfg) {
                    long pool = cashCowDao.getActivityPool(activityId, cfg.getId());
                    if (pool == 0 && cfg.getType() != 4) {
                        //初始化池子
                        cashCowDao.addActivityPool(activityId, cfg.getId(), cfg.getInitialprizepool());
                    } else {
                        long nextPoll = BigDecimal.valueOf(pool)
                                .multiply(BigDecimal.valueOf(configCfg.getIntValue()))
                                .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN)
                                .longValue();
                        cashCowDao.setActivityPool(activityId, cfg.getId(), nextPoll);

                    }
                }
            }
            //删除在线玩家的数据
            List<Long> onlinePlayerIds = activityManager.getOnlinePlayerIds();
            for (Long onlinePlayerId : onlinePlayerIds) {
                cashCowDao.delPlayerActivityProgress(onlinePlayerId, activityId);
                playerActivityDao.deletePlayerActivityData(onlinePlayerId, activityData.getType(), activityId);
            }
            //修改活动状态
            activityData.setStatus(ActivityConstant.ActivityStatus.RUNNING);
        }
    }

    private void addRobotTimer(CashcowCfg cfg, LocalDateTime now, long activityId, boolean targetRewards) {
        List<List<Integer>> winningFrequency = cfg.getWinningFrequency();
        if (CollectionUtil.isEmpty(winningFrequency) || cfg.getType() == 4) {
            return;
        }
        for (List<Integer> probabilityList : winningFrequency) {
            if (probabilityList.size() < 5 || !(probabilityList.get(0) <= now.getHour() && now.getHour() < probabilityList.get(1))) {
                continue;
            }
            //触发奖励
            if (targetRewards) {
                if (probabilityList.get(4) > RandomUtil.randomInt(10000)) {
                    //随机机器人
                    RobotCfg robotCfg = RandomUtil.randomEle(GameDataManager.getRobotCfgList());
                    //扣除奖池
                    long get = cashCowDao.reduceActivityPool(activityId, cfg.getId(), cfg.getDistribution());
                    if (get > 0) {
                        //发奖
                        //添加记录
                        CashCowRecordData cashCowRecordData = new CashCowRecordData(robotCfg.getId(), System.currentTimeMillis(), robotCfg.getName(), cfg.getType(), get);
                        cashCowDao.savePlayerRecordActivity(robotCfg.getId(), activityId, cashCowRecordData);
                    }
                }
            }
            //随机间隔时间
            long nextTime = RandomUtil.randomInt(probabilityList.get(2), probabilityList.get(3));
            //添加定时器
            log.debug("摇钱树添加机器人获奖成功 activity:{} detailId:{} time:{}", activityId, cfg.getId(), nextTime);
            //保存到列表
            Map<Integer, Long> paramMap = timerMap.computeIfAbsent(activityId, key -> new HashMap<>());
            paramMap.put(cfg.getId(), System.currentTimeMillis() + nextTime * TimeHelper.ONE_SECOND_OF_MILLIS);
        }
    }

    @Override
    public int updateActivity(String jsonData) {
        return 0;
    }

    @Override
    public AbstractResponse getPlayerActivityDetail(long playerId, ActivityData data, int detailId) {
        long activityId = data.getId();
        ResCashCowDetailInfo detailInfo = new ResCashCowDetailInfo(Code.SUCCESS);
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        Map<Integer, CashCowPlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, data.getType(), activityId);
        detailInfo.detailInfo = new ArrayList<>();
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(detailId);
        if (baseCfgBean instanceof CashcowCfg cfg) {
            CashCowDetailInfo cardDetailInfo = buildPlayerActivityDetail(activityId, cfg, playerActivityData.get(detailId));
            detailInfo.detailInfo.add(cardDetailInfo);
        }
        return detailInfo;
    }

    @Override
    public CashCowDetailInfo buildPlayerActivityDetail(long activityId, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (baseCfgBean instanceof CashcowCfg cfg) {
            CashCowDetailInfo info = new CashCowDetailInfo();
            info.activityId = activityId;
            info.detailId = baseCfgBean.getId();
            info.type = cfg.getType();
            //累计奖励
            if (cfg.getType() == 4) {
                //奖励信息
                info.rewardItems = ItemUtils.buildItemInfo(cfg.getRewards());
                if (data != null) {
                    info.claimStatus = data.getClaimStatus();
                }
                info.needProgress = cfg.getCondition();
            } else {
                //抽奖
                info.costItems = ItemUtils.buildItemInfo(cfg.getNeedItem());
                info.pool = cashCowDao.getActivityPool(activityId, baseCfgBean.getId());
            }
            return info;
        }
        return null;
    }

    /**
     * 获取配置的免费次数
     */
    private Item getConfigFreeRewards() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.CashCow.CASH_COW_FREE_ITEM);
        String[] itemInfo = StringUtils.split(globalConfigCfg.getValue(), "_");
        if (itemInfo.length == 2) {
            return new Item(Integer.parseInt(itemInfo[0]), Long.parseLong(itemInfo[1]));
        }
        return null;
    }

    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(long playerId, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResCashCowTypeInfo cashCowTypeInfo = new ResCashCowTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cashCowTypeInfo;
        }
        cashCowTypeInfo.activityData = new ArrayList<>();
        for (Map.Entry<Long, List<BaseActivityDetailInfo>> entry : allDetailInfo.entrySet()) {
            CashCowActivityInfo cashCowActiviTyInfo = new CashCowActivityInfo();
            cashCowActiviTyInfo.detailInfos = new ArrayList<>();
            cashCowTypeInfo.activityData.add(cashCowActiviTyInfo);
            for (BaseActivityDetailInfo baseActivityDetailInfo : entry.getValue()) {
                if (baseActivityDetailInfo instanceof CashCowDetailInfo info) {
                    cashCowActiviTyInfo.detailInfos.add(info);
                }
            }
            Long activityId = entry.getKey();
            cashCowActiviTyInfo.currentProgress = cashCowDao.getPlayerActivityProgress(playerId, activityId);
            ActivityData data = activityManager.getActivityData().get(activityId);
            cashCowActiviTyInfo.endTime = data.getTimeEnd();
            cashCowActiviTyInfo.round = data.getRound();
            cashCowActiviTyInfo.resetRemainTime = TimeHelper.getNextDayRemainTime();
            Item freeRewards = getConfigFreeRewards();
            if (freeRewards != null) {
                cashCowActiviTyInfo.freeItemInfo = ItemUtils.buildItemInfo(freeRewards.getId(), freeRewards.getItemCount());
            }
            cashCowActiviTyInfo.freeStatus = cashCowDao.getFreeRewardsStatus(playerId, activityId);
        }
        return cashCowTypeInfo;
    }


    @Override
    public ActivityInfo buildActivityInfo(long playerId, ActivityData activityData) {
        //获取进度
        long activityProgress = cashCowDao.getPlayerActivityProgress(playerId, activityData.getId());
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        int claimStatus = 0;
        Map<Integer, CashCowPlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
        if (CollectionUtil.isNotEmpty(baseCfgBeanMap)) {
            if (cashCowDao.getFreeRewardsStatus(playerId, activityData.getId())) {
                claimStatus = ActivityConstant.ClaimStatus.CAN_CLAIM;
                return ActivityBuilder.buildActivityInfo(activityData, claimStatus);
            }
            for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                //累计奖励
                if (cfgBean instanceof CashcowCfg cfg) {
                    CashCowPlayerActivityData data = playerActivityData.get(cfg.getId());
                    if (cfg.getType() == 4 && activityProgress >= cfg.getCondition()) {
                        if (data != null && data.getClaimStatus() == ActivityConstant.ClaimStatus.CAN_CLAIM) {
                            claimStatus = ActivityConstant.ClaimStatus.CAN_CLAIM;
                            break;
                        }
                    }
                }
            }
        }
        return ActivityBuilder.buildActivityInfo(activityData, claimStatus);
    }


    @Override
    public List<BaseCfgBean> getDetailCfgBean() {
        return new ArrayList<>(GameDataManager.getCashcowCfgList());
    }


    @Override
    public Class<CashcowCfg> getDetailDataClass() {
        return CashcowCfg.class;
    }

    public AbstractResponse reqCashCowRecord(PlayerController playerController, ReqCashCowRecord req) {
        ResCashCowRecord res = new ResCashCowRecord(Code.SUCCESS);
        res.activityId = req.activityId;
        res.type = req.type;
        Pair<List<CashCowRecordData>, Boolean> playerRecordActivities = null;
        //个人记录
        if (req.type == 1) {
            playerRecordActivities = cashCowDao.getPlayerRecordActivities(playerController.playerId(), req.activityId,
                    req.startIndex, req.startIndex + Math.min(req.size, ActivityConstant.CashCow.DEFAULT_SIZE));
        } else if (req.type == 2) {
            //全局记录
            playerRecordActivities = cashCowDao.getAllRecordActivities(req.activityId, req.startIndex, req.startIndex +
                    Math.min(req.size, ActivityConstant.CashCow.DEFAULT_SIZE));
        }
        if (playerRecordActivities != null && CollectionUtil.isNotEmpty(playerRecordActivities.getFirst())) {
            res.recordList = new ArrayList<>();
            for (CashCowRecordData playerRecordActivity : playerRecordActivities.getFirst()) {
                CashCowShowRecord cashCowShowRecord = new CashCowShowRecord();
                cashCowShowRecord.recordTime = playerRecordActivity.getRecordTime();
                cashCowShowRecord.type = playerRecordActivity.getType();
                cashCowShowRecord.num = playerRecordActivity.getNum();
                cashCowShowRecord.round = playerRecordActivity.getRound();
                cashCowShowRecord.name = playerRecordActivity.getName();
                res.recordList.add(cashCowShowRecord);
            }
            res.hasNext = playerRecordActivities.getSecond();
            res.startIndex = req.startIndex;
        }
        return res;
    }

    /**
     * 请求摇钱树总池子
     */
    public AbstractResponse reqCashCowTotalPool(PlayerController playerController, ReqCashCowTotalPool req) {
        ResCashCowTotalPool res = new ResCashCowTotalPool(Code.SUCCESS);
        res.activityId = req.activityId;
        res.totalNum = cashCowDao.getActivityPool(req.activityId);
        return res;
    }

    @Override
    public void onTimer(TimerEvent<String> timerEvent) {
        try {
            if (activityManager.isExecutionNode()) {
                long timeMillis = System.currentTimeMillis();
                //机器人定时抽奖
                for (Map.Entry<Long, Map<Integer, Long>> entry : timerMap.entrySet()) {
                    Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(entry.getKey());
                    if (CollectionUtil.isEmpty(baseCfgBeanMap)) {
                        continue;
                    }
                    for (Map.Entry<Integer, Long> longEntry : entry.getValue().entrySet()) {
                        if (timeMillis >= longEntry.getValue()) {
                            BaseCfgBean baseCfgBean = baseCfgBeanMap.get(longEntry.getKey());
                            if (baseCfgBean instanceof CashcowCfg cfg) {
                                addRobotTimer(cfg, LocalDateTime.now(), entry.getKey(), true);
                            }
                        }
                    }
                }
                if (GameDataManager.getInstance().isLoadAllFinished()) {
                    //机器人自动增加奖池
                    GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.CashCow.CASH_COW_ROBOT_ADD_FREQUENCY);
                    if (globalConfigCfg != null && StringUtils.isNotEmpty(globalConfigCfg.getValue())) {
                        String[] cfg = StringUtils.split(globalConfigCfg.getValue(), "_");
                        if (cfg.length == 2) {
                            //判断是否触发
                            if (lastRobotAddTime == 0 || lastRobotAddTime + Long.parseLong(cfg[0]) * TimeHelper.ONE_SECOND_OF_MILLIS < timeMillis) {
                                lastRobotAddTime = timeMillis;
                                if (Integer.parseInt(cfg[0]) > RandomUtil.randomInt(10000)) {
                                    //触发增加
                                    GlobalConfigCfg addCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.CashCow.CASH_COW_ROBOT_ADD_VALUE);
                                    List<List<Integer>> cfgAdd = getCfgAdd(addCfg.getValue());
                                    if (CollectionUtil.isEmpty(cfgAdd)) {
                                        return;
                                    }
                                    int hour = LocalDateTime.now().getHour();
                                    for (List<Integer> list : cfgAdd) {
                                        if (list.getFirst() >= hour && hour < list.get(1)) {
                                            int addValue = RandomUtil.randomInt(list.get(2), list.get(3));
                                            Map<Long, ActivityData> activityDataMap = activityManager.getActivityTypeData().get(ActivityType.CASH_COW);
                                            for (ActivityData activityData : activityDataMap.values()) {
                                                Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
                                                for (BaseCfgBean baseCfgBean : baseCfgBeanMap.values()) {
                                                    if (baseCfgBean instanceof CashcowCfg cashcowCfg) {
                                                        if (cashcowCfg.getType() != 4) {
                                                            cashCowDao.addActivityPool(activityData.getId(), baseCfgBean.getId(), addValue);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("摇钱树 定时任务执行失败", e);
        }
    }

    /**
     * 获取摇钱树机器人自动增加奖池配置
     *
     * @param cfg 配置
     */
    public List<List<Integer>> getCfgAdd(String cfg) {
        List<List<Integer>> arrayList = new ArrayList<>();
        if (StringUtils.isNotEmpty(cfg)) {
            String[] split = StringUtils.split(cfg, "|");
            for (String string : split) {
                List<Integer> list = new ArrayList<>();
                arrayList.add(list);
                String[] split2 = StringUtils.split(string, "_");
                for (String s : split2) {
                    list.add(Integer.parseInt(s));
                }
            }
        }
        return arrayList;
    }

    @Override
    public void isLeader() {
        if (activityManager.isExecutionNode()) {
            addTimerEvent();
            //获取所有该类型的活动
            Map<Long, ActivityData> activityDataMap = activityManager.getActivityTypeData().get(ActivityType.CASH_COW);
            if (CollectionUtil.isEmpty(activityDataMap)) {
                return;
            }
            for (ActivityData activityData : activityDataMap.values()) {
                if (!activityData.canRun()) {
                    Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
                    for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                        if (cfgBean instanceof CashcowCfg cfg) {
                            addRobotTimer(cfg, LocalDateTime.now(), activityData.getId(), false);
                        }
                    }
                }
            }
            log.debug("选举为主节点 摇钱树机器人添加");
        }
    }

    @Override
    public void notLeader() {
        timerCenter.remove(this, TIMER_KEY);
        timerEvent = null;
        timerMap.clear();
        log.debug("退举 摇钱树机器人删除");
    }

    public AbstractResponse reqCashCowFreeRewards(PlayerController playerController, ReqCashCowFreeRewards req) {
        ResCashCowFreeRewards res = new ResCashCowFreeRewards(Code.SUCCESS);
        Item freeRewards = getConfigFreeRewards();
        if (freeRewards == null) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        boolean freeRewardsStatus = cashCowDao.getFreeRewardsStatus(playerController.playerId(), req.activityId);
        if (freeRewardsStatus) {
            res.code = Code.REPEAT_OP;
            return res;
        }
        CommonResult<ItemOperationResult> addItems = null;
        String playerFreeLockKey = cashCowDao.getPlayerFreeLockKey(playerController.playerId(), req.activityId);
        redisLock.lock(playerFreeLockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            freeRewardsStatus = cashCowDao.getFreeRewardsStatus(playerController.playerId(), req.activityId);
            if (freeRewardsStatus) {
                res.code = Code.REPEAT_OP;
                return res;
            }
            //添加道具
            addItems = playerPackService.addItem(playerController.playerId(), freeRewards.getId(), freeRewards.getItemCount(), "CashCowFreeRewards");
            if (!addItems.success()) {
                res.code = Code.UNKNOWN_ERROR;
                return res;
            }
            cashCowDao.addFreeRewardsCount(playerController.playerId(), req.activityId);
        } catch (Exception e) {
            log.error("摇钱树请求领取免费道具失败 playerId:{} activityId:{}", playerController.playerId(), req.activityId);
        } finally {
            redisLock.unlock(playerFreeLockKey);
        }
        if (addItems != null && addItems.success()) {
            activityLogger.sendCashCowFreeRewards(playerController.getPlayer(), req.activityId, addItems.data, freeRewards);
        }
        res.activityId = req.activityId;
        res.itemInfos = ItemUtils.buildItemInfo(freeRewards.getId(), freeRewards.getItemCount());
        return res;
    }
}
