package com.jjg.game.activity.common.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.dao.PlayerActivityDao;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.ActivityBuilder;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.bean.BaseCfgBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/9/3 16:14
 */
public abstract class BaseActivityController {

    protected final PlayerActivityDao playerActivityDao;
    protected final ActivityManager activityManager;
    protected final PlayerPackService playerPackService;

    public PlayerActivityDao getPlayerActivityDao() {
        return playerActivityDao;
    }


    public BaseActivityController(PlayerActivityDao playerActivityDao, ActivityManager activityManager, PlayerPackService playerPackService) {
        this.playerActivityDao = playerActivityDao;
        this.activityManager = activityManager;
        this.playerPackService = playerPackService;
    }


    /**
     * 增加玩家活动进度
     */
    public void AddPlayerProgress(long playerId, ActivityData activityData, long progress) {

    }

    /**
     * 增加总体活动进度
     */
    public void AddActivityProgress(long playerId, ActivityData activityData, long progress) {

    }

    public abstract AbstractResponse joinActivity(long playerId, ActivityData activityData,int detailId);

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
    public abstract AbstractResponse getPlayerActivityDetail(long playerId, long activityId, int detailId);

    /**
     * 获取类型获取活动详情响应信息
     */
    public abstract AbstractResponse getPlayerActivityInfoByTypeRes(List<List<BaseActivityDetailInfo>> allDetailInfo);


    /**
     * 通过类型获取活动详情
     */
    public AbstractResponse getPlayerActivityInfoByType(long playerId, ActivityType activityType) {
        Map<Long, Map<Integer, PlayerActivityData>> playerActivityData = playerActivityDao.getAllPlayerActivityData(playerId, activityType);
        if (CollectionUtil.isEmpty(playerActivityData)) {
            return ActivityBuilder.getDefaultResponse();
        }
        Map<Long, ActivityData> activityDataMap = activityManager.getActivityTypeData().get(activityType);
        if (CollectionUtil.isEmpty(activityDataMap)) {
            return ActivityBuilder.getDefaultResponse();
        }
        Map<Long, Map<Integer, BaseCfgBean>> activityDetailInfo = activityManager.getActivityDetailInfo();
        List<List<BaseActivityDetailInfo>> allDetailInfo = new ArrayList<>();
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
            Map<Integer, PlayerActivityData> privilegeCardCfgMap = playerActivityData.get(activityData.getId());
            if (CollectionUtil.isEmpty(privilegeCardCfgMap)) {
                continue;
            }
            List<BaseActivityDetailInfo> arrayList = new ArrayList<>();
            allDetailInfo.add(arrayList);
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
        return getPlayerActivityInfoByTypeRes(allDetailInfo);
    }


    /**
     * 加载活动详细数据
     *
     * @return
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
