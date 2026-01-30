package com.jjg.game.activity.common.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.activity.activitylog.ActivityLogger;
import com.jjg.game.activity.common.dao.PlayerActivityDao;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.ClaimRewardsResult;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.ActivityBuilder;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.manager.ConditionManager;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.TipUtils;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动基础控制器抽象类
 * <p>
 * 封装了游戏活动的通用逻辑，提供：
 * 1. 玩家参与活动流程
 * 2. 活动奖励发放
 * 3. 活动进度管理
 * 4. 活动开始/结束生命周期处理
 * 5. 活动数据加载和重置
 * <p>
 * 子类需要实现抽象方法，根据不同活动类型进行扩展。
 *
 * @author lm
 * @date 2025/9/3 16:14
 */
public abstract class BaseActivityController {
    private final Logger log = LoggerFactory.getLogger(BaseActivityController.class);

    /**
     * 活动初始化进度
     */
    private static final String INIT_PROGRESS_KEY = "initProgress:%s";

    /**
     * 玩家活动数据访问对象
     */
    @Autowired
    protected PlayerActivityDao playerActivityDao;

    /**
     * 活动管理器，负责全局活动配置和状态管理
     */
    @Autowired
    protected ActivityManager activityManager;

    /**
     * 玩家背包服务，用于发放道具、物品
     */
    @Autowired
    protected PlayerPackService playerPackService;

    /**
     * 玩家核心服务，用于获取玩家基本信息
     */
    @Autowired
    protected CorePlayerService corePlayerService;

    /**
     * 分布式 Redis 锁，防止并发导致数据错误
     */
    @Autowired
    protected RedisLock redisLock;

    /**
     * 条件检查服务，用于判断玩家是否符合参与条件
     */
    @Autowired
    protected ConditionManager conditionManager;

    /**
     * 活动日志记录器，用于追踪活动行为
     */
    @Autowired
    protected ActivityLogger activityLogger;


    /**
     * 红点管理器
     */
    @Autowired
    protected RedDotManager redDotManager;

    /**
     * 计数dao
     */
    @Autowired
    protected CountDao countDao;

    /**
     * 增加玩家的活动进度
     *
     * @param player               玩家数据
     * @param activityData         活动数据
     * @param progress             要增加的进度值
     * @param activityTargetKey    触发key
     * @param additionalParameters 额外参数，留作扩展
     * @return true 需要给前端发送数据，false 不需要给前端发送数据
     */
    public boolean addPlayerProgress(Player player, ActivityData activityData, long progress, long activityTargetKey, Object additionalParameters) {
        return false;
    }

    /**
     * 增加活动整体进度（全局共享进度）
     *
     * @param activityData         活动数据
     * @param progress             增加的进度值
     * @param additionalParameters 扩展参数
     */
    public void addActivityProgress(ActivityData activityData, long progress, Object additionalParameters) {
    }

    /**
     * 活动加载完成后的回调
     *
     * @param activityData 活动数据
     */
    public void activityLoadCompleted(ActivityData activityData) {
    }

    /**
     * 检查玩家是否能参与活动
     *
     * @param player
     * @param activityData
     * @return
     */
    public boolean checkPlayerCanJoinActivity(Player player, ActivityData activityData) {
        // 调用条件检查服务，验证触发条件是否完成
        return conditionManager.isAchievement(player, "", activityData.getCondition());
    }


    /**
     * 玩家请求参加活动
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动子项ID
     * @param times        请求参加的次数
     * @return 响应对象
     */
    public abstract AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times);

    /**
     * 玩家领取活动奖励
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     奖励详情ID
     * @return 响应对象
     */
    public abstract AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId);


    /**
     * 更新红点数据
     *
     * @param playerId 玩家id
     * @param data     活动数据
     * @param oldState 老的红点状态
     */
    public final void updateRodDot(long playerId, ActivityData data, boolean oldState, boolean compulsory) {
        boolean hasRedDot = hasRedDot(playerId, data);
        if (hasRedDot != oldState || compulsory) {
            redDotManager.updateActivityRedDot(playerId, data.getType().getType(), hasRedDot);
        }
    }

    /**
     * 更新红点数据
     *
     * @param playerId 玩家id
     * @param data     活动数据
     * @param oldState 老的红点状态
     */
    public final void updateRodDot(long playerId, ActivityData data, boolean oldState) {
        updateRodDot(playerId, data, oldState, false);
    }

    /**
     * 玩家购买活动礼包
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param giftId       礼包ID
     */
    public void buyActivityGift(Player player, ActivityData activityData, int giftId) {
    }

    /**
     * 活动结束回调
     *
     * @param activityData 活动数据
     */
    public void onActivityEnd(ActivityData activityData) {
    }


    /**
     * 活动开始回调
     *
     * @param activityData 活动数据
     */
    public void onActivityStart(ActivityData activityData) {
    }

    /**
     * 构建玩家的活动详情数据
     *
     * @param player
     * @param activityData 活动ID
     * @param baseCfgBean  配置数据
     * @param data         玩家活动数据
     * @return 活动详情对象
     */
    public abstract BaseActivityDetailInfo buildPlayerActivityDetail(Player player, ActivityData activityData, BaseCfgBean baseCfgBean, PlayerActivityData data);

    /**
     * 获取指定玩家的活动详情
     *
     * @param player       玩家ID
     * @param activityData 活动数据
     * @param detailId     活动详情ID
     * @return 响应对象
     */
    public abstract AbstractResponse getPlayerActivityDetail(Player player, ActivityData activityData, int detailId);


    /**
     * 构建指定活动类型的响应
     *
     * @param player        玩家数据
     * @param allDetailInfo 活动详情映射
     * @return 响应对象
     */
    public abstract AbstractResponse getPlayerActivityInfoByTypeRes(Player player, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo);

    /**
     * 构建玩家活动基本信息
     *
     * @param activityData 活动数据
     * @return 活动信息
     */
    public ActivityInfo buildActivityInfo(ActivityData activityData) {
        return ActivityBuilder.buildActivityInfo(activityData);
    }

    /**
     * 请求数据时检查玩家的活动数据是否需要重置
     *
     * @param player       玩家数据
     * @param activityData 活动数据
     */
    public Map<Integer, PlayerActivityData> checkPlayerDataAndResetOnRequest(Player player, ActivityData activityData) {
        return null;
    }


    /**
     * 初始化数据
     *
     * @param player       玩家数据
     * @param activityData 活动数据
     * @return 活动数据
     */
    public boolean initProgress(Player player, ActivityData activityData) {
        return false;
    }

    /**
     * 是否能初始化进度
     *
     * @param playerId     玩家id
     * @param activityData 活动数据
     * @return true能初始化进度 false不能初始化进度
     */
    public boolean canInitProgress(long playerId, ActivityData activityData) {
        String customId = INIT_PROGRESS_KEY.formatted(activityData.getId());
        return countDao.setIfAbsent(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(playerId), customId);
    }

    /**
     * 清除初始化进度标识
     *
     * @param playerId     玩家id
     * @param activityData 活动数据
     */
    public void clearInitProgress(long playerId, ActivityData activityData) {
        String customId = INIT_PROGRESS_KEY.formatted(activityData.getId());
        countDao.reset(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(playerId), customId);
    }

    /**
     * 首次登录检查玩家的活动数据是否需要重置
     *
     * @param playerId     玩家ID
     * @param activityData 活动数据
     */
    public void checkPlayerDataAndResetOnLogin(long playerId, ActivityData activityData) {
        // 限时活动（openType=2）不需要重置
        if (activityData.getOpenType() == ActivityConstant.Common.LIMIT_TYPE) {
            return;
        }
        // 获取玩家该活动的历史数据
        Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
        if (CollectionUtil.isNotEmpty(playerActivityData)) {
            boolean needRest = false;
            for (PlayerActivityData data : playerActivityData.values()) {
                // 如果期数数不一致，则需要重置
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
     * 获取指定类型的玩家活动信息
     *
     * @param player       玩家对象
     * @param activityType 活动类型
     * @return 响应对象
     */
    public AbstractResponse getPlayerActivityInfoByType(Player player, ActivityType activityType) {
        long playerId = player.getId();
        Player latestPlayer = corePlayerService.get(playerId);
        // 获取玩家的活动数据（分活动ID -> 分子项ID）
        Map<Long, Map<Integer, PlayerActivityData>> playerActivityData = playerActivityDao.getAllPlayerActivityData(playerId, activityType);

        // 获取全局活动配置
        Map<Long, ActivityData> activityDataMap = activityManager.getActivityTypeData().get(activityType);
        Map<Long, List<BaseActivityDetailInfo>> allDetailInfoMap = new HashMap<>();

        // 如果没有活动，直接返回空结果
        if (CollectionUtil.isEmpty(activityDataMap)) {
            return getPlayerActivityInfoByTypeRes(latestPlayer, allDetailInfoMap);
        }
        // 获取活动的子配置数据
        // 遍历每个活动
        for (ActivityData activityData : activityDataMap.values()) {
            Map<Integer, ? extends BaseCfgBean> baseCfgBeanMap = activityData.getType().getController().getDetailCfgBean(activityData);
            // 过滤掉不可运行、无配置、或玩家不符合条件的活动
            if (!activityData.getType().isShowInNotOpen() && (CollectionUtil.isEmpty(baseCfgBeanMap) || !activityData.canRun()
                    || CollectionUtil.isEmpty(activityData.getValue()))) {
                continue;
            }
            //请求时处理数据重置
            Map<Integer, PlayerActivityData> playerActivityDataMap = checkPlayerDataAndResetOnRequest(player, activityData);
            if (playerActivityDataMap == null) {
                // 获取玩家该活动的数据（子项维度）
                playerActivityDataMap = playerActivityData.getOrDefault(activityData.getId(), Map.of());
            }
            // 构建活动详情列表
            allDetailInfoMap.put(activityData.getId(), getBaseActivityDetailInfos(activityData, baseCfgBeanMap, latestPlayer, playerActivityDataMap));
        }
        return getPlayerActivityInfoByTypeRes(latestPlayer, allDetailInfoMap);
    }

    /**
     * 获取活动详细信息
     *
     * @param activityData          活动数据
     * @param baseCfgBeanMap        配置信息
     * @param player                玩家信息
     * @param playerActivityDataMap 玩家活动数据
     * @return 活动详细信息
     */
    public List<BaseActivityDetailInfo> getBaseActivityDetailInfos(ActivityData activityData, Map<Integer, ? extends BaseCfgBean> baseCfgBeanMap,
                                                                   Player player, Map<Integer, PlayerActivityData> playerActivityDataMap) {
        List<BaseActivityDetailInfo> arrayList = new ArrayList<>();
        for (Integer id : activityData.getValue()) {
            BaseCfgBean baseCfgBean = baseCfgBeanMap.get(id);
            if (baseCfgBean == null) {
                continue;
            }
            BaseActivityDetailInfo detail = buildPlayerActivityDetail(player, activityData, baseCfgBean, playerActivityDataMap.get(id));
            if (detail != null) {
                arrayList.add(detail);
            }
        }
        return arrayList;
    }


    /**
     * 获取活动子项配置（抽象方法，子类实现）
     */
    public abstract Map<Integer, ? extends BaseCfgBean> getDetailCfgBean(ActivityData activityData);

    /**
     * 返回当前活动是否有红点
     *
     * @return true 有红点 false没有红点
     */
    public boolean hasRedDot(long playerId, ActivityData activityData) {
        Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
        if (CollectionUtil.isEmpty(playerActivityData)) {
            return false;
        }
        for (PlayerActivityData data : playerActivityData.values()) {
            if (data.getClaimStatus() == ActivityConstant.ClaimStatus.CAN_CLAIM) {
                return true;
            }
        }
        return false;
    }

    /**
     * 通用活动领取奖励
     *
     * @param playerId     玩家id
     * @param activityData 活动数据
     * @param detailId     活动详情id
     * @param getItem      获得道具
     * @return 最新玩家活动数据,添加道具结果
     */
    public ClaimRewardsResult claimActivityRewards(long playerId, ActivityData activityData, int detailId, AddType addType, Map<Integer, Long> getItem) {
        long activityId = activityData.getId();
        PlayerActivityData data;
        CommonResult<ItemOperationResult> addedItems;
        try {
            Map<Integer, PlayerActivityData> dataMap = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
            if (CollectionUtil.isEmpty(dataMap)) {
                TipUtils.sendTip(playerId, TipUtils.TipType.TOAST, Code.PARAM_ERROR);
                return null;
            }
            data = dataMap.get(detailId);
            if (data == null) {
                TipUtils.sendTip(playerId, TipUtils.TipType.TOAST, Code.PARAM_ERROR);
                return null;
            }
            if (data.getClaimStatus() != ActivityConstant.ClaimStatus.CAN_CLAIM) {
                TipUtils.sendTip(playerId, TipUtils.TipType.TOAST, Code.REPEAT_OP);
                return null;
            }
            // 发放奖励
            addedItems = playerPackService.addItems(playerId, getItem, addType);
            if (!addedItems.success()) {
                return null;
            }
            data.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED);
            playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityId, dataMap);
            return new ClaimRewardsResult(data, addedItems.data);
        } catch (Exception e) {
            log.error("活动领取异常 playerId:{} activityId:{} detailId:{}", playerId, activityId, detailId, e);
        }
        return null;
    }

    /**
     * 获取活动订单生成配置
     *
     * @param player   玩家数据
     * @param order    订单
     * @param dealType 处理类型1加入活动 2购买礼包
     */
    public final void dealActivityRecharge(Player player, Order order, int dealType) {
        log.info("充值事件 参加活动 playerId:{}  order;{}", player.getId(), JSONObject.toJSONString(order));
        String[] idCfg = StringUtils.split(order.getProductId(), "_");
        if (idCfg.length != 2) {
            log.error("活动充值回调 productId错误 playerId:{} order;{}", player.getId(), JSONObject.toJSONString(order));
            return;
        }
        long activityId = Long.parseLong(idCfg[0]);
        int detailId = Integer.parseInt(idCfg[1]);
        ActivityData data = activityManager.getActivityData().get(activityId);
        if (data == null || !data.getValue().contains(detailId) || !checkPlayerCanJoinActivity(player, data)) {
            log.error("充值事件 不能参加活动 playerId:{} order;{}", player.getId(), JSONObject.toJSONString(order));
            return;
        }
        if (dealType == 1) {
            AbstractResponse res = joinActivity(player, data, detailId, 1);
            if (res != null) {
                log.info("充值事件 参加活动成功 playerId:{}  order;{}", player.getId(), JSONObject.toJSONString(order));
                activityManager.sendToPlayer(player.getId(), res);
            }
        } else if (dealType == 2) {
            buyActivityGift(player, data, detailId);
            log.info("充值事件 购买活动礼包成功 playerId:{}  order;{}", player.getId(), JSONObject.toJSONString(order));
        }
        //更新红点
        updateRodDot(player.getId(), data, false);
    }

    /**
     * 获取活动订单生成配置
     *
     * @param productId 原id
     *
     */
    public final BaseCfgBean getOrderGenerateBean(Player player, String productId) {
        String[] idCfg = StringUtils.split(productId, "_");
        if (idCfg.length != 2) {
            return null;
        }
        long activityId = Long.parseLong(idCfg[0]);
        int detailId = Integer.parseInt(idCfg[1]);
        ActivityData data = activityManager.getActivityData().get(activityId);
        if (data == null || !data.getValue().contains(detailId) || !checkPlayerCanJoinActivity(player, data)) {
            return null;
        }
        Map<Integer, ? extends BaseCfgBean> bean = getDetailCfgBean(data);
        return bean.get(detailId);
    }
}
