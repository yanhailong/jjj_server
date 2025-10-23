package com.jjg.game.activity.officialawards.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.util.RandomUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
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
import com.jjg.game.common.listener.IGameClusterLeaderListener;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.OfficialAwardsCfg;
import com.jjg.game.sampledata.bean.RobotCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public boolean addPlayerProgress(Player player, ActivityData activityData, long progress, long activityTargetKey, Object additionalParameters) {
        long playerId = player.getId();
        //转换比例
        Pair<Integer, Integer> pair = dataCache.getRechargeConvertRatio();
        //计算增加的积分值
        int addValue = BigDecimal.valueOf(progress).multiply(BigDecimal.valueOf(pair.getSecond()))
                .divide(BigDecimal.valueOf(pair.getFirst()), RoundingMode.DOWN)
                .intValue();
        if (addValue > 0) {
            officialAwardsDao.incrementPlayerProgress(playerId, addValue);
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
        int needPoints = activityData.getValueParam().get(1) * times;
        //扣减积分
        int remainPoint = officialAwardsDao.reducePlayerProgress(playerId, needPoints);
        if (remainPoint < 0) {
            res.code = Code.NOT_ENOUGH;
            return res;
        }
        List<Integer> getRewards = new ArrayList<>();
        Pair<Integer, Integer> reducedPair = new Pair<>(0, 0);
        OfficialAwardsCfg cfg = null;
        for (int i = 0; i < times; i++) {
            WeightRandom<OfficialAwardsCfg> random = getOfficialAwardsCfgWeightRandom(baseCfgBeanMap);
            //获取随机奖励
            cfg = random.next();
            Integer getNum = cfg.getGetitem().getLast();
            reducedPair = officialAwardsDao.reduceTotalPool(activityData.getId(), getNum);
            if (reducedPair.getFirst() < 1) {
                break;
            }
            getRewards.add(reducedPair.getFirst());
        }
        if (cfg == null || getRewards.isEmpty()) {
            //奖池不足
            res.code = Code.OFFICIAL_AWARDS_POOL_NULL;
            return res;
        }
        int totalGet = getRewards.stream().mapToInt(Integer::intValue).sum();
        //添加道具
        CommonResult<ItemOperationResult> addResult = playerPackService.addItem(playerId, cfg.getGetitem().getFirst(), totalGet, "officialAwards");
        if (!addResult.success()) {
            log.error("官方派奖玩家参加活动发奖失败 playerId:{} get:{}", playerId, totalGet);
            return res;
        }
        addPlayerRecord(player, activityData.getId(), getRewards);
        res.infoList = new ArrayList<>();
        for (Integer getReward : getRewards) {
            res.infoList.add(ItemUtils.buildItemInfo(cfg.getGetitem().getFirst(), getReward));
        }
        res.remainPoint = remainPoint;
        res.totalPool = reducedPair.getSecond();
        res.rewardDetailId = cfg.getId();
        return res;
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
        RobotCfg robotCfg = RandomUtil.randomEle(GameDataManager.getRobotCfgList());
        OfficialAwardsRecord officialAwardsRecord = new OfficialAwardsRecord();
        officialAwardsRecord.setName(robotCfg.getName());
        officialAwardsRecord.setCreateTime(System.currentTimeMillis());
        officialAwardsRecord.setGetNum(robotGet);
        //添加记录
        officialAwardsDao.savePlayerRecord(0, activityId, officialAwardsRecord);
    }

    /**
     * 添加玩家记录
     */
    private void addPlayerRecord(Player player, long activityId, List<Integer> playerGetList) {
        for (Integer playerGet : playerGetList) {
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
        List<Integer> valueParam = activityData.getValueParam();
        if (CollectionUtil.isEmpty(valueParam) || valueParam.getFirst() == 0) {
            log.error("官方派奖活动未配置 主奖金");
            return;
        }
        officialAwardsDao.setTotalPool(activityData.getId(), valueParam.getFirst());
        //添加机器人获奖逻辑
        robotAction(activityData.getId());
    }

    @Override
    public void onActivityEnd(ActivityData activityData) {
        clearData(activityData.getId());
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
        ResOfficialAwardsDetailInfo detailInfo = new ResOfficialAwardsDetailInfo(Code.SUCCESS);
        Map<Integer, OfficialAwardsCfg> baseCfgBeanMap = getDetailCfgBean(activityData);

        detailInfo.detailInfo = new ArrayList<>();
        OfficialAwardsDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(activityData, baseCfgBeanMap.get(detailId), null);
        detailInfo.detailInfo.add(baseActivityDetailInfo);
        return detailInfo;
    }

    /**
     * 构建玩家官方派奖活动明细信息
     */
    @Override
    public OfficialAwardsDetailInfo buildPlayerActivityDetail(ActivityData activityData, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (baseCfgBean instanceof OfficialAwardsCfg cfg && activityData.canRun()) {
            OfficialAwardsDetailInfo info = new OfficialAwardsDetailInfo();
            info.activityId = activityData.getId();
            info.detailId = cfg.getId();
            info.rewardItems = ItemUtils.buildItemInfo(Map.of(cfg.getGetitem().getFirst(), (long) cfg.getGetitem().getLast()));
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
    public AbstractResponse getPlayerActivityInfoByTypeRes(long playerId, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
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
                officialAwardsActivity.costPoint = activityData.getValueParam().get(1);
                officialAwardsActivity.turntableType = activityData.getValueParam().get(2);
                officialAwardsActivity.showType = activityData.getValueParam().get(3);
            }
            if (activityData.canRun()) {
                officialAwardsActivity.remainTime = activityData.getTimeEnd() - System.currentTimeMillis();
            }
            officialAwardsActivity.startInfos = getOfficialAwardsStartInfo(activityData);
            officialAwardsActivity.remainPoints = officialAwardsDao.getPlayerProgress(playerId);
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

    @Override
    public void isLeader() {
        if (activityManager.isExecutionNode()) {
            Map<Long, ActivityData> longActivityDataMap = activityManager.getActivityTypeData().get(ActivityType.OFFICIAL_AWARDS);
            if (CollectionUtil.isEmpty(longActivityDataMap)) {
                return;
            }
            for (ActivityData activityData : longActivityDataMap.values()) {
                if (activityData.canRun()) {
                    robotAction(activityData.getId());
                    break;
                }
            }
        }
    }

    @Override
    public void notLeader() {

    }

    @Override
    public void onTimer(TimerEvent<Long> e) {
        if (activityManager.isExecutionNode()) {
            Long activityId = e.getParameter();
            ActivityData activityData = activityManager.getActivityData().get(activityId);
            if (!activityData.canRun()) {
                return;
            }
            //奖池为空直接返回
            long pool = officialAwardsDao.getTotalPool(activityData.getId());
            if (pool <= 0) {
                return;
            }
            //机器人进行中奖
            Map<Integer, OfficialAwardsCfg> map = getDetailCfgBean(activityData);
            WeightRandom<OfficialAwardsCfg> random = getOfficialAwardsCfgWeightRandom(map);
            OfficialAwardsCfg next = random.next();
            Pair<Integer, Integer> pair = officialAwardsDao.reduceTotalPool(activityData.getId(), next.getGetitem().getLast());
            if (pair.getFirst() > 0) {
                addRobotRecord(activityData.getId(), pair.getFirst());
            }
            if (pair.getSecond() > 0) {
                robotAction(activityId);
            }
        }
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
