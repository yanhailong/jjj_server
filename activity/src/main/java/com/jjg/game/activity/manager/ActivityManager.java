package com.jjg.game.activity.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.dao.ActivityDao;
import com.jjg.game.activity.common.dao.ActivityDetailDao;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityTargetType;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.common.message.res.NotifyActivityChange;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.StrConstant;
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
import com.jjg.game.core.base.condition.CheckParamCategory.EffectiveFlowingParam;
import com.jjg.game.core.base.condition.ConditionCheckService;
import com.jjg.game.core.base.drop.ConditionProgressKeyCons;
import com.jjg.game.core.base.drop.DropItemDao;
import com.jjg.game.core.base.drop.DropItemLogger;
import com.jjg.game.core.base.drop.ItemDropDataHolder;
import com.jjg.game.core.base.gameevent.*;
import com.jjg.game.core.base.gameevent.PlayerEventCategory.PlayerEffectiveFlowingEvent;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.ActivityItemDropInfo;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.core.pb.NotifyItemDropInfo;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ActivityConfigCfg;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.ConditionCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
     * 活动数据 DAO（活动基本信息存储）
     */
    private final ActivityDao activityDao;
    /**
     * 活动详细数据 DAO（活动子项配置存储）
     */
    private final ActivityDetailDao activityDetailDao;
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
     * 活动 id -> 活动详细配置
     */
    private Map<Long, Map<Integer, BaseCfgBean>> activityDetailInfo = new ConcurrentHashMap<>();
    // 事件类型 => 活动ID列表
    private final Map<EGameEventType, List<Integer>> eventTypeListMap = new HashMap<>();
    /**
     * 红点管理
     */
    private final RedDotManager redDotManager;
    @Autowired
    private GameEventManager gameEventManager;
    @Autowired
    private ConditionCheckService conditionCheckService;
    @Autowired
    private DropItemDao dropItemDao;
    /**
     * 开服时间（毫秒）
     */
    private final long startServerTime = 1756656000000L;
    @Autowired
    private ItemDropDataHolder itemDropDataHolder;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private DropItemLogger dropItemLogger;

    public ActivityManager(TimerCenter timerCenter, ActivityDao activityDao,
                           ActivityDetailDao activityDetailDao, ClusterSystem clusterSystem,
                           CoreMarqueeManager marqueeManager,
                           MarsCurator marsCurator, NodeConfig nodeConfig, RedDotManager redDotManager) {
        this.timerCenter = timerCenter;
        this.activityDao = activityDao;
        this.activityDetailDao = activityDetailDao;
        this.clusterSystem = clusterSystem;
        this.marqueeManager = marqueeManager;
        this.marsCurator = marsCurator;
        this.nodeConfig = nodeConfig;
        this.redDotManager = redDotManager;
    }


    public Map<Long, ActivityData> getActivityData() {
        return activityData;
    }

    public Map<Long, Map<Integer, BaseCfgBean>> getActivityDetailInfo() {
        return activityDetailInfo;
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
        Map<Long, Map<Integer, BaseCfgBean>> tempActivityDetailInfo = new ConcurrentHashMap<>();
        //要添加定时器的列表 时间戳 活动id
        List<Pair<Long, Long>> timerList = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        //从数据库加载
        loadActivityConfigByDB(currentTime, timerList, tempActivityDetailInfo, tempActivityData);
        //从配置表加载
        loadActivityConfigByExcel(tempActivityData, currentTime, timerList, tempActivityDetailInfo);
        //添加活动定时器
        for (Pair<Long, Long> pair : timerList) {
            timerCenter.add(new TimerEvent<>(this, pair.getFirst(), pair.getSecond()));
        }
        //主节点保存到redis
        if (isExecutionNode()) {
            activityDao.saveActivities(tempActivityData);
            for (Map.Entry<Long, Map<Integer, BaseCfgBean>> entry : tempActivityDetailInfo.entrySet()) {
                activityDetailDao.saveActivityDetails(entry.getKey(), entry.getValue());
            }
        }
        //重新赋值到内存
        activityData = tempActivityData;
        activityDetailInfo = tempActivityDetailInfo;
        activityTypeData = new ConcurrentHashMap<>();
        //缓存为活动类型->活动数据保存
        for (ActivityData data : tempActivityData.values()) {
            activityTypeData.computeIfAbsent(data.getType(), k -> new ConcurrentHashMap<>()).put(data.getId(), data);
        }
        //检查是否要主动开启
        for (ActivityData data : activityData.values()) {
            //设置状态
            if (data.getStatus() != ActivityConstant.ActivityStatus.RUNNING && data.getTimeStart() <= currentTime && currentTime <= data.getTimeEnd()) {
                data.getType().getController().onActivityStart(data);
                data.setStatus(ActivityConstant.ActivityStatus.RUNNING);
            }
            if (data.canRun()) {
                data.getType().getController().activityLoadCompleted(data);
            }
        }
    }

    /**
     * 从excel加载活动数据
     *
     * @param tempActivityData       活动临时数据数据
     * @param currentTime            当前时间
     * @param timerList              定时器列表
     * @param tempActivityDetailInfo 活动详情临时数据
     */
    private void loadActivityConfigByExcel(Map<Long, ActivityData> tempActivityData, long currentTime, List<Pair<Long
            , Long>> timerList, Map<Long, Map<Integer, BaseCfgBean>> tempActivityDetailInfo) {
        List<ActivityConfigCfg> activityConfigCfgList = GameDataManager.getActivityConfigCfgList();
        for (ActivityConfigCfg activityConfigCfg : activityConfigCfgList) {
            ActivityType activityType = ActivityType.fromType(activityConfigCfg.getType());
            if (activityType == null || tempActivityData.containsKey((long) activityConfigCfg.getId())) {
                continue;
            }
            ActivityData data = ActivityData.getActivityData(activityConfigCfg, activityType);
            if (!checkActivityData(data, currentTime, timerList)) {
                continue;
            }
            long activityInfoId = activityConfigCfg.getId();
            Map<Integer, BaseCfgBean> loadedDetailData =
                    data.getType().getController().loadDetailData(activityDetailInfo.get(activityInfoId));
            if (CollectionUtil.isNotEmpty(loadedDetailData)) {
                //移除未配置在ActivityConfigCfg中的活动详情
                Iterator<Integer> iterator = loadedDetailData.keySet().iterator();
                while (iterator.hasNext()) {
                    Integer id = iterator.next();
                    if (data.getValue().contains(id)) {
                        continue;
                    }
                    iterator.remove();
                }
                tempActivityDetailInfo.put(activityInfoId, loadedDetailData);
            }
            tempActivityDetailInfo.put(activityInfoId, loadedDetailData);
            tempActivityData.put(activityInfoId, data);
        }
    }

    /**
     * 从数据库加载活动数据
     *
     * @param timeMillis             当前时间
     * @param timerList              定时器列表
     * @param tempActivityDetailInfo 临时活动详情数据
     * @param tempActivityData       临时活动数据
     */
    private void loadActivityConfigByDB(long timeMillis, List<Pair<Long, Long>> timerList, Map<Long, Map<Integer,
            BaseCfgBean>> tempActivityDetailInfo, Map<Long, ActivityData> tempActivityData) {
        List<ActivityData> allActivityInfos = activityDao.getAllActivityInfos();
        for (ActivityData data : allActivityInfos) {
            if (!checkActivityData(data, timeMillis, timerList)) {
                continue;
            }
            long activityInfoId = data.getId();
            //获取详细配置信息
            Map<Integer, BaseCfgBean> activityDetailInfos = activityDetailDao.getActivityDetailInfos(activityInfoId,
                    data.getType());
            if (CollectionUtil.isNotEmpty(activityDetailInfos)) {
                tempActivityDetailInfo.put(activityInfoId, activityDetailInfos);
            }
            tempActivityData.put(activityInfoId, data);
        }
    }

    /**
     * 定时更新活动数据到redis
     */
    @Scheduled(cron = "0 0 0/6 * * ?")
    private void saveActivity() {
        if (isExecutionNode()) {
            Map<Long, ActivityData> dataHashMap = new HashMap<>(activityData);
            activityDao.saveActivities(dataHashMap);
            Map<Long, Map<Integer, BaseCfgBean>> longMapHashMap = new HashMap<>(activityDetailInfo);
            for (Map.Entry<Long, Map<Integer, BaseCfgBean>> longMapEntry : longMapHashMap.entrySet()) {
                activityDetailDao.saveActivityDetails(longMapEntry.getKey(), longMapEntry.getValue());
            }
        }
    }


    /**
     * 检查活动数据
     *
     * @param data       活动数据
     * @param timeMillis 当前时间
     * @param timerList  需要添加定时器的timer列表
     * @return ture 将会添加到活动中
     */
    public boolean checkActivityData(ActivityData data, long timeMillis, List<Pair<Long, Long>> timerList) {
        if (!data.isOpen() || data.getStatus() == ActivityConstant.ActivityStatus.ENDED) {
            return false;
        }
        //通过活动类型判断
        if (data.getOpenType() == ActivityConstant.Common.OPEN_SERVER_TYPE) {
            //开服 开始时间戳为0设置为开服时间，结束时间戳为持续时间的时间戳 添加结束时间
            if (data.getTimeEnd() < timeMillis) {
                long timestampByDay = TimeHelper.getTimestampByDay(startServerTime, data.getDuration());
                data.setTimeStart(startServerTime);
                data.setTimeEnd(timestampByDay);
            }
        } else if (data.getOpenType() == ActivityConstant.Common.LIMIT_TYPE && timeMillis > data.getTimeEnd()) {
            return false;
        }
        //当前时间大于结束直接不添加到活动中
        if (data.getTimeEnd() < timeMillis) {
            return false;
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
        return true;
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
            data.setStatus(ActivityConstant.ActivityStatus.RUNNING);
            //活动开始执行
            data.getType().getController().onActivityStart(data);
            //活动开始发送跑马灯
            marqueeManager.activityMarquee(data.getMarquee());
            //推送活动变化
            notifyNodeActivityChange(data);
        }
        //活动结束
        if (data.getStatus() == ActivityConstant.ActivityStatus.RUNNING && timeMillis >= data.getTimeEnd()) {
            //限时活动
            if (data.getOpenType() == ActivityConstant.Common.LIMIT_TYPE) {
                data.setStatus(ActivityConstant.ActivityStatus.ENDED);
                //活动结束执行
                data.getType().getController().onActivityEnd(data);
                //推送活动变化
                notifyNodeActivityChange(data);
            } else if (data.getOpenType() == ActivityConstant.Common.OPEN_SERVER_TYPE) {
                //开发活动
                //修改轮数
                data.addRound();
                data.setTimeStart(data.getTimeEnd());
                //计算结束时间
                long timestampByDay = TimeHelper.getTimestampByDay(startServerTime, data.getDuration());
                data.setTimeEnd(timestampByDay);
                //活动结束执行
                data.getType().getController().onActivityEnd(data);
                notifyNodeActivityChange(data);
            }
        }

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
        for (ActivityData data : activityData.values()) {
            BaseActivityController controller = data.getType().getController();
            if (!playerCanJoinActivity(data, playerController.getPlayer())) {
                continue;
            }
            //玩家首次登录执行
            if (firstLogin) {
                controller.checkPlayerDataAndReset(player.getId(), data);
            }
            info.activityInfos.add(controller.buildActivityInfo(player.getId(), data));
        }
        if (firstLogin) {
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
                    boolean canClaim = data.getType().getController().addPlayerProgress(playerId, data, value,
                            additionalParameters);
                    //如果进度增加后能够领取则放入
                    if (canClaim) {
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
        if ("reload".equalsIgnoreCase(cmd)) {
            Map<Long, ActivityData> tempActivityData = new ConcurrentHashMap<>();
            Map<Long, Map<Integer, BaseCfgBean>> tempActivityDetailInfo = new ConcurrentHashMap<>();
            //要添加定时器的列表 时间戳 活动id
            List<Pair<Long, Long>> timerList = new ArrayList<>();
            long currentTime = System.currentTimeMillis();
            activityDetailInfo.clear();
            //从配置表加载
            loadActivityConfigByExcel(tempActivityData, currentTime, timerList, tempActivityDetailInfo);
            //添加活动定时器
            for (Pair<Long, Long> pair : timerList) {
                timerCenter.add(new TimerEvent<>(this, pair.getFirst(), pair.getSecond()));
            }
            //重新赋值到内存
            activityData = tempActivityData;
            activityDetailInfo = tempActivityDetailInfo;
            activityTypeData = new ConcurrentHashMap<>();
            //缓存为活动类型->活动数据保存
            for (ActivityData data : tempActivityData.values()) {
                activityTypeData.computeIfAbsent(data.getType(), k -> new ConcurrentHashMap<>()).put(data.getId(), data);
            }
            //检查是否要主动开启
            for (ActivityData data : activityData.values()) {
                //设置状态
                if (data.getStatus() != ActivityConstant.ActivityStatus.RUNNING && data.getTimeStart() <= currentTime && currentTime <= data.getTimeEnd()) {
                    data.getType().getController().onActivityStart(data);
                    data.setStatus(ActivityConstant.ActivityStatus.RUNNING);
                }
                if (data.canRun()) {
                    data.getType().getController().activityLoadCompleted(data);
                }
            }
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
        if ("recharge".equalsIgnoreCase(cmd)) {
            long count = Long.parseLong(gmOrders[1]);
            addPlayerActivityProgress(playerController.getPlayer(), ActivityTargetType.RECHARGE.getTargetKey(), count
                    , null);
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
        // 产生有效流水 需要检查是否能掉落道具
        if (gameEvent instanceof PlayerEffectiveFlowingEvent playerEffectiveFlowingEvent) {
            // 道具掉落检查
            checkDropItem(playerEffectiveFlowingEvent);
        }
        if (gameEvent instanceof PlayerEvent playerEvent && playerEvent.getGameEventType() == EGameEventType.PLAYER_LEVEL) {
            Player player = playerEvent.getPlayer();
            //添加其他活动进度
            addPlayerActivityProgress(player, ActivityTargetType.LEVEL.getTargetKey(), player.getLevel(), null);
        }
    }

    /**
     * 检查道具掉落
     */
    private void checkDropItem(PlayerEffectiveFlowingEvent effectiveFlowingEvent) {
        Player player = effectiveFlowingEvent.getPlayer();
        // 检查道具掉落
        List<Map.Entry<Long, ActivityData>> activityIdList =
                activityData.entrySet().stream()
                        .filter(entry ->
                                !CollectionUtils.isEmpty(entry.getValue().getDropCondition()) && !CollectionUtils.isEmpty(entry.getValue().getDropId()))
                        .toList();
        List<ActivityItemDropInfo> itemDropInfos = new ArrayList<>();
        for (Map.Entry<Long, ActivityData> activityDataEntry : activityIdList) {
            long activityId = activityDataEntry.getKey();
            ActivityData activityData = activityDataEntry.getValue();
            // 需要判断活动是否开启
            if (!playerCanJoinActivity(activityData, player)) {
                continue;
            }
            List<Integer> dropCondition = activityData.getDropCondition();
            // 条件key
            String conditionKey =
                    ConditionProgressKeyCons.BET_EFFECTIVE_FLOWING + player.getId() + StrConstant.COLON + activityId;
            ConditionCfg cfg = GameDataManager.getConditionCfg(dropCondition.getFirst());
            EffectiveFlowingParam effectiveFlowingParam = new EffectiveFlowingParam(cfg.getConditionType(), null);
            effectiveFlowingParam.setFlowingValue((Long) effectiveFlowingEvent.getEventChangeValue());
            effectiveFlowingParam.setGameCfgId(effectiveFlowingParam.getGameCfgId());
            effectiveFlowingParam.setConditionProgressKey(conditionKey);
            effectiveFlowingParam.setNeedUpdateProgress(true);
            effectiveFlowingParam.setConditionCfg(dropCondition.subList(1, dropCondition.size()));
            // 检查活动进度是否达到
            boolean triggerRes =
                    conditionCheckService.isTriggerComplete(player, cfg, Collections.singletonList(effectiveFlowingParam));
            log.debug("activity id: {} 参数：{} checkRes: {}",
                    activityId, JSON.toJSONString(effectiveFlowingParam), triggerRes);
            if (triggerRes) {
                // 触发次数
                int triggerTimes = effectiveFlowingParam.getTriggerTimes();
                // 触发掉落逻辑
                List<Item> dropItems = triggerDropItem(player, activityData, triggerTimes, effectiveFlowingEvent);
                if (!dropItems.isEmpty()) {
                    ActivityItemDropInfo activityItemDropInfo =
                            buildActivityDropInfo(activityData, effectiveFlowingEvent.getGameCfgId(), dropItems);
                    itemDropInfos.add(activityItemDropInfo);
                    log.info("玩家：{} 在活动中：{} 游戏：{} 产生有效流水：{} 产出道具：{}",
                            player.getId(), activityId, effectiveFlowingEvent.getGameCfgId(),
                            effectiveFlowingParam.getFlowingValue(), dropItems);
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

    /**
     * 触发道具掉落
     */
    private List<Item> triggerDropItem(
            Player player, ActivityData activityData, int triggerTimes, PlayerEffectiveFlowingEvent event) {
        Map<Integer, Integer> itemDropGroupCounter = dropItemDao.getItemDropGroupCounter(player.getId());
        if (itemDropGroupCounter == null) {
            itemDropGroupCounter = new HashMap<>();
        }
        List<Item> dropItems = new ArrayList<>();
        // 随机N次
        for (int i = 0; i < triggerTimes; i++) {
            // 获取当前活动的掉落配置
            List<Integer> dropIdList = new ArrayList<>(activityData.getDropId());
            Map<Integer, Integer> finalItemDropGroupCounter = itemDropGroupCounter;
            // 先排除已经不能掉落的分组ID
            dropIdList.removeIf(dropGroupId -> {
                int useTimes = finalItemDropGroupCounter.getOrDefault(dropGroupId, 0);
                int limitTimes = itemDropDataHolder.getDropGroupLimit(dropGroupId);
                return useTimes >= limitTimes;
            });
            // 根据分组配置，获取对应的子包组ID 分组ID <=> 道具ID
            List<Pair<Integer, Item>> randDropItems =
                    itemDropDataHolder.randDropItems(dropIdList, itemDropGroupCounter);
            if (!CollectionUtils.isEmpty(randDropItems)) {
                dropItems.addAll(randDropItems.stream().map(Pair::getSecond).toList());
            }
        }
        if (dropItems.isEmpty()) {
            return dropItems;
        }
        // 更新道具掉落使用map
        dropItemDao.updateItemDropGroupCounter(player.getId(), itemDropGroupCounter);
        // 添加道具
        CommonResult<ItemOperationResult> result =
                playerPackService.addItems(player.getId(), dropItems, "ACTIVITY_DROP_ITEM");
        if (result.success()) {
            // 记录日志
            dropItemLogger.recordDropItem(player, activityData.getId(), event.getGameCfgId(), result.data);
        }
        return dropItems;
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        // 全监听
        return Arrays.stream(EGameEventType.values()).toList();
    }

    @Override
    public void initSampleCallbackCollector() {
        // 添加活动表监听
        addChangeSampleFileObserveWithCallBack(
                ActivityConfigCfg.EXCEL_NAME, () -> gameEventManager.registerEventListener(this))
                .addInitSampleFileObserveWithCallBack(
                        ActivityConfigCfg.EXCEL_NAME, () -> gameEventManager.registerEventListener(this));
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
