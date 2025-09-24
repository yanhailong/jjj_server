package com.jjg.game.activity.officialawards.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.util.RandomUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityTargetType;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.officialawards.dao.OfficialAwardsDao;
import com.jjg.game.activity.officialawards.data.OfficialAwardsRecord;
import com.jjg.game.activity.officialawards.data.OfficialAwardsTempData;
import com.jjg.game.activity.officialawards.message.bean.OfficialAwardsActivity;
import com.jjg.game.activity.officialawards.message.bean.OfficialAwardsDetailInfo;
import com.jjg.game.activity.officialawards.message.bean.OfficialAwardsShowRecord;
import com.jjg.game.activity.officialawards.message.req.ReqOfficialAwardsRecord;
import com.jjg.game.activity.officialawards.message.res.*;
import com.jjg.game.activity.util.DataCache;
import com.jjg.game.common.listener.IGameClusterLeaderListener;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.OfficialAwardsCfg;
import com.jjg.game.sampledata.bean.RobotCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 官方派奖活动控制器
 *
 * @author lm
 * @date 2025/9/3
 */
@Component
public class OfficialAwardsController extends BaseActivityController implements IGameClusterLeaderListener
        , TimerListener<Long> {
    private final Logger log = LoggerFactory.getLogger(OfficialAwardsController.class);
    private final OfficialAwardsDao officialAwardsDao;
    private final DataCache dataCache;
    private final TimerCenter timerCenter;

    public OfficialAwardsController(OfficialAwardsDao officialAwardsDao, DataCache dataCache, TimerCenter timerCenter) {
        this.officialAwardsDao = officialAwardsDao;
        this.dataCache = dataCache;
        this.timerCenter = timerCenter;
    }

    @Override
    public boolean addPlayerProgress(long playerId, ActivityData activityData, long progress, long activityTargetKey, Object additionalParameters) {
        int conversionType = getConversionType(activityData);
        if (conversionType == 0) {
            return false;
        }
        if (activityTargetKey == ActivityTargetType.RECHARGE.getTargetKey()
                && conversionType != ActivityConstant.OfficialAwards.CALCULATION_RECHARGE) {
            return false;
        }
        if (activityTargetKey == ActivityTargetType.EFFECTIVE_BET.getTargetKey()
                && conversionType != ActivityConstant.OfficialAwards.CALCULATION_EFFECTIVE_WATER_FLOW) {
            return false;
        }
        //获取本轮的参加类型
        int addValue = getAddValue(progress, conversionType);
        if (addValue > 0) {
            officialAwardsDao.incrementPlayerProgress(playerId, ActivityConstant.OfficialAwards.TOMORROW_POINTS, addValue);
        }
        return false;
    }

    private int getAddValue(long progress, int conversionType) {
        Pair<Integer, Integer> pair = null;
        //充值类型
        if (conversionType == ActivityConstant.OfficialAwards.CALCULATION_RECHARGE) {
            pair = dataCache.getRechargeConvertRatio();
        } else if (conversionType == ActivityConstant.OfficialAwards.CALCULATION_EFFECTIVE_WATER_FLOW) {
            //有效下注类型
            pair = dataCache.getEffectiveWaterFlowConvertRatio();
        }
        if (pair == null) {
            return 0;
        }
        return BigDecimal.valueOf(progress).multiply(BigDecimal.valueOf(pair.getSecond()))
                .divide(BigDecimal.valueOf(pair.getFirst()), RoundingMode.DOWN)
                .intValue();
    }

    @Override
    public void checkPlayerDataAndReset(long playerId, ActivityData activityData) {
        int playerProgress = officialAwardsDao.getPlayerProgress(playerId, ActivityConstant.OfficialAwards.TOMORROW_POINTS);
        if (playerProgress > 0) {
            officialAwardsDao.deletePlayerProgress(playerId, ActivityConstant.OfficialAwards.TODAY_POINTS);
            //设置今日积分
            officialAwardsDao.incrementPlayerProgress(playerId, ActivityConstant.OfficialAwards.TODAY_POINTS, playerProgress);
            //设置明天积分
            officialAwardsDao.deletePlayerProgress(playerId, ActivityConstant.OfficialAwards.TOMORROW_POINTS);
        }
    }

    /**
     * 玩家参与官方派奖活动
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动明细ID
     * @param times        参与次数
     * @return 参与活动结果响应
     */
    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        ResOfficialAwardsJoinActivity res = new ResOfficialAwardsJoinActivity(Code.SUCCESS);
        long playerId = player.getId();
        long pool = officialAwardsDao.getTotalPool();
        if (pool <= 0) {
            res.code = Code.OFFICIAL_AWARDS_POOL_NULL;
            return res;
        }
        // 获取活动明细配置
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(detailId);
        //默认初始场
        int turntableType;
        if (baseCfgBean instanceof OfficialAwardsCfg baseCfg) {
            turntableType = baseCfg.getTurntableType();
        } else {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        //获取消耗
        Map<Integer, Integer> needPoints = dataCache.getNeedPoints();
        if (needPoints == null || !needPoints.containsKey(turntableType)) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        int costPoint = needPoints.get(turntableType);
        //扣减积分
        int remainPoint = officialAwardsDao.reducePlayerProgress(playerId, ActivityConstant.OfficialAwards.TODAY_POINTS, costPoint);
        if (remainPoint < 0) {
            res.code = Code.NOT_ENOUGH;
            return res;
        }
        WeightRandom<OfficialAwardsCfg> random = getOfficialAwardsCfgWeightRandom(baseCfgBeanMap, turntableType);
        //获取随机奖励
        OfficialAwardsCfg cfg = random.next();
        Integer getNum = cfg.getGetitem().getLast();
        Pair<Integer, Integer> reducedPair = officialAwardsDao.reduceTotalPool(getNum);
        if (reducedPair.getFirst() < 1) {
            //奖池不足
            res.code = Code.OFFICIAL_AWARDS_POOL_NULL;
            return res;
        }
        //添加道具
        CommonResult<ItemOperationResult> addResult = playerPackService.addItem(playerId, cfg.getGetitem().getFirst(), getNum, "officialAwards");
        if (!addResult.success()) {
            log.error("官方派奖玩家参加活动发奖失败 playerId:{} get:{}", playerId, getNum);
            return res;
        }
        addPlayerRecord(player, cfg, getNum);
        res.infoList = ItemUtils.buildItemInfo(cfg.getGetitem().getFirst(), getNum);
        res.todayPoint = remainPoint;
        res.totalPool = reducedPair.getSecond();
        res.rewardDetailId = cfg.getId();
        return res;
    }

    /**
     * 获取本轮配置的随机器
     *
     * @param baseCfgBeanMap 本轮配置
     * @param turntableType  场次类型
     * @return 随机器
     */
    private WeightRandom<OfficialAwardsCfg> getOfficialAwardsCfgWeightRandom(Map<Integer, BaseCfgBean> baseCfgBeanMap, int turntableType) {
        // 构建权重随机器，只选择 type = turntableType 的官方派奖奖项
        WeightRandom<OfficialAwardsCfg> random = new WeightRandom<>();
        for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
            if (cfgBean instanceof OfficialAwardsCfg cfg && cfg.getTurntableType() == turntableType) {
                random.add(cfg, cfg.getProbability());
            }
        }
        return random;
    }

    /**
     * 添加机器人记录
     */
    private void addRobotRecord(OfficialAwardsCfg cfg, long robotGet) {
        RobotCfg robotCfg = RandomUtil.randomEle(GameDataManager.getRobotCfgList());
        OfficialAwardsRecord officialAwardsRecord = new OfficialAwardsRecord();
        officialAwardsRecord.setName(robotCfg.getName());
        officialAwardsRecord.setCreateTime(System.currentTimeMillis());
        officialAwardsRecord.setType(cfg.getTurntableType());
        officialAwardsRecord.setGetNum(robotGet);
        //添加记录
        officialAwardsDao.savePlayerRecord(0, officialAwardsRecord);
    }

    /**
     * 添加玩家记录
     */
    private void addPlayerRecord(Player player, OfficialAwardsCfg cfg, long playerGet) {
        OfficialAwardsRecord officialAwardsRecord = new OfficialAwardsRecord();
        officialAwardsRecord.setName(player.getNickName());
        officialAwardsRecord.setCreateTime(System.currentTimeMillis());
        officialAwardsRecord.setType(cfg.getTurntableType());
        officialAwardsRecord.setGetNum(playerGet);
        //添加记录
        officialAwardsDao.savePlayerRecord(player.getId(), officialAwardsRecord);
    }


    /**
     * 官方派奖活动奖励领取接口（暂未实现）
     */
    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        return null;
    }

    @Override
    public void onActivityEnd(ActivityData activityData) {
//        if (activityManager.isExecutionNode()) {
            //清除所有记录数据
            officialAwardsDao.deleteAllRecords();
            officialAwardsDao.deleteAllPlayerRecords();
            //清除所有玩家信息
            officialAwardsDao.deleteAllPlayerAllProgress();
            //清除奖池信息
            officialAwardsDao.deleteTotalPool();
//        }
    }

    @Override
    public void onActivityStart(ActivityData activityData) {
        if (getConversionType(activityData) == 0) {
            return;
        }
//        if (activityManager.isExecutionNode()) {
            //设置初始奖池
            GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.OfficialAwards.INITIAL_AMOUNT);
            if (globalConfigCfg == null || globalConfigCfg.getLongValue() == 0) {
                log.error("官方派奖活动未配置 主奖金");
                return;
            }
            officialAwardsDao.setTotalPool(globalConfigCfg.getLongValue());
            //添加机器人获奖逻辑
            robotAction(activityData.getId());
//        }
    }

    /**
     * 获取本轮转换类型
     *
     * @param activityData 活动数据
     * @return 本轮转换类型 1充值 2有效流水
     */
    private int getConversionType(ActivityData activityData) {
        if (activityData.getActivityTempData() instanceof OfficialAwardsTempData data) {
            return data.getConversionType();
        }
        Map<Integer, BaseCfgBean> beanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        if (beanMap == null || beanMap.isEmpty()) {
            return 0;
        }
        for (BaseCfgBean cfgBean : beanMap.values()) {
            if (cfgBean instanceof OfficialAwardsCfg cfg) {
                OfficialAwardsTempData data = new OfficialAwardsTempData();
                data.setConversionType(cfg.getCalculationType());
                activityData.setActivityTempData(data);
                return data.getConversionType();
            }
        }
        return 0;
    }

    /**
     * 机器人行为
     */
    private void robotAction(long activityId) {
        //获取权重随机器
        WeightRandom<Integer> robotRandom = dataCache.getRobotRandom();
        if (robotRandom != null) {
            Integer nextTime = robotRandom.next();
            if (nextTime != null) {
                timerCenter.add(new TimerEvent<>(this, nextTime, activityId));
            }
        }
    }

    @Override
    public int updateActivity(String jsonData) {
        // 可用于更新活动配置
        return 0;
    }

    /**
     * 获取玩家官方派奖活动明细
     */
    @Override
    public AbstractResponse getPlayerActivityDetail(long playerId, ActivityData activityData, int detailId) {
        long activityId = activityData.getId();
        ResOfficialAwardsDetailInfo detailInfo = new ResOfficialAwardsDetailInfo(Code.SUCCESS);
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);

        detailInfo.detailInfo = new ArrayList<>();
        OfficialAwardsDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(activityId, baseCfgBeanMap.get(detailId), null);
        detailInfo.detailInfo.add(baseActivityDetailInfo);
        return detailInfo;
    }

    /**
     * 构建玩家官方派奖活动明细信息
     */
    @Override
    public OfficialAwardsDetailInfo buildPlayerActivityDetail(long activityId, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (baseCfgBean instanceof OfficialAwardsCfg cfg) {
            OfficialAwardsDetailInfo info = new OfficialAwardsDetailInfo();
            info.activityId = activityId;
            info.detailId = cfg.getId();
            info.type = cfg.getTurntableType();
            info.costNum = dataCache.getNeedPoints().getOrDefault(cfg.getCalculationType(), 0);
            info.rewardItems = ItemUtils.buildItemInfo(Map.of(cfg.getGetitem().getFirst(), (long) cfg.getGetitem().getLast()));
            return info;
        }
        return null;
    }

    @Override
    public void activityLoadCompleted(ActivityData activityData) {
        super.activityLoadCompleted(activityData);
    }

    /**
     * 请求记录官方派奖记录
     *
     * @param playerController 玩家控制器
     * @param req              请求
     * @return 记录响应
     */
    public AbstractResponse reqOfficialAwardsRecord(PlayerController playerController, ReqOfficialAwardsRecord req) {
        // 查询玩家或全局中奖记录（分页）
        ResOfficialAwardsRecord res = new ResOfficialAwardsRecord(Code.SUCCESS);
        Pair<Boolean, List<OfficialAwardsRecord>> playerRecordActivities = null;
        // type == 1：个人记录，type == 2：全局记录
        if (req.type == 1) {
            playerRecordActivities = officialAwardsDao.getPlayerRecord(playerController.playerId(),
                    req.startIndex, req.startIndex + Math.min(req.size, ActivityConstant.OfficialAwards.GET_MAX_RECORD_NUM));
        } else if (req.type == 2) {
            playerRecordActivities = officialAwardsDao.getAllRecords(req.startIndex, req.startIndex +
                    Math.min(req.size, ActivityConstant.OfficialAwards.GET_MAX_RECORD_NUM));
        }
        if (playerRecordActivities != null && CollectionUtil.isNotEmpty(playerRecordActivities.getSecond())) {
            res.recordList = new ArrayList<>();
            for (OfficialAwardsRecord record : playerRecordActivities.getSecond()) {
                OfficialAwardsShowRecord showRecord = new OfficialAwardsShowRecord();
                showRecord.recordTime = record.getCreateTime();
                showRecord.name = record.getName();
                showRecord.num = record.getGetNum();
                showRecord.type = record.getType();
                res.recordList.add(showRecord);
            }
            // 是否还有下一页（由 DAO 返回的布尔值）
            res.hasNext = playerRecordActivities.getFirst();
            res.startIndex = req.startIndex;
        }
        return res;
    }

    /**
     * 获取玩家官方派奖活动类型信息（前端展示）
     */
    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(long playerId, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResOfficialAwardsTypeInfo cardTypeInfo = new ResOfficialAwardsTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cardTypeInfo;
        }
        cardTypeInfo.activityData = new ArrayList<>();
        for (Map.Entry<Long, List<BaseActivityDetailInfo>> entry : allDetailInfo.entrySet()) {
            List<BaseActivityDetailInfo> baseActivityDetailInfos = entry.getValue();
            OfficialAwardsActivity officialAwardsType = new OfficialAwardsActivity();
            officialAwardsType.detailInfos = new ArrayList<>();
            cardTypeInfo.activityData.add(officialAwardsType);
            for (BaseActivityDetailInfo baseActivityDetailInfo : baseActivityDetailInfos) {
                if (baseActivityDetailInfo instanceof OfficialAwardsDetailInfo info) {
                    officialAwardsType.detailInfos.add(info);
                }
            }
            ActivityData activityData = activityManager.getActivityData().get(entry.getKey());
            officialAwardsType.tomorrowPoint = officialAwardsDao.getPlayerProgress(playerId, ActivityConstant.OfficialAwards.TOMORROW_POINTS);
            officialAwardsType.todayPoint = officialAwardsDao.getPlayerProgress(playerId, ActivityConstant.OfficialAwards.TODAY_POINTS);
            officialAwardsType.totalPool = officialAwardsDao.getTotalPool();
            officialAwardsType.remainTime = activityData.getTimeEnd() - System.currentTimeMillis();
            if (activityData.getActivityTempData() instanceof OfficialAwardsTempData data) {
                officialAwardsType.conversionType = data.getConversionType();
            }
        }
        return cardTypeInfo;
    }

    @Override
    public List<BaseCfgBean> getDetailCfgBean() {
        return new ArrayList<>(GameDataManager.getOfficialAwardsCfgList());
    }

    @Override
    public Class<OfficialAwardsCfg> getDetailDataClass() {
        return OfficialAwardsCfg.class;
    }

    @Override
    public void isLeader() {
//        if (activityManager.isExecutionNode()) {
            Map<Long, ActivityData> longActivityDataMap = activityManager.getActivityTypeData().get(ActivityType.OFFICIAL_AWARDS);
            for (ActivityData activityData : longActivityDataMap.values()) {
                if (activityData.canRun()) {
                    robotAction(activityData.getId());
                    break;
                }
            }
//        }
    }

    @Override
    public void notLeader() {

    }

    @Override
    public void onTimer(TimerEvent<Long> e) {
//        if (activityManager.isExecutionNode()) {
            //奖池为空直接返回
            long pool = officialAwardsDao.getTotalPool();
            if (pool <= 0) {
                return;
            }
            Long activityId = e.getParameter();
            ActivityData activityData = activityManager.getActivityData().get(activityId);
            if (!activityData.canRun()) {
                return;
            }
            //机器人进行中奖
            Map<Integer, BaseCfgBean> map = activityManager.getActivityDetailInfo().get(activityId);
            Iterator<BaseCfgBean> iterator = map.values().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            BaseCfgBean bean = iterator.next();
            if (bean instanceof OfficialAwardsCfg cfg) {
                WeightRandom<OfficialAwardsCfg> random = getOfficialAwardsCfgWeightRandom(map, cfg.getTurntableType());
                OfficialAwardsCfg next = random.next();
                Pair<Integer, Integer> pair = officialAwardsDao.reduceTotalPool(next.getGetitem().getLast());
                if (pair.getFirst() > 0) {
                    addRobotRecord(next, pair.getFirst());
                }
                if (pair.getSecond() > 0) {
                    robotAction(activityId);
                }
            }
//        }
    }

    /**
     * 请求官方派奖总奖池
     */
    public AbstractResponse reqOfficialAwardsTotalPool() {
        ResOfficialAwardsTotalPool resOfficialAwardsTotalPool = new ResOfficialAwardsTotalPool(Code.SUCCESS);
        resOfficialAwardsTotalPool.totalPool = officialAwardsDao.getTotalPool();
        return resOfficialAwardsTotalPool;
    }
}
