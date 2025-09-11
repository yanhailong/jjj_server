package com.jjg.game.activity.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.dao.ActivityDao;
import com.jjg.game.activity.common.dao.ActivityDetailDao;
import com.jjg.game.activity.common.dao.PlayerActivityDao;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityTargetType;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.common.message.res.NotifyActivityChange;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ActivityConfigCfg;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lm
 * @date 2025/9/3 18:16
 */
@Component
public class ActivityManager implements TimerListener<Long>, IPlayerLoginSuccess, GmListener {
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
     * 玩家活动数据 DAO
     */
    private final PlayerActivityDao playerActivityDao;
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

    /**
     * 开服时间（毫秒）
     */
    private final long startServerTime = 1756656000000L;

    public ActivityManager(TimerCenter timerCenter, ActivityDao activityDao,
                           ActivityDetailDao activityDetailDao, ClusterSystem clusterSystem,
                           PlayerActivityDao playerActivityDao, CoreMarqueeManager marqueeManager,
                           MarsCurator marsCurator) {
        this.timerCenter = timerCenter;
        this.activityDao = activityDao;
        this.activityDetailDao = activityDetailDao;
        this.clusterSystem = clusterSystem;
        this.playerActivityDao = playerActivityDao;
        this.marqueeManager = marqueeManager;
        this.marsCurator = marsCurator;
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
        long timeMillis = System.currentTimeMillis();
        //从数据库加载
        List<ActivityData> allActivityInfos = activityDao.getAllActivityInfos();
        for (ActivityData data : allActivityInfos) {
            if (!checkActivityData(data, timeMillis, timerList)) {
                continue;
            }
            long activityInfoId = data.getId();
            //获取详细配置信息
            Map<Integer, BaseCfgBean> activityDetailInfos = activityDetailDao.getActivityDetailInfos(activityInfoId, data.getType());
            if (CollectionUtil.isNotEmpty(activityDetailInfos)) {
                tempActivityDetailInfo.put(activityInfoId, activityDetailInfos);
            }
            tempActivityData.put(activityInfoId, data);
        }
        //从配置表加载
        List<ActivityConfigCfg> activityConfigCfgList = GameDataManager.getActivityConfigCfgList();
        for (ActivityConfigCfg activityConfigCfg : activityConfigCfgList) {
            ActivityType activityType = ActivityType.fromType(activityConfigCfg.getType());
            if (activityType == null || tempActivityData.containsKey((long) activityConfigCfg.getId())) {
                continue;
            }
            ActivityData data = ActivityData.getActivityData(activityConfigCfg, activityType);
            if (!checkActivityData(data, timeMillis, timerList)) {
                continue;
            }
            long activityInfoId = activityConfigCfg.getId();
            Map<Integer, BaseCfgBean> loadedDetailData = data.getType().getController().loadDetailData(activityDetailInfo.get(activityInfoId));
            if (CollectionUtil.isNotEmpty(loadedDetailData)) {
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
        //添加定时器
        for (Pair<Long, Long> pair : timerList) {
            timerCenter.add(new TimerEvent<>(this, pair.getFirst(), pair.getSecond()));
        }

        //主节点保存到redis
        if (marsCurator.isMaster()) {
            activityDao.saveActivities(tempActivityData);
            for (Map.Entry<Long, Map<Integer, BaseCfgBean>> entry : tempActivityDetailInfo.entrySet()) {
                activityDetailDao.saveActivityDetails(entry.getKey(), entry.getValue());
            }
        }
        activityData = tempActivityData;
        activityDetailInfo = tempActivityDetailInfo;
        activityTypeData = new ConcurrentHashMap<>();
        for (ActivityData data : tempActivityData.values()) {
            activityTypeData.computeIfAbsent(data.getType(), k -> new ConcurrentHashMap<>()).put(data.getId(), data);
        }
        //检查是否要主动开启
        for (ActivityData data : activityData.values()) {
            //设置状态
            if (data.getStatus() != ActivityConstant.ActivityStatus.RUNNING && data.getTimeStart() <= timeMillis && timeMillis <= data.getTimeEnd()) {
                data.getType().getController().onActivityStart(data);
                data.setStatus(ActivityConstant.ActivityStatus.RUNNING);
            }
            if (data.canRun()) {
                data.getType().getController().activityLoadCompleted(data);
            }
        }
    }

    /**
     * 定时更新
     */
    @Scheduled(cron = "0 0 * * * ? ")
    private void saveActivity() {
        if (marsCurator.isMaster()) {
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
        if (data.getOpenType() == 1) {
            //开服 开始时间戳为0设置为开服时间，结束时间戳为持续时间的时间戳 添加结束时间
            if (data.getTimeEnd() < timeMillis) {
                long timestampByDay = TimeHelper.getTimestampByDay(startServerTime, data.getDuration());
                data.setTimeStart(startServerTime);
                data.setTimeEnd(timestampByDay);
            }
        } else if (data.getOpenType() == 2 && timeMillis > data.getTimeEnd()) {
            return false;
        }
        if (data.getTimeEnd() < timeMillis) {
            return false;
        }
        long activityInfoId = data.getId();
        //定时器添加
        if (data.getTimeStart() > timeMillis) {
            timerList.add(Pair.newPair(data.getTimeStart(), activityInfoId));
        }
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
        //判断是开启还是结束
        long timeMillis = System.currentTimeMillis();
        //开启
        if (data.getStatus() == ActivityConstant.ActivityStatus.NOT_START && timeMillis >= data.getTimeStart()) {
            data.setStatus(ActivityConstant.ActivityStatus.RUNNING);
            data.getType().getController().onActivityStart(data);
            //活动开始
            marqueeManager.activityMarquee(data.getMarquee());
            //推送活动变化
            notifyNodeActivityChange(data);
        }
        //结束
        if (data.getStatus() == ActivityConstant.ActivityStatus.RUNNING && timeMillis >= data.getTimeEnd()) {
            //限时
            if (data.getOpenType() == 2) {
                data.setStatus(ActivityConstant.ActivityStatus.ENDED);
                data.getType().getController().onActivityEnd(data);
                //推送活动变化
                notifyNodeActivityChange(data);
            } else if (data.getOpenType() == 1) {
                //修改轮数
                data.addRound();
                data.setTimeStart(data.getTimeEnd());
                long timestampByDay = TimeHelper.getTimestampByDay(startServerTime, data.getDuration());
                data.setTimeEnd(timestampByDay);
                data.getType().getController().onActivityEnd(data);
                notifyNodeActivityChange(data);
            }
        }

    }

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
    public void onPlayerLoginSuccess(PlayerController playerController, Player player) {
        NotifyActivityChange info = new NotifyActivityChange();
        info.activityInfos = new ArrayList<>();
        for (ActivityData data : activityData.values()) {
            if (!data.canRun()) {
                continue;
            }
            BaseActivityController controller = data.getType().getController();
            controller.checkPlayerDataAndReset(player.getId(), data);
            info.activityInfos.add(controller.buildActivityInfo(player.getId(), data));
        }
        playerController.send(info);
    }


    /**
     * 玩家活动进度更新
     *
     * @param playerId           玩家id
     * @param activityTargetType 触发类型
     * @param value              增加值
     */
    public void addPlayerActivityProgress(long playerId, ActivityTargetType activityTargetType, long value) {
        //获取需要增加的活动类型
        for (ActivityType activityType : ActivityType.values()) {
            if (!activityType.isCanAddPlayerProgress() || (activityType.getTargetKey() & activityTargetType.getTargetKey()) == 0) {
                continue;
            }
            Map<Long, ActivityData> activityDataMap = activityTypeData.get(activityType);
            if (activityDataMap == null) {
                continue;
            }
            List<ActivityData> dataArrayList = new ArrayList<>();
            for (ActivityData data : activityDataMap.values()) {
                try {
                    //获取该玩家的活动详细信息
                    Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityType, data.getId());
                    int added = data.getType().getController().addPlayerProgress(playerId, playerActivityData, value);
                    if (added == ActivityConstant.ClaimStatus.CAN_CLAIM) {
                        dataArrayList.add(data);
                    }
                } catch (Exception e) {
                    log.error("增加玩家活动进度失败 playerId:{} activityId:{} value:{}", playerId, data.getId(), value, e);
                }
            }
            //广播给玩家
            if (CollectionUtil.isNotEmpty(dataArrayList)) {
                NotifyActivityChange activityChange = new NotifyActivityChange();
                activityChange.activityInfos = new ArrayList<>();
                for (ActivityData data : dataArrayList) {
                    ActivityInfo activityInfo = data.getType().getController().buildActivityInfo(playerId, data);
                    activityChange.activityInfos.add(activityInfo);
                }
                clusterSystem.sendToPlayer(activityChange, playerId);
            }
        }
    }

    /**
     * 活动进度更新
     *
     * @param activityTargetType
     * @param value
     */
    public void addActivityProgress(ActivityTargetType activityTargetType, long value) {
        for (ActivityType activityType : ActivityType.values()) {
            if (!activityType.isCanAddActivityProgress() || (activityType.getTargetKey() & activityTargetType.getTargetKey()) == 0) {
                continue;
            }
            Map<Long, ActivityData> activityDataMap = activityTypeData.get(activityType);
            if (activityDataMap == null) {
                continue;
            }
            for (ActivityData data : activityDataMap.values()) {
                try {
                    //获取该玩家的活动详细信息
                    data.getType().getController().addActivityProgress(data, value);
                } catch (Exception e) {
                    log.error("增加活动进度失败  activityId:{} value:{}", data.getId(), value, e);
                }
            }
            //TODO 广播给全节点玩家
        }
    }

    /**
     * 参加活动
     */
    public void joinActivity(long playerId, long activityId, int detailId) {
        try {
            //获取该玩家的活动详细信息
            ActivityData data = activityData.get(activityId);
            if (data == null || !data.canRun()) {
                log.warn("玩家请求参加的活动未开始 playerId:{} activityId:{}  ", playerId, activityId);
                return;
            }
            AbstractResponse res = data.getType().getController().joinActivity(playerId, data, detailId);
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
     * @return 节点在线玩家id
     */
    public List<Long> getOnlinePlayerIds() {
        return clusterSystem.getAllPlayerIds();
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        String cmd = gmOrders[0];
        if ("joinActivity".equalsIgnoreCase(cmd)) {
            if (gmOrders.length < 3) {
                return new CommonResult<>(Code.FAIL);
            }
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
            joinActivity(playerController.playerId(), data.getId(), detailId);
            return new CommonResult<>(Code.SUCCESS);
        }
        return null;
    }
}
