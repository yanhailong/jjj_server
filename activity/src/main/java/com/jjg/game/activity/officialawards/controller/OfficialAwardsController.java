package com.jjg.game.activity.officialawards.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.WeightRandom;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.officialawards.dao.OfficialAwardsDao;
import com.jjg.game.activity.officialawards.data.OfficialAwardsRecord;
import com.jjg.game.activity.officialawards.message.bean.OfficialAwardsActivity;
import com.jjg.game.activity.officialawards.message.bean.OfficialAwardsDetailInfo;
import com.jjg.game.activity.officialawards.message.bean.OfficialAwardsShowRecord;
import com.jjg.game.activity.officialawards.message.bean.OfficialAwardsStartInfo;
import com.jjg.game.activity.officialawards.message.req.ReqOfficialAwardsRecord;
import com.jjg.game.activity.officialawards.message.req.ReqOfficialAwardsTotalPool;
import com.jjg.game.activity.officialawards.message.res.*;
import com.jjg.game.activity.util.CronUtil;
import com.jjg.game.activity.util.DataCache;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.core.utils.RedisUtils;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.OfficialAwardsCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 官方派奖活动控制器
 *
 * @author lm
 * @date 2025/9/3
 */
@Component
public class OfficialAwardsController extends BaseActivityController {
    private final Logger log = LoggerFactory.getLogger(OfficialAwardsController.class);
    private final OfficialAwardsDao officialAwardsDao;
    private final DataCache dataCache;
    private final RobotUtil robotUtil;

    public OfficialAwardsController(OfficialAwardsDao officialAwardsDao, DataCache dataCache, RobotUtil robotUtil) {
        this.officialAwardsDao = officialAwardsDao;
        this.dataCache = dataCache;
        this.robotUtil = robotUtil;
    }

    @Override
    public boolean addPlayerProgress(Player player, ActivityData activityData, long progress, long activityTargetKey, Object additionalParameters) {
        long playerId = player.getId();
        BigDecimal realProgress = RedisUtils.fromLong(progress);
        //转换比例
        Pair<Integer, Integer> pair = dataCache.getRechargeConvertRatio();
        //计算增加的积分值
        int addValue = realProgress.multiply(BigDecimal.valueOf(pair.getSecond()))
                .divide(BigDecimal.valueOf(pair.getFirst()), RoundingMode.DOWN)
                .intValue();
        if (addValue > 0) {
            int incremented = officialAwardsDao.incrementPlayerProgress(playerId, addValue);
            activityLogger.sendOfficialAwardsLog(player, activityData, 1, 0
                    , addValue, incremented, 0, null, null);
            return hasRedDot(playerId, activityData);
        }
        return false;
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
        long pool = officialAwardsDao.getTotalPool(activityData.getId());
        if (pool <= 0) {
            res.code = Code.OFFICIAL_AWARDS_POOL_NULL;
            return res;
        }
        if (activityData.getValueParam().size() < 2) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        // 获取活动明细配置
        Map<Integer, OfficialAwardsCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        //获取消耗
        int needPoints = activityData.getValueParam().get(1).intValue() * times;
        //扣减积分
        int remainPoint = officialAwardsDao.reducePlayerProgress(playerId, needPoints);
        if (remainPoint < 0) {
            res.code = Code.NOT_ENOUGH;
            return res;
        }
        List<Long> getRewards = new ArrayList<>();
        Pair<Long, Long> reducedPair = new Pair<>(0L, 0L);
        OfficialAwardsCfg cfg = null;
        for (int i = 0; i < times; i++) {
            WeightRandom<OfficialAwardsCfg> random = getOfficialAwardsCfgWeightRandom(baseCfgBeanMap);
            //获取随机奖励
            cfg = random.next();
            if (CollectionUtil.isEmpty(cfg.getGetItem())) {
                continue;
            }
            Long getNum = cfg.getGetItem().values().iterator().next();
            reducedPair = officialAwardsDao.reduceTotalPool(activityData.getId(), getNum);
            if (reducedPair.getFirst() < 1) {
                break;
            }
            getRewards.add(reducedPair.getFirst());
            if (reducedPair.getSecond() <= 0) {
                break;
            }
        }
        if (cfg == null || getRewards.isEmpty()) {
            //奖池不足
            res.code = Code.OFFICIAL_AWARDS_POOL_NULL;
            return res;
        }
        long totalGet = getRewards.stream().mapToLong(Long::longValue).sum();
        //添加道具
        Integer id = cfg.getGetItem().keySet().iterator().next();
        CommonResult<ItemOperationResult> addResult = playerPackService.addItem(playerId, id, totalGet, AddType.ACTIVITY_OFFICIAL_AWARDS);
        if (!addResult.success()) {
            log.error("官方派奖玩家参加活动发奖失败 playerId:{} get:{}", playerId, totalGet);
            return res;
        }
        //发送日志
        activityLogger.sendOfficialAwardsLog(player, activityData, 2, activityData.getValueParam().getLast().intValue(), needPoints,
                remainPoint, reducedPair.getSecond(), addResult.data, Map.of(id, totalGet));
        addPlayerRecord(player, activityData.getId(), getRewards);
        res.infoList = new ArrayList<>();
        for (Long getReward : getRewards) {
            res.infoList.add(ItemUtils.buildItemInfo(id, getReward));
        }
        targetRobotAction(activityData, times);
        res.remainPoint = remainPoint;
        res.totalPool = reducedPair.getSecond();
        res.rewardDetailId = cfg.getId();
        return res;
    }

    /**
     * 触发机器人中奖
     *
     * @param activityData 活动数据
     */
    private void targetRobotAction(ActivityData activityData, int baseTimes) {
        try {
            int type = activityData.getValueParam().get(2).intValue();
            GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(34);
            if (globalConfigCfg == null || StringUtils.isEmpty(globalConfigCfg.getValue())) {
                return;
            }
            String[] split = StringUtils.split(globalConfigCfg.getValue(), "_");
            if (split.length != 3) {
                return;
            }
            int times = Integer.parseInt(split[type - 1]) * baseTimes;
            WeightRandom<Integer> robotRandom = dataCache.getRobotRandom();
            AtomicInteger actionCount = new AtomicInteger(times);
            WheelTimerUtil.scheduleAtFixedCount(() -> robotAction(activityData.getId(), actionCount), actionCount, robotRandom.next(), robotRandom, TimeUnit.MILLISECONDS);
            log.info("添加机器人定时任务成功 times:{}", times);
        } catch (Exception e) {
            log.error("刮刮乐 触发机器人中奖失败 activityId:{}", activityData.getId(), e);
        }
    }

    /**
     * 获取本轮配置的随机器
     *
     * @param baseCfgBeanMap 本轮配置
     * @return 随机器
     */
    private WeightRandom<OfficialAwardsCfg> getOfficialAwardsCfgWeightRandom(Map<Integer, OfficialAwardsCfg> baseCfgBeanMap) {
        // 构建权重随机器
        WeightRandom<OfficialAwardsCfg> random = new WeightRandom<>();
        for (OfficialAwardsCfg cfg : baseCfgBeanMap.values()) {
            random.add(cfg, cfg.getProbability());
        }
        return random;
    }

    /**
     * 添加机器人记录
     */
    private void addRobotRecord(long activityId, long robotGet) {
        RobotPlayer robotPlayer = robotUtil.randomRobotPlayer();
        OfficialAwardsRecord officialAwardsRecord = new OfficialAwardsRecord();
        officialAwardsRecord.setName(robotPlayer.getNickName());
        officialAwardsRecord.setCreateTime(System.currentTimeMillis());
        officialAwardsRecord.setGetNum(robotGet);
        //添加记录
        officialAwardsDao.savePlayerRecord(0, activityId, officialAwardsRecord);
    }

    /**
     * 添加玩家记录
     */
    private void addPlayerRecord(Player player, long activityId, List<Long> playerGetList) {
        for (Long playerGet : playerGetList) {
            OfficialAwardsRecord officialAwardsRecord = new OfficialAwardsRecord();
            officialAwardsRecord.setName(player.getNickName());
            officialAwardsRecord.setCreateTime(System.currentTimeMillis());
            officialAwardsRecord.setGetNum(playerGet);
            //添加记录
            officialAwardsDao.savePlayerRecord(player.getId(), activityId, officialAwardsRecord);
        }
    }


    /**
     * 官方派奖活动奖励领取接口（暂未实现）
     */
    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        return null;
    }

    /**
     * 清除所有数据
     */
    private void clearData(long activityId) {
        //清除所有记录数据
        officialAwardsDao.deleteAllRecords(activityId);
        log.info("官方派奖删除所有记录成功 activityId:{}", activityId);
        officialAwardsDao.deleteAllPlayerRecords(activityId);
        log.info("官方派奖删除所有玩家记录成功 activityId:{}", activityId);
        //清除奖池信息
        officialAwardsDao.deleteTotalPool(activityId);
        log.info("官方派奖删除总奖池成功 activityId:{}", activityId);
    }

    @Override
    public void onActivityStart(ActivityData activityData) {
        //开启时清除数据
        clearData(activityData.getId());
        //设置初始奖池
        List<Long> valueParam = activityData.getValueParam();
        if (CollectionUtil.isEmpty(valueParam) || valueParam.getFirst() == 0) {
            log.error("官方派奖活动未配置 主奖金");
            return;
        }
        officialAwardsDao.setTotalPool(activityData.getId(), valueParam.getFirst());
    }


    @Override
    public void onActivityEnd(ActivityData activityData) {
        try {
            clearData(activityData.getId());
        } catch (Exception e) {
            log.error("官方派奖活动结算，数据清除异常", e);
        }
    }


    /**
     * 获取玩家官方派奖活动明细
     */
    @Override
    public AbstractResponse getPlayerActivityDetail(Player player, ActivityData activityData, int detailId) {
        ResOfficialAwardsDetailInfo detailInfo = new ResOfficialAwardsDetailInfo(Code.SUCCESS);
        Map<Integer, OfficialAwardsCfg> baseCfgBeanMap = getDetailCfgBean(activityData);

        detailInfo.detailInfo = new ArrayList<>();
        OfficialAwardsDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(player, activityData, baseCfgBeanMap.get(detailId), null);
        detailInfo.detailInfo.add(baseActivityDetailInfo);
        return detailInfo;
    }

    @Override
    public boolean hasRedDot(long playerId, ActivityData activityData) {
        int playerProgress = officialAwardsDao.getPlayerProgress(playerId);
        if (playerProgress <= 0) {
            return false;
        }
        Map<Long, ActivityData> activityDataMap = activityManager.getActivityTypeData().get(activityData.getType());
        if (CollectionUtil.isEmpty(activityDataMap)) {
            return false;
        }
        for (ActivityData data : activityDataMap.values()) {
            int needPoints = activityData.getValueParam().get(1).intValue();
            if (playerProgress >= needPoints) {
                long totalPool = officialAwardsDao.getTotalPool(data.getId());
                if (totalPool > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 构建玩家官方派奖活动明细信息
     */
    @Override
    public OfficialAwardsDetailInfo buildPlayerActivityDetail(Player player, ActivityData activityData, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (baseCfgBean instanceof OfficialAwardsCfg cfg && activityData.canRun()) {
            OfficialAwardsDetailInfo info = new OfficialAwardsDetailInfo();
            info.activityId = activityData.getId();
            info.detailId = cfg.getId();
            info.rewardItems = ItemUtils.buildItemInfo(cfg.getGetItem());
            return info;
        }
        return null;
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
            playerRecordActivities = officialAwardsDao.getPlayerRecord(playerController.playerId(), req.activityId,
                    req.startIndex, req.startIndex + Math.min(req.size, ActivityConstant.OfficialAwards.GET_MAX_RECORD_NUM));
        } else if (req.type == 2) {
            playerRecordActivities = officialAwardsDao.getAllRecords(req.activityId, req.startIndex, req.startIndex +
                    Math.min(req.size, ActivityConstant.OfficialAwards.GET_MAX_RECORD_NUM));
        }
        if (playerRecordActivities != null && CollectionUtil.isNotEmpty(playerRecordActivities.getSecond())) {
            res.recordList = new ArrayList<>();
            for (OfficialAwardsRecord record : playerRecordActivities.getSecond()) {
                OfficialAwardsShowRecord showRecord = new OfficialAwardsShowRecord();
                showRecord.recordTime = record.getCreateTime();
                showRecord.name = record.getName();
                showRecord.num = record.getGetNum();
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
    public AbstractResponse getPlayerActivityInfoByTypeRes(Player player, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResOfficialAwardsTypeInfo cardTypeInfo = new ResOfficialAwardsTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cardTypeInfo;
        }
        cardTypeInfo.activityData = new ArrayList<>();
        for (Map.Entry<Long, List<BaseActivityDetailInfo>> entry : allDetailInfo.entrySet()) {
            List<BaseActivityDetailInfo> baseActivityDetailInfos = entry.getValue();
            OfficialAwardsActivity officialAwardsActivity = new OfficialAwardsActivity();
            officialAwardsActivity.detailInfos = new ArrayList<>();
            cardTypeInfo.activityData.add(officialAwardsActivity);
            for (BaseActivityDetailInfo baseActivityDetailInfo : baseActivityDetailInfos) {
                if (baseActivityDetailInfo instanceof OfficialAwardsDetailInfo info) {
                    officialAwardsActivity.detailInfos.add(info);
                }
            }
            ActivityData activityData = activityManager.getActivityData().get(entry.getKey());
            officialAwardsActivity.totalPool = officialAwardsDao.getTotalPool(activityData.getId());
            if (activityData.getValueParam().size() >= 4) {
                officialAwardsActivity.costPoint = activityData.getValueParam().get(1).intValue();
                officialAwardsActivity.turntableType = activityData.getValueParam().get(2).intValue();
                officialAwardsActivity.showType = activityData.getValueParam().get(3).intValue();
            }
            if (activityData.canRun()) {
                officialAwardsActivity.remainTime = activityData.getTimeEnd() - System.currentTimeMillis();
            }
            officialAwardsActivity.startInfos = getOfficialAwardsStartInfo(activityData);
            officialAwardsActivity.remainPoints = officialAwardsDao.getPlayerProgress(player.getId());
            officialAwardsActivity.activityState = activityData.getStatus();
        }
        return cardTypeInfo;
    }

    public OfficialAwardsStartInfo getOfficialAwardsStartInfo(ActivityData activityData) {
        LocalDateTime offset = LocalDateTime.now();
        if (activityData.canRun()) {
            offset = TimeHelper.getLocalDateTime(activityData.getTimeEnd());
        }
        Pair<LocalDateTime, LocalDateTime> nextOpenTime = CronUtil.getNextOpenTime(activityData.getTimeStartCorn(), activityData.getTimeEndCorn(), offset);
        if (nextOpenTime == null) {
            return null;
        }
        OfficialAwardsStartInfo officialAwardsStartInfo = new OfficialAwardsStartInfo();
        officialAwardsStartInfo.startTime = TimeHelper.getTimestamp(nextOpenTime.getFirst());
        officialAwardsStartInfo.number = nextOpenTime.getFirst().getDayOfMonth();
        return officialAwardsStartInfo;
    }

    @Override
    public Map<Integer, OfficialAwardsCfg> getDetailCfgBean(ActivityData activityData) {
        return GameDataManager.getOfficialAwardsCfgList()
                .stream()
                .filter(cfg -> activityData.getValue().contains(cfg.getId()))
                .collect(Collectors.toMap(BaseCfgBean::getId, cfg -> cfg));
    }

    /**
     * 机器人中奖行为
     *
     * @param activityId  活动ID
     * @param actionCount
     */
    public void robotAction(long activityId, AtomicInteger actionCount) {
        PlayerExecutorGroupDisruptor.getDefaultExecutor()
                .tryPublish(0, 0, new BaseHandler<String>() {
                    @Override
                    public void action() {
                        ActivityData activityData = activityManager.getActivityData().get(activityId);
                        if (!activityData.canRun()) {
                            actionCount.set(0);
                            return;
                        }
                        //奖池为空直接返回
                        long pool = officialAwardsDao.getTotalPool(activityData.getId());
                        if (pool <= 0) {
                            actionCount.set(0);
                            return;
                        }
                        //机器人进行中奖
                        Map<Integer, OfficialAwardsCfg> map = getDetailCfgBean(activityData);
                        WeightRandom<OfficialAwardsCfg> random = getOfficialAwardsCfgWeightRandom(map);
                        OfficialAwardsCfg next = random.next();
                        if (CollectionUtil.isEmpty(next.getGetItem())) {
                            return;
                        }
                        Pair<Long, Long> pair = officialAwardsDao.reduceTotalPool(activityData.getId(), next.getGetItem().values().iterator().next());
                        if (pair.getFirst() > 0) {
                            addRobotRecord(activityData.getId(), pair.getFirst());
                        }
                    }
                }.setHandlerParamWithSelf("officialAwards robotAction"));
    }

    /**
     * 请求官方派奖总奖池
     */
    public AbstractResponse reqOfficialAwardsTotalPool(ReqOfficialAwardsTotalPool req) {
        ResOfficialAwardsTotalPool resOfficialAwardsTotalPool = new ResOfficialAwardsTotalPool(Code.SUCCESS);
        resOfficialAwardsTotalPool.totalPool = officialAwardsDao.getTotalPool(req.activityId);
        return resOfficialAwardsTotalPool;
    }
}
