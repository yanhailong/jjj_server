package com.jjg.game.activity.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.dao.PlayerActivityDao;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityTargetType;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.message.ActivityBuilder;
import com.jjg.game.activity.common.message.res.NotifyActivityChange;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.util.CronUtil;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.condition.ConditionNode;
import com.jjg.game.core.base.condition.ConditionParser;
import com.jjg.game.core.base.condition.MatchResult;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.conditionnode.AndNode;
import com.jjg.game.core.base.condition.conditionnode.AtomicNode;
import com.jjg.game.core.base.condition.conditionnode.NotNode;
import com.jjg.game.core.base.condition.conditionnode.OrNode;
import com.jjg.game.core.base.condition.event.BetEvent;
import com.jjg.game.core.base.condition.event.TimeEvent;
import com.jjg.game.core.base.gameevent.*;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.data.Account;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.listener.DropItemListener;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.manager.ConditionManager;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.manager.DropItemManager;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.ActivityItemDropInfo;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.utils.MessageBuildUtil;
import com.jjg.game.core.utils.RedisUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ActivityConfigCfg;
import com.jjg.game.sampledata.bean.DropConfigCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lm
 * @date 2025/9/3 18:16
 */
@Component
public class ActivityManager implements TimerListener<Long>, IPlayerLoginSuccess, GmListener, GameEventListener,
        ConfigExcelChangeListener, IRedDotService, DropItemListener {
    private static final Logger log = LoggerFactory.getLogger(ActivityManager.class);
    /**
     * 定时器中心，用于添加活动开始/结束的定时任务
     */
    private final TimerCenter timerCenter;
    /**
     * 集群系统，节点间消息广播
     */
    private final ClusterSystem clusterSystem;
    /**
     * 节点配置
     */
    private final NodeConfig nodeConfig;

    /**
     * 活动跑马灯管理器
     */
    private final CoreMarqueeManager marqueeManager;
    /**
     * 节点管理
     */
    private final MarsCurator marsCurator;
    /**
     * 活动 id -> 活动数据
     */
    private Map<Long, ActivityData> activityData = new ConcurrentHashMap<>();
    /**
     * 活动类型 -> (活动 id -> 活动数据)
     */
    private Map<ActivityType, Map<Long, ActivityData>> activityTypeData = new ConcurrentHashMap<>();

    /**
     * 红点管理
     */
    private final RedDotManager redDotManager;

    /**
     * 条件检查
     */
    private final ConditionManager conditionManager;
    /**
     * 开服时间（毫秒）
     */
    private long startServerTime;
    /**
     * 玩家获得数据dao
     */
    private final PlayerActivityDao playerActivityDao;


    /**
     * 掉落管理器
     */
    private final DropItemManager dropItemManager;

    /**
     * 计数dao(用于判断活动是否开启过)
     */
    private final CountDao countDao;

    /**
     * 条件解析
     */
    private final ConditionParser conditionParser;

    /**
     * 事件管理
     */
    private final GameEventManager gameEventManager;

    private Map<EGameEventType, List<ActivityData>> activityConditionCache = new HashMap<>();


    public ActivityManager(TimerCenter timerCenter, ClusterSystem clusterSystem,
                           CoreMarqueeManager marqueeManager,
                           MarsCurator marsCurator, NodeConfig nodeConfig, RedDotManager redDotManager,
                           ConditionManager conditionManager, PlayerActivityDao playerActivityDao,
                           DropItemManager dropItemManager, CountDao countDao, ConditionParser conditionParser,
                           GameEventManager gameEventManager) {
        this.timerCenter = timerCenter;
        this.clusterSystem = clusterSystem;
        this.marqueeManager = marqueeManager;
        this.marsCurator = marsCurator;
        this.nodeConfig = nodeConfig;
        this.redDotManager = redDotManager;
        this.conditionManager = conditionManager;
        this.playerActivityDao = playerActivityDao;
        this.dropItemManager = dropItemManager;
        this.countDao = countDao;
        this.conditionParser = conditionParser;
        this.gameEventManager = gameEventManager;
    }


    public Map<Long, ActivityData> getActivityData() {
        return activityData;
    }

    public Map<ActivityType, Map<Long, ActivityData>> getActivityTypeData() {
        return activityTypeData;
    }

    /**
     * 初始化活动数据
     */
    public void initData() {
        ActivityType.initialize();
        //添加开服时间
        checkStartServerTime();
        Map<Long, ActivityData> tempActivityData = new ConcurrentHashMap<>();
        //要添加定时器的列表 时间戳 活动id
        List<Pair<Long, Long>> timerList = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        //从配置表加载
        loadActivityConfigByExcel(tempActivityData, currentTime, timerList);
        addActivityTimer(timerList);
        //重新赋值到内存
        activityData = tempActivityData;
        activityTypeData = new ConcurrentHashMap<>();
        //缓存为活动类型->活动数据保存
        for (ActivityData data : tempActivityData.values()) {
            activityTypeData.computeIfAbsent(data.getType(), k -> new ConcurrentHashMap<>()).put(data.getId(), data);
        }
        //检查是否要主动开启
        for (ActivityData data : activityData.values()) {
            checkActivityStatus(data, currentTime);
        }
        if (CollectionUtil.isEmpty(activityConditionCache)) {
            loadActivityConditionCache();
            gameEventManager.registerEventListener(this);
        }
    }

    /**
     * 检查开服时间
     */
    private void checkStartServerTime() {
        long serverStartTime = TimeHelper.getCurrentDateZeroSecondTime();
        boolean ifAbsent = countDao.setIfAbsent(CountDao.CountType.SYSTEM.getParam(), "openServerTime", BigDecimal.valueOf(serverStartTime));
        if (ifAbsent) {
            startServerTime = serverStartTime;
        } else {
            startServerTime = countDao.getCount(CountDao.CountType.SYSTEM.getParam(), "openServerTime").longValue();
        }
    }

    /**
     * 添加活动定时器
     *
     * @param timerList 定时器列表
     */
    private void addActivityTimer(List<Pair<Long, Long>> timerList) {
        //添加活动定时器
        for (Pair<Long, Long> pair : timerList) {
            timerCenter.add(new TimerEvent<>(this, pair.getFirst(), pair.getSecond()));
        }
    }

    /**
     * 移除活动定时器
     *
     * @param activityId 活动id
     */
    private void removeActivityTimer(long activityId) {
        timerCenter.remove(this, activityId);
    }

    /**
     * 检查活动状态并执行活动开启
     *
     * @param data        活动数据
     * @param currentTime 当时间
     */
    private void checkActivityStatus(ActivityData data, long currentTime) {
        //设置状态
        if (data.getStatus() != ActivityConstant.ActivityStatus.RUNNING && data.getTimeStart() <= currentTime && currentTime <= data.getTimeEnd()) {
            activityOpenAction(data);
        }
        if (data.canRun()) {
            data.getType().getController().activityLoadCompleted(data);
        }
    }

    /**
     * 从excel加载活动数据
     *
     * @param tempActivityData 活动临时数据数据
     * @param currentTime      当前时间
     * @param timerList        定时器列表
     */
    private void loadActivityConfigByExcel(Map<Long, ActivityData> tempActivityData, long currentTime,
                                           List<Pair<Long, Long>> timerList) {
        List<ActivityConfigCfg> activityConfigCfgList = GameDataManager.getActivityConfigCfgList();
        for (ActivityConfigCfg activityConfigCfg : activityConfigCfgList) {
            ActivityType activityType = ActivityType.fromType(activityConfigCfg.getType());
            if (activityType == null) {
                continue;
            }
            ActivityData data = ActivityData.getActivityData(activityConfigCfg, activityType, startServerTime);
            checkActivityTimer(data, currentTime, timerList);
            long activityInfoId = activityConfigCfg.getId();
            tempActivityData.put(activityInfoId, data);
        }
    }


    /**
     * 检查活动数据
     *
     * @param data       活动数据
     * @param timeMillis 当前时间
     * @param timerList  需要添加定时器的timer列表
     */
    public void checkActivityTimer(ActivityData data, long timeMillis, List<Pair<Long, Long>> timerList) {
        //定时器添加判断
        if (!data.isOpen() || data.getStatus() == ActivityConstant.ActivityStatus.ENDED || data.getTimeEnd() < timeMillis) {
            return;
        }
        long activityInfoId = data.getId();
        //添加活动开始定时器
        if (data.getTimeStart() > timeMillis) {
            timerList.add(Pair.newPair(data.getTimeStart(), activityInfoId));
        }
        //添加活动结束定时器
        if (data.getTimeEnd() > timeMillis) {
            timerList.add(Pair.newPair(data.getTimeEnd(), activityInfoId));
        }
    }

    /**
     * 通过类型获取玩家能参加的活动数据
     *
     * @param player       玩家
     * @param activityType 活动类型
     * @return 活动信息
     */
    public ActivityData getOpenActivityData(Player player, ActivityType activityType) {
        Map<Long, ActivityData> activityDataMap = getActivityTypeData().get(activityType);
        List<ActivityData> list = activityDataMap.values().stream()
                .filter(activityData -> playerCanJoinActivity(activityData, player))
                .sorted(Comparator.comparing(ActivityData::getTimeStart).reversed())
                .toList();
        for (ActivityData data : list) {
            if (playerCanJoinActivity(data, player)) {
                return data;
            }
        }
        return null;
    }

    @Override
    public void onTimer(TimerEvent<Long> timerEvent) {
        Long activityId = timerEvent.getParameter();
        //获取活动数据
        ActivityData data = activityData.get(activityId);
        if (data == null) {
            return;
        }
        long timeMillis = System.currentTimeMillis();
        //活动开启
        if (data.getStatus() == ActivityConstant.ActivityStatus.NOT_START && timeMillis >= data.getTimeStart()) {
            log.info("活动activity:{} 开启 time:{}", activityId, data.getTimeStart());
            activityOpenAction(data);
        }
        //活动结束
        if (data.getStatus() == ActivityConstant.ActivityStatus.RUNNING && timeMillis >= data.getTimeEnd()) {
            log.info("活动activity:{} 进入结束阶段", activityId);
            switch (data.getOpenType()) {
                case ActivityConstant.Common.LIMIT_TYPE -> {
                    data.setStatus(ActivityConstant.ActivityStatus.ENDED);
                    //活动结束执行
                    if (addActivityStatusChangeCount(data.getId(), data.getTimeEnd())) {
                        data.getType().getController().onActivityEnd(data);
                    }
                    //推送活动变化
                    notifyNodeActivityChange(data);
                }
                case ActivityConstant.Common.OPEN_SERVER_TYPE -> {
                    if (addActivityStatusChangeCount(data.getId(), data.getTimeEnd())) {
                        data.getType().getController().onActivityEnd(data);
                    }
                    data.setTimeStart(data.getTimeEnd());
                    //计算结束时间
                    long timestampByDay = TimeHelper.getTimestampByDay(data.getTimeStart(), data.getDuration());
                    data.setTimeEnd(timestampByDay);
                    log.info("开服活动 activity:{} 下一次结束时间{}", activityId, data.getTimeEnd());
                    notifyNodeActivityChange(data);
                    //修改轮数
                    data.addRound();
                    //添加到定时器
                    addActivityTimer(List.of(Pair.newPair(data.getTimeEnd(), activityId)));
                }
                case ActivityConstant.Common.CYCLE_SERVER_TYPE -> {
                    data.setStatus(ActivityConstant.ActivityStatus.ENDED);
                    if (addActivityStatusChangeCount(data.getId(), data.getTimeEnd())) {
                        data.getType().getController().onActivityEnd(data);
                    }
                    //推送活动变化
                    notifyNodeActivityChange(data);
                    data.addRound();
                    //重新计算时间并添加到定时器
                    Pair<LocalDateTime, LocalDateTime> nextOpenTime = CronUtil.getNextOpenTime(data.getTimeStartCorn(), data.getTimeEndCorn(), LocalDateTime.now());
                    if (nextOpenTime != null) {
                        //设置下一轮的开始时间
                        data.setTimeStart(TimeHelper.getTimestamp(nextOpenTime.getFirst()));
                        log.info("循环活动 activity:{} 下一次开始时间{}", activityId, data.getTimeStart());
                        //设置下一轮的结束
                        data.setTimeEnd(TimeHelper.getTimestamp(nextOpenTime.getSecond()));
                        log.info("循环活动 activity:{} 下一次结束时间{}", activityId, data.getTimeEnd());
                        //设置定时器
                        addActivityTimer(List.of(Pair.newPair(data.getTimeEnd(), activityId),
                                Pair.newPair(data.getTimeStart(), activityId)));
                        data.setStatus(ActivityConstant.ActivityStatus.NOT_START);
                    }
                }
            }
            log.info("活动activity:{} 完成结束阶段", activityId);
        }

    }

    /**
     * 活动开始处理
     *
     * @param data 活动数据
     */
    private void activityOpenAction(ActivityData data) {
        if (addActivityStatusChangeCount(data.getId(), data.getTimeStart())) {
            //活动开始执行
            data.getType().getController().onActivityStart(data);
            log.info("活动开启 activityId:{} 开始时间:{} 结束时间:{} ", data.getId(), data.getTimeStart(), data.getTimeEnd());
            //活动开始发送跑马灯
            marqueeManager.activityMarquee(data.getMarquee());
        }
        data.setStatus(ActivityConstant.ActivityStatus.RUNNING);
        //推送活动变化
        notifyNodeActivityChange(data);
    }

    /**
     * 通知节点活动变化
     *
     * @param data 活动数据
     */
    public void notifyNodeActivityChange(ActivityData data) {
        NotifyActivityChange notifyActivityChange = new NotifyActivityChange();
        notifyActivityChange.activityInfos = new ArrayList<>();
        notifyActivityChange.activityInfos.add(ActivityBuilder.buildActivityInfo(data));
        clusterSystem.broadcastToOnlinePlayer(notifyActivityChange);
    }


    /**
     * 发送消息给指定玩家
     *
     * @param playerId 玩家id
     * @param msg      消息
     */
    public void sendToPlayer(long playerId, AbstractMessage msg) {
        clusterSystem.sendToPlayer(msg, playerId);
    }

    @Override
    public void onPlayerLoginSuccess(PlayerController playerController, Player player, Account account, boolean firstLogin) {
        NotifyActivityChange info = new NotifyActivityChange();
        info.activityInfos = new ArrayList<>();
        Boolean checked = playerActivityDao.checkCanTargetFirstLogin(player.getId());
        for (ActivityData data : activityData.values()) {
            if (!data.isOpen()) {
                continue;
            }
            BaseActivityController controller = data.getType().getController();
            if (!data.getType().isShowInNotOpen() && !data.canRun()) {
                continue;
            }
            info.activityInfos.add(controller.buildActivityInfo(data));
            if (!data.getType().getController().checkPlayerCanJoinActivity(player, data)) {
                continue;
            }
            //玩家首次登录执行
            if (firstLogin && Boolean.TRUE.equals(checked)) {
                controller.checkPlayerDataAndResetOnLogin(player.getId(), data);
            }
            //登录初始化进度
            controller.initProgress(player, data);
        }
        if (firstLogin && Boolean.TRUE.equals(checked)) {
            //触发登录活动
            addPlayerActivityProgress(player, ActivityTargetType.LOGIN.getTargetKey(), 1, null);
        }
        playerController.send(info);
    }


    /**
     * 玩家活动进度更新
     *
     * @param player               玩家数据
     * @param activityTargetKey    触发key
     * @param value                增加值
     * @param additionalParameters 额外参数
     */
    public void addPlayerActivityProgress(Player player, long activityTargetKey, long value, Object additionalParameters) {
        if (value <= 0) {
            return;
        }
        long playerId = player.getId();
        //获取需要增加的活动类型
        List<ActivityData> dataArrayList = new ArrayList<>();
        for (ActivityType activityType : ActivityType.values()) {
            if (!activityType.isCanAddPlayerProgress() || (activityType.getTargetKey() & activityTargetKey) == 0) {
                continue;
            }
            Map<Long, ActivityData> activityDataMap = activityTypeData.get(activityType);
            if (activityDataMap == null) {
                continue;
            }
            for (ActivityData data : activityDataMap.values()) {
                //检查活动参加条件 //首次登陆不检查
                if (!playerCanJoinActivity(data, player) && (activityTargetKey & ActivityTargetType.LOGIN.getTargetKey()) == 0) {
                    continue;
                }
                try {
                    boolean changeStatus = data.getType().getController().addPlayerProgress(player, data, value, activityTargetKey, additionalParameters);
                    //如果进度增加后能够领取则放入
                    if (changeStatus) {
                        dataArrayList.add(data);
                    }
                    if (activityType == ActivityType.OFFICIAL_AWARDS) {
                        break;
                    }
                } catch (Exception e) {
                    log.error("增加玩家活动进度失败 playerId:{} activityId:{} value:{}", playerId, data.getId(), value, e);
                }
            }
        }
        //通知红点
        if (CollectionUtil.isNotEmpty(dataArrayList)) {
            List<RedDotDetails> redInfo = new ArrayList<>();
            for (ActivityData data : dataArrayList) {
                RedDotDetails redDotDetails = new RedDotDetails();
                redDotDetails.setRedDotModule(getModule());
                redDotDetails.setRedDotType(RedDotDetails.RedDotType.COMMON);
                redDotDetails.setCount(1);
                redDotDetails.setRedDotSubmodule(data.getType().getType());
                redInfo.add(redDotDetails);
            }
            redDotManager.updateRedDot(redInfo, playerId);
        }
    }

    /**
     * 活动进度更新
     *
     * @param activityTargetKey    活动触发key
     * @param value                增加值
     * @param additionalParameters 额外参数
     */
    public void addActivityProgress(Player player, long activityTargetKey, long value, Object additionalParameters) {
        if (value <= 0) {
            return;
        }
        for (ActivityType activityType : ActivityType.values()) {
            if (!activityType.isCanAddActivityProgress() || (activityType.getTargetKey() & activityTargetKey) == 0) {
                continue;
            }
            Map<Long, ActivityData> activityDataMap = activityTypeData.get(activityType);
            if (activityDataMap == null) {
                continue;
            }
            for (ActivityData data : activityDataMap.values()) {
                //检查活动参加条件
                if (!playerCanJoinActivity(data, player)) {
                    continue;
                }
                try {
                    //获取该玩家的活动详细信息
                    data.getType().getController().addActivityProgress(data, value, additionalParameters);
                } catch (Exception e) {
                    log.error("增加活动进度失败  activityId:{} value:{}", data.getId(), value, e);
                }
            }
        }
    }

    /**
     * 参加活动
     *
     * @param player     玩家信息
     * @param activityId 活动id
     * @param detailId   活动详情ID
     * @param times      次数
     */
    public void joinActivity(Player player, long activityId, int detailId, int times) {
        long playerId = player.getId();
        try {
            //获取该玩家的活动详细信息
            ActivityData data = activityData.get(activityId);
            if (!playerCanJoinActivity(data, player)) {
                log.warn("玩家请求参加的活动未开始 playerId:{} activityId:{}  ", playerId, activityId);
                return;
            }
            AbstractResponse res = data.getType().getController().joinActivity(player, data, detailId, times);
            if (res != null) {
                //同步一次活动状态
                clusterSystem.sendToPlayer(res, playerId);
            }
            log.debug("joinActivityResp = {}",JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("玩家参加活动失败 playerId:{} activityId:{} ", playerId, activityId, e);
        }

    }

    /**
     * 获取节点在线玩家id
     *
     * @return 节点在线玩家id集合
     */
    public List<Long> getOnlinePlayerIds() {
        return clusterSystem.getAllPlayerIds();
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        String cmd = gmOrders[0];
        if ("rechargeGold".equalsIgnoreCase(cmd)) {
            long count = Long.parseLong(gmOrders[1]);
            addPlayerActivityProgress(playerController.getPlayer(), ActivityTargetType.RECHARGE.getTargetKey(), RedisUtils.toLong(BigDecimal.valueOf(count))
                    , null);
            return new CommonResult<>(Code.SUCCESS);
        }

        if (gmOrders.length < 3) {
            return new CommonResult<>(Code.FAIL);
        }
        if ("joinActivity".equalsIgnoreCase(cmd)) {
            Long activityId = Long.parseLong(gmOrders[1]);
            int detailId = Integer.parseInt(gmOrders[2]);
            ActivityData data = getActivityData().get(activityId);
            if (data == null) {
                return new CommonResult<>(Code.NOT_FOUND);
            }
            int times = 1;
            if (gmOrders.length == 4) {
                times = Integer.parseInt(gmOrders[3]);
            }
            if (times < 1) {
                return new CommonResult<>(Code.PARAM_ERROR);
            }
            joinActivity(playerController.getPlayer(), data.getId(), detailId, times);
            return new CommonResult<>(Code.SUCCESS);
        }
        if ("buyActivityGift".equalsIgnoreCase(cmd)) {
            Long activityId = Long.parseLong(gmOrders[1]);
            int detailId = Integer.parseInt(gmOrders[2]);
            ActivityData data = getActivityData().get(activityId);
            if (data == null) {
                return new CommonResult<>(Code.NOT_FOUND);
            }
            data.getType().getController().buyActivityGift(playerController.getPlayer(), data, detailId);
            return new CommonResult<>(Code.SUCCESS);
        }

        if ("claimActivityReward".equalsIgnoreCase(cmd)) {
            Long activityId = Long.parseLong(gmOrders[1]);
            int detailId = Integer.parseInt(gmOrders[2]);
            ActivityData data = getActivityData().get(activityId);
            if (data == null) {
                return new CommonResult<>(Code.NOT_FOUND);
            }
            int times = 1;
            if (gmOrders.length == 4) {
                times = Integer.parseInt(gmOrders[3]);
            }
            if (times < 1) {
                return new CommonResult<>(Code.PARAM_ERROR);
            }

            AbstractResponse res = data.getType().getController().claimActivityRewards(playerController.getPlayer(), data, detailId);
            if (res != null) {
                //同步一次活动状态
                clusterSystem.sendToPlayer(res, playerController.playerId());
            }
            return new CommonResult<>(Code.SUCCESS);
        }

        if ("getPlayerActivityInfoByType".equalsIgnoreCase(cmd)) {
            int typeId = Integer.parseInt(gmOrders[1]);
            Long activityId = Long.parseLong(gmOrders[2]);
            ActivityData data = getActivityData().get(activityId);
            if (data == null) {
                return new CommonResult<>(Code.NOT_FOUND);
            }

            ActivityType activityType = ActivityType.fromType(typeId);

            AbstractResponse res = data.getType().getController().getPlayerActivityInfoByType(playerController.getPlayer(), activityType);
            if (res != null) {
                //同步一次活动状态
                clusterSystem.sendToPlayer(res, playerController.playerId());
            }
            log.debug("res = {}",JSON.toJSONString(res));
            return new CommonResult<>(Code.SUCCESS);
        }

        return null;
    }

    /**
     * 判断是否是活动执行节点
     *
     * @return true 是活动执行节点
     */
    public boolean isExecutionNode() {
        return NodeType.HALL == NodeType.getNodeTypeByName(nodeConfig.getType()) && marsCurator.isMaster();
    }

    /**
     * 判断玩家是否剋参加活动
     *
     * @param data   活动数据
     * @param player 玩家数据
     * @return true 能参加活动 false不能参加活动
     */
    public boolean playerCanJoinActivity(ActivityData data, Player player) {
        if (data == null || player == null) {
            return false;
        }
        return data.canRun() && data.getType().getController().checkPlayerCanJoinActivity(player, data);
    }

    /**
     * 判断玩家是否能参加活动
     *
     * @param data   活动数据
     * @param player 玩家数据
     * @return 错误码
     */
    public boolean playerJoinActivityCheck(ActivityData data, Player player) {
        if (data == null || player == null) {
            return false;
        }
        return data.canRun() && conditionManager.isAchievementAndNotify(player, "", createTimeEvent(data), data.getCondition());
    }

    public TimeEvent createTimeEvent(ActivityData data) {
        TimeEvent event = new TimeEvent();
        event.setStartTime(data.getTimeStart());
        event.setEndTime(data.getTimeEnd());
        return event;
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        switch (gameEvent) {
            case PlayerEventCategory.PlayerRechargeEvent event -> {
                if (event.getGameEventType() != EGameEventType.RECHARGE) {
                    return;
                }
                //充值类型的扩大了100倍
                addPlayerActivityProgress(event.getPlayer(), ActivityTargetType.RECHARGE.getTargetKey(), RedisUtils.toLong(event.getOrder().getPrice()), event.getOrder());
            }
            case PlayerEvent playerEvent -> {
                Player player = playerEvent.getPlayer();
                if (playerEvent.getGameEventType() == EGameEventType.PLAYER_LEVEL) {
                    //添加其他活动进度
                    addPlayerActivityProgress(player, ActivityTargetType.LEVEL.getTargetKey(), player.getLevel(), playerEvent.getNewlyValue());
                }
                if (playerEvent.getGameEventType() == EGameEventType.BIND_PHONE) {
                    addPlayerActivityProgress(player, ActivityTargetType.BIND_PHONE.getTargetKey(), player.getLevel(), playerEvent.getNewlyValue());
                }
                //更新活动变化
                List<ActivityData> openActivityData = new ArrayList<>();
                List<ActivityData> dataList = activityConditionCache.get(playerEvent.getGameEventType());
                if (CollectionUtil.isNotEmpty(dataList)) {
                    for (ActivityData data : dataList) {
                        if (!data.canRun()) {
                            continue;
                        }
                        if (!conditionManager.isAchievement(player, "", createTimeEvent(data), data.getCondition())) {
                            continue;
                        }
                        openActivityData.add(data);
                    }
                    if (CollectionUtil.isNotEmpty(openActivityData)) {
                        NotifyActivityChange change = buildNotifyActivityChange(openActivityData);
                        sendToPlayer(player.getId(), change);
                    }
                }
            }
            case ClockEvent clockEvent -> {
                //0点事件
                if (clockEvent.getHour() == 0) {
                    onZeroEvent();
                }
            }
            default -> {
            }
        }

    }

    /**
     * 构建活动变化信息
     *
     * @param openActivityData 活动数据
     * @return 活动变化数据
     */
    private NotifyActivityChange buildNotifyActivityChange(List<ActivityData> openActivityData) {
        NotifyActivityChange change = new NotifyActivityChange();
        if (CollectionUtil.isNotEmpty(openActivityData)) {
            change.activityInfos = new ArrayList<>();
            for (ActivityData data : openActivityData) {
                change.activityInfos.add(ActivityBuilder.buildActivityInfo(data));
            }
        }
        return change;
    }

    /**
     * 活动0点事件处理
     */
    private void onZeroEvent() {
        //获取在线玩家
        List<PFSession> allPlayerPFSession = clusterSystem.getAllOnlinePlayerPFSession();
        if (CollectionUtil.isEmpty(allPlayerPFSession)) {
            return;
        }
        //节点全部在线玩家
        for (PFSession pfSession : allPlayerPFSession) {
            long playerId = pfSession.playerId;
            if (playerId <= 0 || !(pfSession.getReference() instanceof PlayerController playerController)) {
                continue;
            }
            Player player = playerController.getPlayer();
            if (player == null) {
                continue;
            }
            //分发到对应玩家线程处理
            PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(pfSession.getWorkId(), 0, new BaseHandler<String>() {
                @Override
                public void action() {
                    //确保极限情况下登录不多次触发
                    if (Boolean.FALSE.equals(playerActivityDao.checkCanTargetFirstLogin(playerId))) {
                        return;
                    }
                    log.info("玩家触发在线跨天 playerId:{}", player.getId());
                    //全部活动
                    for (ActivityData data : activityData.values()) {
                        BaseActivityController controller = data.getType().getController();
                        //检查是否能参加活动
                        if (!playerCanJoinActivity(data, player)) {
                            continue;
                        }
                        //重置活动数据
                        controller.checkPlayerDataAndResetOnLogin(player.getId(), data);
                    }
                    //触发登录活动
                    addPlayerActivityProgress(player, ActivityTargetType.LOGIN.getTargetKey(), 1, null);
                    log.info("玩家触发登陆行为完成 playerId:{}", player.getId());
                }
            }.setHandlerParamWithSelf("activity onZeroEvent"));
        }
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        Set<EGameEventType> set = new HashSet<>();
        set.add(EGameEventType.CLOCK_EVENT);
        set.add(EGameEventType.RECHARGE);
        if (CollectionUtil.isEmpty(activityConditionCache)) {
            loadActivityConditionCache();
        }
        set.addAll(activityConditionCache.keySet());
        return new ArrayList<>(set);
    }

    @Override
    public void initSampleCallbackCollector() {
        // 添加活动表监听
        addChangeSampleFileObserveWithCallBack(ActivityConfigCfg.EXCEL_NAME, this::reloadConfig);
    }

    /**
     * 重载配置
     */
    private void reloadConfig() {
        List<ActivityConfigCfg> activityConfigCfgList = GameDataManager.getActivityConfigCfgList();
        long currentTime = System.currentTimeMillis();
        List<ActivityData> changeData = new ArrayList<>();
        for (ActivityConfigCfg activityConfigCfg : activityConfigCfgList) {
            ActivityType activityType = ActivityType.fromType(activityConfigCfg.getType());
            if (activityType == null) {
                continue;
            }
            long activityInfoId = activityConfigCfg.getId();
            ActivityData oldData = activityData.get(activityInfoId);
            //非未开始的只能修改开启关闭状态
            if (oldData != null && oldData.getStatus() != ActivityConstant.ActivityStatus.NOT_START) {
                if (oldData.isOpen() != activityConfigCfg.getOpen()) {
                    //推送给前端
                    changeData.add(oldData);
                }
                oldData.setOpen(activityConfigCfg.getOpen());
                log.info("活动更新 正在进行中的活动只更新开关 activityId:{}", activityInfoId);
                continue;
            }
            List<Pair<Long, Long>> timerList = new ArrayList<>(2);
            //非未开始的 或者新增直接重新构建
            ActivityData data = ActivityData.getActivityData(activityConfigCfg, activityType, startServerTime);
            checkActivityTimer(data, currentTime, timerList);
            if (oldData != null) {
                //移除之前的定时器
                removeActivityTimer(oldData.getId());
                log.info("活动更新 移除老的定时器 activityId:{}", activityInfoId);
            }
            //添加新的定时器
            addActivityTimer(timerList);
            //检查活动是否要立即开启
            checkActivityStatus(data, currentTime);
            //重新放入活动数据中
            activityData.put(activityInfoId, data);
            //更新类型活动数据
            Map<Long, ActivityData> dataMap = activityTypeData.computeIfAbsent(data.getType(), key -> new ConcurrentHashMap<>());
            dataMap.put(activityInfoId, data);
            changeData.add(data);
            log.info("活动更新成功 activityId:{} activityData:{}", activityInfoId, JSON.toJSONString(activityData));
        }
        if (CollectionUtil.isNotEmpty(changeData)) {
            NotifyActivityChange notifyActivityChange = buildNotifyActivityChange(changeData);
            clusterSystem.broadcastToOnlinePlayer(notifyActivityChange);
        }
        loadActivityConditionCache();
        gameEventManager.registerEventListener(this);
    }

    /**
     * 活动开启或者关闭时进行计数
     *
     * @param activityId 活动id
     * @param time       时间
     */
    public final boolean addActivityStatusChangeCount(long activityId, long time) {
        return countDao.setIfAbsent(CountDao.CountType.ACTIVITY_STATUS.getParam().formatted(activityId), String.valueOf(time));
    }

    /**
     * 获取所属模块{@link RedDotDetails.RedDotModule}
     */
    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.ACTIVITY;
    }

    /**
     * 初始化红点信息
     *
     * @param playerId  玩家id
     * @param submodule 子模块
     *                  </p>
     *                  (如果指定了子模块则加载子模块数据,没有则加载所有子模块)
     */
    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        if (CollectionUtil.isEmpty(activityData)) {
            return List.of();
        }
        Map<Long, ActivityData> activityDataMap = null;
        //全活动红点
        if (submodule == 0) {
            activityDataMap = activityData;
        } else {
            //指定活动类型红点
            ActivityType activityType = ActivityType.fromType(submodule);
            if (activityType != null) {
                activityDataMap = activityTypeData.get(activityType);
            }
        }
        //没有数据直接返回
        if (CollectionUtil.isEmpty(activityDataMap)) {
            return List.of();
        }
        List<RedDotDetails> redDotDetails = new ArrayList<>();
        for (ActivityData data : activityDataMap.values()) {
            //判断该活动是否有红点
            boolean redDot = data.getType().getController().hasRedDot(playerId, data);
            if (redDot) {
                RedDotDetails redDotDetailInfo = new RedDotDetails();
                redDotDetailInfo.setRedDotModule(getModule());
                redDotDetailInfo.setRedDotType(RedDotDetails.RedDotType.COMMON);
                redDotDetailInfo.setCount(1);
                redDotDetailInfo.setRedDotSubmodule(data.getType().getType());
                redDotDetails.add(redDotDetailInfo);
            }
        }
        return redDotDetails;
    }

    @Override
    public List<ActivityItemDropInfo> dropItem(Player player, Object param) {
        if (player == null) {
            log.error("触发掉落时 玩家为null");
            return List.of();
        }
        if (param instanceof PlayerEventCategory.PlayerEffectiveFlowingEvent effectiveFlowingEvent) {
            //只支持有效流水条件检查
            BetEvent effectiveParam = BetEvent.getPlayerEffectiveParam(effectiveFlowingEvent);
            if (effectiveParam == null) {
                return List.of();
            }
            // 检查道具掉落
            List<ActivityData> activityIdList = activityData.values()
                    .stream()
                    .filter(data -> data.getDropId() > 0)
                    .toList();
            List<ActivityItemDropInfo> itemDropInfos = new ArrayList<>();
            for (ActivityData activityData : activityIdList) {
                long activityId = activityData.getId();
                // 需要判断活动是否开启
                if (!playerCanJoinActivity(activityData, player)) {
                    continue;
                }
                DropConfigCfg dropConfigCfg = GameDataManager.getDropConfigCfg(activityData.getDropId());
                if (dropConfigCfg == null) {
                    continue;
                }
                String dropCondition = dropConfigCfg.getDropCondition();
                if (StringUtils.isEmpty(dropCondition)) {
                    continue;
                }
                //每个活动单独计数
                String key = CountDao.CountType.ACTIVITY_CONDITIONS.getParam().formatted(activityData.getId());
                long triggerTimes = 0;
                MatchResultData matchResultData = conditionManager.addProgressAndGetAchievements(player, effectiveParam, key, dropCondition);
                if (matchResultData.result() == MatchResult.MATCH) {
                    triggerTimes = matchResultData.achieveTimes();
                }
                log.debug("activity id: {} 参数：{} checkRes: {}", activityId, JSON.toJSONString(effectiveParam), triggerTimes);
                if (triggerTimes > 0) {
                    // 触发次数
                    // 触发掉落逻辑
                    Map<Integer, Long> dropItems = dropItemManager.triggerDropItem(player, AddType.ACTIVITY, activityData.getId() + "", activityData.getDropId(), (int) triggerTimes, effectiveFlowingEvent);
                    if (!dropItems.isEmpty()) {
                        ActivityItemDropInfo activityItemDropInfo = MessageBuildUtil.buildActivityDropInfo(activityData.getType().getType(), activityId, effectiveFlowingEvent.getGameCfgId(), dropItems);
                        itemDropInfos.add(activityItemDropInfo);
                        log.info("玩家：{} 在活动中：{} 游戏：{} 产生有效流水：{} 产出道具：{}",
                                player.getId(), activityId, effectiveFlowingEvent.getGameCfgId(),
                                effectiveParam.getBetAmount(), JSON.toJSONString(dropItems));
                    }
                }
            }
            return itemDropInfos;
        }
        return List.of();
    }

    private void loadActivityConditionCache() {
        Map<EGameEventType, List<ActivityData>> tempMap = new HashMap<>();
        for (ActivityData data : activityData.values()) {
            ConditionNode node = conditionParser.parse(data.getCondition());
            analysisCondition(data, node, tempMap);
        }
        activityConditionCache = tempMap;
    }

    private void analysisCondition(ActivityData activityData, ConditionNode node, Map<EGameEventType, List<ActivityData>> tmpGameTypeOfFuncCache) {
        switch (node) {
            case AtomicNode<?> atomicNode -> {
                String type = atomicNode.getHandler().type();
                // 获取游戏事件类型
                EGameEventType gameEventType = EGameEventType.gameEventType(type);
                if (gameEventType == null) {
                    log.error("活动表配置异常，配置的事件触发类型：{} 在游戏事件枚举中缺失", type);
                    return;
                }
                tmpGameTypeOfFuncCache.computeIfAbsent(gameEventType, k -> new ArrayList<>()).add(activityData);
            }
            case AndNode andNode ->
                    andNode.getChildren().forEach(child -> analysisCondition(activityData, child, tmpGameTypeOfFuncCache));
            case OrNode orNode ->
                    orNode.getChildren().forEach(child -> analysisCondition(activityData, child, tmpGameTypeOfFuncCache));
            case NotNode notNode -> analysisCondition(activityData, notNode.getChild(), tmpGameTypeOfFuncCache);
            default -> {
            }
        }
    }
}
