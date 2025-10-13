package com.jjg.game.activity.common.controller;

import cn.hutool.core.collection.CollectionUtil;
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
import com.jjg.game.core.base.condition.ConditionCheckService;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.TipUtils;
import com.jjg.game.sampledata.bean.BaseCfgBean;
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
    protected ConditionCheckService conditionCheckService;

    /**
     * 活动日志记录器，用于追踪活动行为
     */
    @Autowired
    protected ActivityLogger activityLogger;


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
     * @param activityData 活动数据
     * @param player       玩家对象
     * @return true 可以参与；false 不能参与
     */
    public boolean checkPlayerCanJoinActivity(Player player, ActivityData activityData) {
        // 调用条件检查服务，验证触发条件是否完成
        return conditionCheckService.isTriggerComplete(player, activityData.getCondition());
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
     * 后台更新活动配置数据
     *
     * @param jsonData 活动配置的JSON字符串
     * @return 更新后的记录数
     */
    public int updateActivity(String jsonData) {
        return 0;
    }

    /**
     * 构建玩家的活动详情数据
     *
     * @param activityId  活动ID
     * @param baseCfgBean 配置数据
     * @param data        玩家活动数据
     * @return 活动详情对象
     */
    public abstract BaseActivityDetailInfo buildPlayerActivityDetail(long activityId, BaseCfgBean baseCfgBean, PlayerActivityData data);

    /**
     * 获取指定玩家的活动详情
     *
     * @param playerId     玩家ID
     * @param activityData 活动数据
     * @param detailId     活动详情ID
     * @return 响应对象
     */
    public abstract AbstractResponse getPlayerActivityDetail(long playerId, ActivityData activityData, int detailId);


    /**
     * 构建指定活动类型的响应
     *
     * @param playerId      玩家ID
     * @param allDetailInfo 活动详情映射
     * @return 响应对象
     */
    public abstract AbstractResponse getPlayerActivityInfoByTypeRes(long playerId, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo);

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
     * @param playerId     玩家ID
     * @param activityData 活动数据
     */
    public Map<Integer, PlayerActivityData> checkPlayerDataAndResetOnRequest(long playerId, ActivityData activityData) {
        return null;
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
        // 获取玩家的活动数据（分活动ID -> 分子项ID）
        Map<Long, Map<Integer, PlayerActivityData>> playerActivityData = playerActivityDao.getAllPlayerActivityData(playerId, activityType);

        // 获取全局活动配置
        Map<Long, ActivityData> activityDataMap = activityManager.getActivityTypeData().get(activityType);
        Map<Long, List<BaseActivityDetailInfo>> allDetailInfoMap = new HashMap<>();

        // 如果没有活动，直接返回空结果
        if (CollectionUtil.isEmpty(activityDataMap)) {
            return getPlayerActivityInfoByTypeRes(playerId, allDetailInfoMap);
        }

        // 获取活动的子配置数据
        // 遍历每个活动
        for (ActivityData activityData : activityDataMap.values()) {
            Map<Integer, ? extends BaseCfgBean> baseCfgBeanMap = activityData.getType().getController().getDetailCfgBean(activityData);
            // 过滤掉不可运行、无配置、或玩家不符合条件的活动
            if (!activityData.getType().isShowInNotOpen() && (CollectionUtil.isEmpty(baseCfgBeanMap) || !activityData.canRun()
                    || CollectionUtil.isEmpty(activityData.getValue())
                    || !checkPlayerCanJoinActivity(player, activityData))) {
                continue;
            }
            //请求时处理数据重置
            Map<Integer, PlayerActivityData> playerActivityDataMap = checkPlayerDataAndResetOnRequest(playerId, activityData);
            if (playerActivityDataMap == null) {
                // 获取玩家该活动的数据（子项维度）
                playerActivityDataMap = playerActivityData.getOrDefault(activityData.getId(), Map.of());
            }
            // 构建活动详情列表
            List<BaseActivityDetailInfo> arrayList = new ArrayList<>();
            allDetailInfoMap.put(activityData.getId(), arrayList);

            for (Integer id : activityData.getValue()) {
                BaseCfgBean baseCfgBean = baseCfgBeanMap.get(id);
                if (baseCfgBean == null) {
                    continue;
                }
                BaseActivityDetailInfo detail = buildPlayerActivityDetail(activityData.getId(), baseCfgBean, playerActivityDataMap.get(id));
                if (detail != null) {
                    arrayList.add(detail);
                }
            }
        }
        return getPlayerActivityInfoByTypeRes(playerId, allDetailInfoMap);
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
    public ClaimRewardsResult claimActivityRewards(long playerId, ActivityData activityData, int detailId,String addType, Map<Integer, Long> getItem) {
        long activityId = activityData.getId();
        PlayerActivityData data;
        CommonResult<ItemOperationResult> addedItems;
        String lockKey = playerActivityDao.getLockKey(playerId, activityId);
        // 加锁，保证领取操作原子性
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
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
        } finally {
            redisLock.unlock(lockKey);
        }
        return null;
    }
}
