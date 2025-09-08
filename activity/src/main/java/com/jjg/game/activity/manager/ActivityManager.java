package com.jjg.game.activity.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.dao.ActivityDao;
import com.jjg.game.activity.common.dao.ActivityDetailDao;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.message.res.NotifyActivityChange;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ActivityConfigCfg;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lm
 * @date 2025/9/3 18:16
 */
@MessageType(MessageConst.MessageTypeDef.ACTIVITY)
@Component
public class ActivityManager implements TimerListener<Long> {
    private final TimerCenter timerCenter;
    private final ActivityDao activityDao;
    private final ActivityDetailDao activityDetailDao;
    private final ClusterSystem clusterSystem;
    //活动id->活动配置
    private Map<Long, ActivityData> activityData = new ConcurrentHashMap<>();
    private Map<ActivityType, Map<Long, ActivityData>> activityTypeData = new ConcurrentHashMap<>();
    //活动id->对应活动配置
    private Map<Long, Map<Integer, BaseCfgBean>> activityDetailInfo = new ConcurrentHashMap<>();
    private final CoreMarqueeManager marqueeManager;
    private final long startServerTime = 104540254;

    public ActivityManager(TimerCenter timerCenter, ActivityDao activityDao, ActivityDetailDao activityDetailDao, ClusterSystem clusterSystem, CoreMarqueeManager marqueeManager) {
        this.timerCenter = timerCenter;
        this.activityDao = activityDao;
        this.activityDetailDao = activityDetailDao;
        this.clusterSystem = clusterSystem;
        this.marqueeManager = marqueeManager;
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
        Map<Long, ActivityData> tempActivityData = new ConcurrentHashMap<>();
        Map<Long, Map<Integer, BaseCfgBean>> tempActivityDetailInfo = new ConcurrentHashMap<>();
        //要添加定时器的列表 时间戳 活动id
        List<Pair<Long, Long>> timerList = new ArrayList<>();
        long timeMillis = System.currentTimeMillis();
        //从数据库加载
        List<ActivityData> allActivityInfos = activityDao.getAllActivityInfos();
        for (ActivityData data : allActivityInfos) {
            if (checkActivityData(data, timeMillis, timerList)) {
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
            if (activityType == null) {
                continue;
            }
            ActivityData data = ActivityData.getActivityData(activityConfigCfg, activityType);
            if (!checkActivityData(data, timeMillis, timerList)) {
                continue;
            }
            long activityInfoId = activityConfigCfg.getId();
            Map<Integer, BaseCfgBean> loadedDetailData = data.getType().getController().loadDetailData(activityDetailInfo.get(activityInfoId));
            if (CollectionUtil.isNotEmpty(loadedDetailData)) {
                for (Integer id : loadedDetailData.keySet()) {
                    if (data.getValue().contains(id)) {
                        continue;
                    }
                    loadedDetailData.remove(id);
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
        //保存到redis
        activityDao.saveActivities(tempActivityData);
        for (Map.Entry<Long, Map<Integer, BaseCfgBean>> entry : tempActivityDetailInfo.entrySet()) {
            activityDetailDao.saveActivityDetails(entry.getKey(), entry.getValue());
        }
        activityData = tempActivityData;
        activityDetailInfo = tempActivityDetailInfo;
        Map<ActivityType, Map<Long, ActivityData>> activityTypeData = new ConcurrentHashMap<>();
        for (ActivityData data : tempActivityData.values()) {
            activityTypeData.computeIfAbsent(data.getType(), k -> new ConcurrentHashMap<>()).put(data.getId(), data);
        }
    }

    public boolean checkActivityData(ActivityData data, long timeMillis, List<Pair<Long, Long>> timerList) {
        if (!data.isOpen() || data.getStatus() == ActivityConstant.ActivityStatus.ENDED) {
            return false;
        }
        //通过活动类型判断
        if (data.getOpenType() == 1) {
            //开服 开始时间戳为0设置为开服时间，结束时间戳为持续时间的时间戳 添加结束时间
            long timestampByDay = TimeHelper.getTimestampByDay(startServerTime, data.getDuration());
            if (data.getTimeStart() == 0) {
                data.setTimeStart(startServerTime);
            }
            data.setTimeEnd(timestampByDay);
        } else if (data.getOpenType() == 2 && timeMillis > data.getTimeEnd()) {
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
        notifyActivityChange.activityId = data.getId();
        notifyActivityChange.status = data.getStatus();
        notifyActivityChange.activityType = data.getType().getType();
        clusterSystem.broadcastNode(notifyActivityChange);
    }
}
