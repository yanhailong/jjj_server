package com.jjg.game.activity.cashcow.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import com.jjg.game.activity.cashcow.dao.CashCowDao;
import com.jjg.game.activity.cashcow.data.CashCowPlayerActivityData;
import com.jjg.game.activity.cashcow.data.CashCowRecordData;
import com.jjg.game.activity.cashcow.data.CashCowTimerParam;
import com.jjg.game.activity.cashcow.message.bean.CashCowDetailInfo;
import com.jjg.game.activity.cashcow.message.bean.CashCowDetailType;
import com.jjg.game.activity.cashcow.message.bean.CashCowShowRecord;
import com.jjg.game.activity.cashcow.message.req.ReqCashCowRecord;
import com.jjg.game.activity.cashcow.message.req.ReqCashCowTotalPool;
import com.jjg.game.activity.cashcow.message.res.*;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.ActivityBuilder;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.CashcowCfg;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.RobotCfg;
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

/**
 * 摇钱树
 *
 * @author lm
 * @date 2025/9/3 17:43
 */
@Component
public class CashCowController extends BaseActivityController implements TimerListener<CashCowTimerParam> {
    private final Logger log = LoggerFactory.getLogger(CashCowController.class);
    private final CashCowDao cashCowDao;
    private final TimerCenter timerCenter;
    //活动id->detailId-> 添加定时器的参数
    private final Map<Long, Map<Integer, CashCowTimerParam>> timerMap;

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
        //添加机器人定时器
        LocalDateTime now = LocalDateTime.now();
        for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
            if (cfgBean instanceof CashcowCfg cfg) {
                addRobotTimer(cfg, now, activityId, false);
            }
        }
    }

    @Override
    public AbstractResponse joinActivity(long playerId, ActivityData activityData, int detailId) {
        ResCashCowJoin res = new ResCashCowJoin(Code.SUCCESS);
        long activityId = activityData.getId();
        if (!activityData.getValue().contains(detailId)) {
            log.error("玩家参加活动失败 已经不存在该活动playerId:{} activityId:{} detailId:{}", playerId, activityId, detailId);
            res.code = Code.NOT_FOUND;
            return res;
        }
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(detailId);
        if (baseCfgBean instanceof CashcowCfg cfg) {
            try {
                Player player = corePlayerService.get(playerId);
                //检查消耗
                boolean checked = playerPackService.checkHasItems(player, cfg.getNeedItem());
                if (!checked) {
                    res.code = Code.NOT_ENOUGH_ITEM;
                    return res;
                }
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
                long get = 0;
                //加锁
                String lockKey = playerActivityDao.getLockKey(playerId, activityId);
                redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
                try {
                    Map<Integer, CashCowPlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
                    CashCowPlayerActivityData data = playerActivityData.computeIfAbsent(detailId, key -> new CashCowPlayerActivityData(activityId, activityData.getRound()));
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
                                    playerPackService.addItem(playerId, ItemUtils.getGoldItemId(), get, "CashCowJoin");
                                    //添加记录
                                    CashCowRecordData cashCowRecordData = new CashCowRecordData(activityData.getRound(), System.currentTimeMillis(), player.getNickName(), cfg.getType(), get);
                                    cashCowDao.savePlayerRecordActivity(playerId, activityId, cashCowRecordData);
                                }
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("玩家参加摇钱树加锁后出现异常 playerId:{} activityId:{} detailId:{}", playerId, activityId, detailId, e);
                } finally {
                    redisLock.unlock(lockKey);
                }
                //构建返回消息
                res.activityId = activityId;
                res.detailId = detailId;
                res.num = get;
                res.poll = cashCowDao.getActivityPool(activityId);
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
    public AbstractResponse claimActivityRewards(long playerId, ActivityData activityData, int detailId) {
        ResCashCowClaimRewards res = new ResCashCowClaimRewards(Code.SUCCESS);
        long activityId = activityData.getId();
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
                CommonResult<Void> addedItems = playerPackService.addItems(playerId, cfg.getRewards(), "CashCowRewords");
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
        //清除机器人定时器
        for (Map<Integer, CashCowTimerParam> value : timerMap.values()) {
            for (CashCowTimerParam timerParam : value.values()) {
                timerCenter.remove(this, timerParam);
            }
        }
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
            activityData.setRound(TimeHelper.toYyyyMMdd00000(activityData.getTimeStart()));
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
            GlobalConfigCfg configCfg = GameDataManager.getGlobalConfigCfg(23);
            for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                if (cfgBean instanceof CashcowCfg cfg) {
                    long pool = cashCowDao.getActivityPool(activityId, cfg.getId());
                    if (pool == 0) {
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
                if (probabilityList.get(4) < RandomUtil.randomInt(10000)) {
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
            int nextTime = RandomUtil.randomInt(probabilityList.get(2), probabilityList.get(3));
            //添加定时器
            CashCowTimerParam timerParam = new CashCowTimerParam(activityId, cfg.getId());
            timerCenter.add(new TimerEvent<>(this, nextTime * 1000, timerParam));
            log.debug("摇钱树添加机器人获奖成功 activity:{} detailId:{} time:{}", activityId, cfg.getId(), nextTime);
            //保存到列表
            Map<Integer, CashCowTimerParam> paramMap = timerMap.computeIfAbsent(activityId, key -> new HashMap<>());
            paramMap.put(cfg.getId(), timerParam);
        }
    }

    @Override
    public int updateActivity(String jsonData) {
        return 0;
    }

    @Override
    public AbstractResponse getPlayerActivityDetail(long playerId, long activityId, int detailId) {
        ResCashCowDetailInfo detailInfo = new ResCashCowDetailInfo(Code.SUCCESS);
        ActivityData data = activityManager.getActivityData().get(activityId);
        if (data == null || detailId != -1 && !data.getValue().contains(detailId)) {
            return detailInfo;
        }
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        if (CollectionUtil.isEmpty(baseCfgBeanMap) || detailId != -1 && !baseCfgBeanMap.containsKey(detailId)) {
            return detailInfo;
        }
        Map<Integer, CashCowPlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, data.getType(), activityId);
        detailInfo.detailInfo = new ArrayList<>();
        if (detailId == -1) {
            for (Integer id : data.getValue()) {
                BaseActivityDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(activityId, baseCfgBeanMap.get(id), playerActivityData.get(id));
                if (baseActivityDetailInfo instanceof CashCowDetailInfo cardDetailInfo) {
                    detailInfo.detailInfo.add(cardDetailInfo);
                }
            }
        } else {
            BaseActivityDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(activityId, baseCfgBeanMap.get(detailId), playerActivityData.get(detailId));
            if (baseActivityDetailInfo instanceof CashCowDetailInfo cardDetailInfo) {
                detailInfo.detailInfo.add(cardDetailInfo);
            }
        }
        return detailInfo;
    }

    @Override
    public BaseActivityDetailInfo buildPlayerActivityDetail(long activityId, BaseCfgBean baseCfgBean, PlayerActivityData data) {
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

    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(long playerId, List<List<BaseActivityDetailInfo>> allDetailInfo) {
        ResCashCowTypeInfo cashCowTypeInfo = new ResCashCowTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cashCowTypeInfo;
        }
        cashCowTypeInfo.activityData = new ArrayList<>();
        long activityId = 0;
        for (List<BaseActivityDetailInfo> baseActivityDetailInfos : allDetailInfo) {
            CashCowDetailType cashCowDetailType = new CashCowDetailType();
            cashCowDetailType.detailInfos = new ArrayList<>();
            cashCowTypeInfo.activityData.add(cashCowDetailType);
            for (BaseActivityDetailInfo baseActivityDetailInfo : baseActivityDetailInfos) {
                if (activityId == 0) {
                    activityId = baseActivityDetailInfo.activityId;
                }
                if (baseActivityDetailInfo instanceof CashCowDetailInfo info) {
                    cashCowDetailType.detailInfos.add(info);
                }
            }
            cashCowDetailType.currentProgress = cashCowDao.getPlayerActivityProgress(playerId, activityId);
            activityId = 0;
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
            for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                //累计奖励
                if (cfgBean instanceof CashcowCfg cfg && cfg.getType() == 4) {
                    if (activityProgress >= cfg.getCondition()) {
                        CashCowPlayerActivityData data = playerActivityData.get(cfg.getId());
                        if (data == null) {
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
        List<CashCowRecordData> playerRecordActivities = null;
        //个人记录
        if (req.type == 1) {
            playerRecordActivities = cashCowDao.getPlayerRecordActivities(playerController.playerId(), req.activityId,
                    req.startIndex, req.startIndex + Math.min(req.size,ActivityConstant.CashCow.DEFAULT_SIZE));
        } else if (req.type == 2) {
            //全局记录
            playerRecordActivities = cashCowDao.getAllRecordActivities(req.activityId, req.startIndex, req.startIndex +
                    Math.min(req.size,ActivityConstant.CashCow.DEFAULT_SIZE));
        }
        if (CollectionUtil.isNotEmpty(playerRecordActivities)) {
            res.recordList = new ArrayList<>();
            for (CashCowRecordData playerRecordActivity : playerRecordActivities) {
                CashCowShowRecord cashCowShowRecord = new CashCowShowRecord();
                cashCowShowRecord.recordTime = playerRecordActivity.getRecordTime();
                cashCowShowRecord.type = playerRecordActivity.getType();
                cashCowShowRecord.num = playerRecordActivity.getNum();
                cashCowShowRecord.round = playerRecordActivity.getRound();
                cashCowShowRecord.name = playerRecordActivity.getName();
                res.recordList.add(cashCowShowRecord);
            }
        }
        return res;
    }

    public AbstractResponse reqCashCowTotalPool(PlayerController playerController, ReqCashCowTotalPool req) {
        ResCashCowTotalPool res = new ResCashCowTotalPool(Code.SUCCESS);
        res.activityId = req.activityId;
        res.totalNum = cashCowDao.getActivityPool(req.activityId);
        return res;
    }

    @Override
    public void onTimer(TimerEvent<CashCowTimerParam> timerEvent) {
        if (marsCurator.isMaster()) {
            //机器人定时抽奖
            CashCowTimerParam param = timerEvent.getParameter();
            ActivityData data = activityManager.getActivityData().get(param.activityId());
            if (data == null || !data.canRun()) {
                return;
            }
            Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(param.activityId());
            BaseCfgBean baseCfgBean = baseCfgBeanMap.get(param.detailId());
            if (baseCfgBean instanceof CashcowCfg cfg) {
                addRobotTimer(cfg, LocalDateTime.now(), param.activityId(), true);
            }
        }
    }
}
