package com.jjg.game.activity.common.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.dao.PlayerActivityDao;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/9/3 16:14
 */
public abstract class BaseActivityController {
    @Autowired
    protected PlayerActivityDao playerActivityDao;
    @Autowired
    protected ActivityManager activityManager;
    @Autowired
    protected PlayerPackService playerPackService;
    @Autowired
    protected CorePlayerService corePlayerService;
    @Autowired
    protected RedisLock redisLock;

    /**
     * 增加玩家活动进度
     */
    public boolean addPlayerProgress(long playerId, ActivityData activityData, long progress) {
        return false;
    }

    /**
     * 增加总体活动进度
     */
    public void addActivityProgress(ActivityData activityData, long progress) {
    }

    /**
     * 活动加载完成执行
     */
    public void activityLoadCompleted(ActivityData activityData) {
    }


    public abstract AbstractResponse joinActivity(long playerId, ActivityData activityData, int detailId);

    /**
     * 领取活动奖励
     */
    public abstract AbstractResponse claimActivityRewards(long playerId, ActivityData activityData, int detailId);

    /**
     * 活动结束
     */
    public abstract void onActivityEnd(ActivityData activityData);

    /**
     * 活动开始
     */
    public abstract void onActivityStart(ActivityData activityData);


    /**
     * 后台更新活动数据
     */
    public abstract int updateActivity(String jsonData);

    /**
     * 获取活动详情
     */
    public abstract BaseActivityDetailInfo buildPlayerActivityDetail(long activityId, BaseCfgBean baseCfgBean, PlayerActivityData data);

    /**
     * 获取活动详情
     */
    public abstract AbstractResponse getPlayerActivityDetail(long playerId, ActivityData activityData, int detailId);

    /**
     * 获取类型获取活动详情响应信息
     */
    public abstract AbstractResponse getPlayerActivityInfoByTypeRes(long playerId, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo);

    /**
     * 构建玩家活动信息
     */
    public abstract ActivityInfo buildActivityInfo(long playerId, ActivityData activityData);

    /**
     * 检查玩家数据并重置
     *
     * @param playerId     玩家id
     * @param activityData 活动数据
     */
    public void checkPlayerDataAndReset(long playerId, ActivityData activityData) {
        //限时活动不需要重置
        if (activityData.getOpenType() == 2) {
            return;
        }
        Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
        if (CollectionUtil.isNotEmpty(playerActivityData)) {
            boolean needRest = false;
            for (PlayerActivityData data : playerActivityData.values()) {
                if (data.getRound() != activityData.getRound()) {
                    needRest = true;
                    break;
                }
            }
            if (needRest) {
                playerActivityDao.deletePlayerActivityData(playerId, activityData.getType(), activityData.getId());
            }
        }
    }

    /**
     * 通过类型获取活动详情
     */
    public AbstractResponse getPlayerActivityInfoByType(long playerId, ActivityType activityType) {
        Map<Long, Map<Integer, PlayerActivityData>> playerActivityData = playerActivityDao.getAllPlayerActivityData(playerId, activityType);
        Map<Long, ActivityData> activityDataMap = activityManager.getActivityTypeData().get(activityType);
        Map<Long, List<BaseActivityDetailInfo>> allDetailInfoMap = new HashMap<>();
        if (CollectionUtil.isEmpty(activityDataMap)) {
            return getPlayerActivityInfoByTypeRes(playerId, allDetailInfoMap);
        }
        Map<Long, Map<Integer, BaseCfgBean>> activityDetailInfo = activityManager.getActivityDetailInfo();
        for (ActivityData activityData : activityDataMap.values()) {
            Map<Integer, BaseCfgBean> baseCfgBeanMap = activityDetailInfo.get(activityData.getId());
            if (CollectionUtil.isEmpty(baseCfgBeanMap)) {
                continue;
            }
            if (!activityData.canRun()) {
                continue;
            }
            if (CollectionUtil.isEmpty(activityData.getValue())) {
                continue;
            }
            Map<Integer, PlayerActivityData> privilegeCardCfgMap = playerActivityData.getOrDefault(activityData.getId(), Map.of());
            List<BaseActivityDetailInfo> arrayList = new ArrayList<>();
            allDetailInfoMap.put(activityData.getId(), arrayList);
            for (Integer id : activityData.getValue()) {
                BaseCfgBean baseCfgBean = baseCfgBeanMap.get(id);
                if (baseCfgBean == null) {
                    continue;
                }
                BaseActivityDetailInfo detail = buildPlayerActivityDetail(activityData.getId(), baseCfgBean, privilegeCardCfgMap.get(id));
                if (detail != null) {
                    arrayList.add(detail);
                }
            }
        }
        return getPlayerActivityInfoByTypeRes(playerId, allDetailInfoMap);
    }


    /**
     * 加载活动详细数据
     */
    public Map<Integer, BaseCfgBean> loadDetailData(Map<Integer, BaseCfgBean> dbData) {
        List<BaseCfgBean> detailCfgBean = getDetailCfgBean();
        if (CollectionUtil.isEmpty(dbData)) {
            //全部从配置表加载
            return detailCfgBean
                    .stream()
                    .collect(Collectors.toMap(BaseCfgBean::getId, b -> b));
        }
        for (BaseCfgBean baseCfgBean : detailCfgBean) {
            int cfgBeanId = baseCfgBean.getId();
            if (dbData.containsKey(cfgBeanId)) {
                continue;
            }
            dbData.put(cfgBeanId, baseCfgBean);
        }
        return dbData;
    }

    public abstract List<BaseCfgBean> getDetailCfgBean();

    /**
     * 获取活动详情类
     */
    public abstract Class<? extends BaseCfgBean> getDetailDataClass();

}
