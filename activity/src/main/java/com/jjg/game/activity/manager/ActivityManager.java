package com.jjg.game.activity.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.dao.PlayerActivityDao;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityTargetType;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.common.message.res.NotifyActivityChange;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.util.CronUtil;
import com.jjg.game.common.cluster.ClusterSystem;
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
import com.jjg.game.core.base.condition.ConditionType;
import com.jjg.game.core.base.condition.check.record.PlayerEffectiveParam;
import com.jjg.game.core.base.gameevent.*;
import com.jjg.game.core.base.gameevent.PlayerEventCategory.PlayerEffectiveFlowingEvent;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.manager.ConditionManager;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.manager.DropItemManager;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.ActivityItemDropInfo;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.core.pb.NotifyItemDropInfo;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ActivityConfigCfg;
import com.jjg.game.sampledata.bean.DropConfigCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lm
 * @date 2025/9/3 18:16
 */
@Component
public class ActivityManager implements TimerListener<Long>, IPlayerLoginSuccess, GmListener, GameEventListener,
        ConfigExcelChangeListener, IRedDotService {
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
     * 事件管理
     */
    private final GameEventManager gameEventManager;
    /**
     * 条件检查
     */
    private final ConditionManager conditionManager;
    /**
     * 开服时间（毫秒）
     */
    private final long startServerTime = 1756656000000L;
    /**
     * 玩家获得数据dao
     */
    private final PlayerActivityDao playerActivityDao;
    /**
     * 掉落管理器
     */
    private final DropItemManager dropItemManager;


    public ActivityManager(TimerCenter timerCenter, ClusterSystem clusterSystem,
                           CoreMarqueeManager marqueeManager,
                           MarsCurator marsCurator, NodeConfig nodeConfig, RedDotManager redDotManager, GameEventManager gameEventManager, ConditionManager conditionManager, PlayerActivityDao playerActivityDao, DropItemManager dropItemManager) {
        this.timerCenter = timerCenter;
        this.clusterSystem = clusterSystem;
        this.marqueeManager = marqueeManager;
        this.marsCurator = marsCurator;
        this.nodeConfig = nodeConfig;
        this.redDotManager = redDotManager;
        this.gameEventManager = gameEventManager;
        this.conditionManager = conditionManager;
        this.playerActivityDao = playerActivityDao;
        this.dropItemManager = dropItemManager;
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
        ActivityType.intialize();
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
            activityOpenAction(data);
        }
        //活动结束
        if (data.getStatus() == ActivityConstant.ActivityStatus.RUNNING && timeMillis >= data.getTimeEnd()) {
            switch (data.getOpenType()) {
                case ActivityConstant.Common.LIMIT_TYPE -> {
                    data.setStatus(ActivityConstant.ActivityStatus.ENDED);
                    //活动结束执行
                    if (isExecutionNode()) {
                        data.getType().getController().onActivityEnd(data);
                    }
                    //推送活动变化
                    notifyNodeActivityChange(data);
                }
                case ActivityConstant.Common.OPEN_SERVER_TYPE -> {
                    data.setTimeStart(data.getTimeEnd());
                    //计算结束时间
                    long timestampByDay = TimeHelper.getTimestampByDay(data.getTimeStart(), data.getDuration());
                    data.setTimeEnd(timestampByDay);
                    if (isExecutionNode()) {
                        //活动结束执行
                        data.getType().getController().onActivityEnd(data);
                    }
                    notifyNodeActivityChange(data);
                    //修改轮数
                    data.addRound();
                    //添加到定时器
                    addActivityTimer(List.of(Pair.newPair(data.getTimeEnd(), activityId)));
                }
                case ActivityConstant.Common.CYCLE_SERVER_TYPE -> {
                    data.setStatus(ActivityConstant.ActivityStatus.ENDED);
                    //活动结束执行
                    if (isExecutionNode()) {
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
                        //设置下一轮的结束
                        data.setTimeEnd(TimeHelper.getTimestamp(nextOpenTime.getFirst()));
                        //设置定时器
                        addActivityTimer(List.of(Pair.newPair(data.getTimeEnd(), activityId),
                                Pair.newPair(data.getTimeStart(), activityId)));
                        data.setStatus(ActivityConstant.ActivityStatus.NOT_START);
                    }
                }
            }
        }

    }

    /**
     * 活动开始处理
     *
     * @param data 活动数据
     */
    private void activityOpenAction(ActivityData data) {
        if (isExecutionNode()) {
            //活动开始执行
            data.getType().getController().onActivityStart(data);
            log.info("活动开启 activityId:{}", data.getId());
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
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.activityType = data.getType().getType();
        activityInfo.activityId = data.getId();
        activityInfo.status = data.getStatus();
        notifyActivityChange.activityInfos = new ArrayList<>();
        notifyActivityChange.activityInfos.add(activityInfo);
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
    public void onPlayerLoginSuccess(PlayerController playerController, Player player, boolean firstLogin) {
        NotifyActivityChange info = new NotifyActivityChange();
        info.activityInfos = new ArrayList<>();
        Boolean checked = playerActivityDao.checkCanTargetFirstLogin(player.getId());
        for (ActivityData data : activityData.values()) {
            BaseActivityController controller = data.getType().getController();
            if (!playerCanJoinActivity(data, playerController.getPlayer())) {
                continue;
            }
            //玩家首次登录执行
            if (firstLogin && Boolean.TRUE.equals(checked)) {
                controller.checkPlayerDataAndResetOnLogin(player.getId(), data);
            }
            info.activityInfos.add(controller.buildActivityInfo(data));
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
    public void addPlayerActivityProgress(Player player, long activityTargetKey, long value,
                                          Object additionalParameters) {
        if (value <= 0) {
            return;
        }
        long playerId = player.getId();
        //获取需要增加的活动类型
        for (ActivityType activityType : ActivityType.values()) {
            if (!activityType.isCanAddPlayerProgress() || (activityType.getTargetKey() & activityTargetKey) == 0) {
                continue;
            }
            Map<Long, ActivityData> activityDataMap = activityTypeData.get(activityType);
            if (activityDataMap == null) {
                continue;
            }
            List<ActivityData> dataArrayList = new ArrayList<>();
            for (ActivityData data : activityDataMap.values()) {
                //检查活动参加条件
                if (!playerCanJoinActivity(data, player)) {
                    continue;
                }
                try {
                    boolean changeStatus = data.getType().getController().addPlayerProgress(player, data, value
                            , activityTargetKey, additionalParameters);
                    //如果进度增加后能够领取则放入
                    if (changeStatus) {
                        dataArrayList.add(data);
                    }
                } catch (Exception e) {
                    log.error("增加玩家活动进度失败 playerId:{} activityId:{} value:{}", playerId, data.getId(), value, e);
                }
            }
            //通知红点
            if (CollectionUtil.isNotEmpty(dataArrayList)) {
                List<RedDotDetails> redInfo = new ArrayList<>();
                for (ActivityData data : dataArrayList) {
                    RedDotDetails redDotDetails = new RedDotDetails();
                    redDotDetails.setRedDotModule(getModule());
                    redDotDetails.setRedDotType(RedDotDetails.RedDotType.COMMON);
                    redDotDetails.setRedDotSubmodule(data.getType().getType());
                    redInfo.add(redDotDetails);
                }
                redDotManager.updateRedDot(redInfo, playerId);
            }
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
            if (data == null || !data.canRun()) {
                log.warn("玩家请求参加的活动未开始 playerId:{} activityId:{}  ", playerId, activityId);
                return;
            }
            AbstractResponse res = data.getType().getController().joinActivity(player, data, detailId, times);
            if (res != null) {
                //同步一次活动状态
                clusterSystem.sendToPlayer(res, playerId);
            }
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
            addPlayerActivityProgress(playerController.getPlayer(), ActivityTargetType.RECHARGE.getTargetKey(), count
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

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        switch (gameEvent) {
            // 产生有效流水 需要检查是否能掉落道具
            case PlayerEffectiveFlowingEvent event -> {
                // 道具掉落检查
                checkDropItem(event);
            }
            case PlayerEvent playerEvent -> {
                if (playerEvent.getGameEventType() == EGameEventType.PLAYER_LEVEL) {
                    Player player = playerEvent.getPlayer();
                    //添加其他活动进度
                    addPlayerActivityProgress(player, ActivityTargetType.LEVEL.getTargetKey(), player.getLevel(), playerEvent.getNewlyValue());
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
     * 活动0点事件处理
     */
    private void onZeroEvent() {
        //获取在线玩家
        List<Long> allPlayerIds = clusterSystem.getAllPlayerIds();
        if (CollectionUtil.isEmpty(allPlayerIds)) {
            return;
        }
        //节点全部在线玩家
        for (Long playerId : allPlayerIds) {
            PFSession session = clusterSystem.getSession(playerId);
            //判断玩家是否存在
            if (session != null && session.getReference() instanceof PlayerController playerController) {
                Player player = playerController.getPlayer();
                if (player == null) {
                    continue;
                }
                //确保极限情况下登录不多次触发
                if (Boolean.FALSE.equals(playerActivityDao.checkCanTargetFirstLogin(playerId))) {
                    continue;
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
                log.info("玩家触发登陆行为 playerId:{}", player.getId());
            }
        }
    }

    /**
     * 检查道具掉落
     */
    private void checkDropItem(PlayerEffectiveFlowingEvent effectiveFlowingEvent) {
        Player player = effectiveFlowingEvent.getPlayer();
        // 检查道具掉落
        List<ActivityData> activityIdList = activityData.values()
                .stream()
                .filter(data -> data.getDropId() > 0)
                .toList();
        int gameCfgId = effectiveFlowingEvent.getGameCfgId();
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(gameCfgId);
        if (warehouseCfg == null) {
            return;
        }
        List<ActivityItemDropInfo> itemDropInfos = new ArrayList<>();
        //只支持有效流水下注
        PlayerEffectiveParam param = new PlayerEffectiveParam();
        param.setPlayerId(player.getId());
        param.setGameId(gameCfgId);
        param.setGameType(warehouseCfg.getGameType());
        param.setRoomType(warehouseCfg.getRoomType());
        param.setFunction(ConditionType.FunctionType.ACTIVITY.name());
        if (effectiveFlowingEvent.getEventChangeValue() instanceof Long value) {
            param.setParamList(List.of(value));
        }
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
            List<String> dropCondition = dropConfigCfg.getDropCondition();
            if (CollectionUtil.isEmpty(dropCondition)) {
                continue;
            }
            long triggerTimes = Long.MAX_VALUE;

            for (String condition : dropCondition) {
                triggerTimes = Math.min(triggerTimes, conditionManager.addProgressAndGetAchievements(player, param, condition, false));
            }
            log.debug("activity id: {} 参数：{} checkRes: {}", activityId, JSON.toJSONString(param), triggerTimes);
            if (triggerTimes > 0) {
                //删除进度值
                for (String condition : dropCondition) {
                    conditionManager.reduceProgress(param, condition, triggerTimes);
                }
                // 触发次数
                // 触发掉落逻辑
                List<Item> dropItems = dropItemManager.triggerDropItem(player, "Activity", activityData.getId(), activityData.getDropId(), (int) triggerTimes, effectiveFlowingEvent);
                if (!dropItems.isEmpty()) {
                    ActivityItemDropInfo activityItemDropInfo = buildActivityDropInfo(activityData, gameCfgId, dropItems);
                    itemDropInfos.add(activityItemDropInfo);
                    log.info("玩家：{} 在活动中：{} 游戏：{} 产生有效流水：{} 产出道具：{}",
                            player.getId(), activityId, gameCfgId,
                            param.getParamList().getFirst(), dropItems);
                }
            }
        }
        // 如果有掉落
        if (!itemDropInfos.isEmpty()) {
            NotifyItemDropInfo notifyItemDropInfo = new NotifyItemDropInfo();
            notifyItemDropInfo.itemDropInfos = itemDropInfos;
            log.debug("玩家：{} 发送掉落数据：{}", player.getId(), JSON.toJSONString(notifyItemDropInfo));
            PFSession pfSession = clusterSystem.getSession(player.getId());
            // 发送道具掉落信息
            pfSession.send(notifyItemDropInfo);
        }
    }

    /**
     * 构建活动掉落信息
     */
    private ActivityItemDropInfo buildActivityDropInfo(ActivityData activityData, int gameCfgId, List<Item> dropItems) {
        ActivityItemDropInfo activityItemDropInfo = new ActivityItemDropInfo();
        activityItemDropInfo.activityType = activityData.getType().getType();
        activityItemDropInfo.activityId = activityData.getId();
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(gameCfgId);
        activityItemDropInfo.gameType = warehouseCfg.getGameType();
        Map<Integer, Long> itemMap = new HashMap<>();
        for (Item dropItem : dropItems) {
            itemMap.put(dropItem.getId(), itemMap.getOrDefault(dropItem.getId(), 0L) + dropItem.getItemCount());
        }
        activityItemDropInfo.itemMap =
                itemMap.entrySet().stream().map(item -> {
                    KVInfo kvInfo = new KVInfo();
                    kvInfo.key = item.getKey();
                    kvInfo.value = item.getValue().intValue();
                    return kvInfo;
                }).toList();
        return activityItemDropInfo;
    }


    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.PLAYER_LEVEL, EGameEventType.CLOCK_EVENT, EGameEventType.EFFECTIVE_FLOWING);
    }

    @Override
    public void initSampleCallbackCollector() {
        // 添加活动表监听
        addChangeSampleFileObserveWithCallBack(
                ActivityConfigCfg.EXCEL_NAME, () -> gameEventManager.registerEventListener(this))
                .addInitSampleFileObserveWithCallBack(
                        ActivityConfigCfg.EXCEL_NAME, () -> gameEventManager.registerEventListener(this));
        //活动热更
        addChangeSampleFileObserveWithCallBack(ActivityConfigCfg.EXCEL_NAME, this::reloadConfig);
    }

    /**
     * 重载配置
     */
    private void reloadConfig() {
        List<ActivityConfigCfg> activityConfigCfgList = GameDataManager.getActivityConfigCfgList();
        long currentTime = System.currentTimeMillis();
        for (ActivityConfigCfg activityConfigCfg : activityConfigCfgList) {
            ActivityType activityType = ActivityType.fromType(activityConfigCfg.getType());
            if (activityType == null) {
                continue;
            }
            long activityInfoId = activityConfigCfg.getId();
            ActivityData oldData = activityData.get(activityInfoId);
            //非未开始的只能修改开启关闭状态
            if (oldData != null && oldData.getStatus() != ActivityConstant.ActivityStatus.NOT_START) {
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
            log.info("活动更新成功 activityId:{} activityData:{}", activityInfoId, JSON.toJSONString(activityData));
        }
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
                redDotDetailInfo.setRedDotSubmodule(data.getType().getType());
                redDotDetails.add(redDotDetailInfo);
            }
        }
        return redDotDetails;
    }

}
