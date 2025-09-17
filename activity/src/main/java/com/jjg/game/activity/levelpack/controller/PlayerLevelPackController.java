package com.jjg.game.activity.levelpack.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.ActivityBuilder;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.common.message.res.ResActivityBuyGift;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.levelpack.message.bean.PlayerLevelPackActivity;
import com.jjg.game.activity.levelpack.message.bean.PlayerLevelPackDetailInfo;
import com.jjg.game.activity.levelpack.message.data.PlayerLevelPackData;
import com.jjg.game.activity.levelpack.message.res.ResPlayerLevelPackDetailInfo;
import com.jjg.game.activity.levelpack.message.res.ResPlayerLevelPackTypeInfo;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.PlayerLevelPackCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/9/3
 */
@Component
public class PlayerLevelPackController extends BaseActivityController {
    private final Logger log = LoggerFactory.getLogger(PlayerLevelPackController.class);

    /**
     * 玩家参与等级礼包
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动明细ID
     * @param times        参与次数
     * @return 参与活动结果响应
     */
    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        long playerLevel = player.getLevel();
        long playerId = player.getId();
        long activityId = activityData.getId();
        // 获取活动明细配置
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        //获取活动数据
        Map<Integer, PlayerLevelPackData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
        //需要增加的活动数据
        List<PlayerLevelPackCfg> playerLevelPack = new ArrayList<>();
        for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
            //当等级比配置大且不包含在活动数据里面的添加
            if (cfgBean instanceof PlayerLevelPackCfg cfg && playerActivityData.get(cfg.getId()) == null && playerLevel >= cfg.getPlayerlevel()) {
                playerLevelPack.add(cfg);
            }
        }
        if (playerLevelPack.isEmpty()) {
            return null;
        }
        long currentTimeMillis = System.currentTimeMillis();
        boolean change = false;
        String lockKey = playerActivityDao.getLockKey(playerId, activityId);
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            //从新获取活动数据
            playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
            for (PlayerLevelPackCfg packCfg : playerLevelPack) {
                PlayerLevelPackData packData = playerActivityData.get(packCfg.getId());
                if (packData != null) {
                    continue;
                }
                //构建新的活动数据
                PlayerLevelPackData data = new PlayerLevelPackData(activityId, activityData.getRound());
                data.setTargetTime(currentTimeMillis);
                data.setBuyEndTime((long) packCfg.getTime() * TimeHelper.ONE_MINUTE_OF_MILLIS + currentTimeMillis);
                playerActivityData.put(packCfg.getId(), data);
                change = true;
            }
            //回存活动数据
            if (change) {
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityId, playerActivityData);
            }
        } catch (Exception e) {
            log.error("等级变化时修改玩家活动数据失败 playerId:{} playerLevel:{} activityId:{}", playerId, playerLevel, activityId, e);
        } finally {
            redisLock.unlock(lockKey);
        }
        return null;
    }


    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        return null;
    }

    @Override
    public void onActivityEnd(ActivityData activityData) {
        // 活动结束逻辑，可扩展
    }

    @Override
    public void onActivityStart(ActivityData activityData) {
        // 活动开始逻辑，可扩展
    }

    @Override
    public int updateActivity(String jsonData) {
        // 可用于更新活动配置
        return 0;
    }

    /**
     * 获取玩家等级礼包活动明细
     */
    @Override
    public AbstractResponse getPlayerActivityDetail(long playerId, ActivityData activityData, int detailId) {
        long activityId = activityData.getId();
        ResPlayerLevelPackDetailInfo detailInfo = new ResPlayerLevelPackDetailInfo(Code.SUCCESS);
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
        detailInfo.detailInfo = new ArrayList<>();
        PlayerLevelPackDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(activityId, baseCfgBeanMap.get(detailId), playerActivityData.get(detailId));
        detailInfo.detailInfo.add(baseActivityDetailInfo);
        return detailInfo;
    }

    /**
     * 构建玩家刮刮乐活动明细信息
     */
    @Override
    public PlayerLevelPackDetailInfo buildPlayerActivityDetail(long activityId, BaseCfgBean baseCfgBean, PlayerActivityData playerActivityData) {
        if (baseCfgBean instanceof PlayerLevelPackCfg cfg && playerActivityData instanceof PlayerLevelPackData packData) {
            PlayerLevelPackDetailInfo info = new PlayerLevelPackDetailInfo();
            info.activityId = activityId;
            info.detailId = cfg.getId();
            info.buyPrice = cfg.getPay();
            // 奖励信息
            info.rewardItems = ItemUtils.buildItemInfo(cfg.getLevelRewards());
            info.claimStatus = packData.getClaimStatus();
            info.remainTime = packData.getBuyEndTime() - System.currentTimeMillis();
            return info;
        }
        return null;
    }

    /**
     * 玩家购买玩家等级礼包
     */
    @Override
    public void buyActivityGift(Player player, ActivityData activityData, int giftId) {
        ResActivityBuyGift res = new ResActivityBuyGift(Code.SUCCESS);
        res.activityId = activityData.getId();
        long playerId = player.getId();
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(giftId);
        if (baseCfgBean instanceof PlayerLevelPackCfg cfg) {
            CommonResult<ItemOperationResult> addItems = playerPackService.addItems(playerId, cfg.getLevelRewards(), "PlayerLevelPackBuyGift");
            if (!addItems.success()) {
                log.error("等级礼包自动领奖失败 playerId:{} activityData:{}", playerId, activityData);
                res.code = Code.UNKNOWN_ERROR;
                activityManager.sendToPlayer(playerId, res);
                return;
            }
            res.itemInfos = ItemUtils.buildItemInfo(cfg.getLevelRewards());
            activityManager.sendToPlayer(playerId, res);
        }
    }

    /**
     * 获取玩家等级礼包活动类型信息（前端展示）
     */
    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(long playerId, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResPlayerLevelPackTypeInfo cardTypeInfo = new ResPlayerLevelPackTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cardTypeInfo;
        }
        cardTypeInfo.activityData = new ArrayList<>();
        for (List<BaseActivityDetailInfo> baseActivityDetailInfos : allDetailInfo.values()) {
            PlayerLevelPackActivity activity = new PlayerLevelPackActivity();
            activity.detailInfos = new ArrayList<>();
            cardTypeInfo.activityData.add(activity);
            for (BaseActivityDetailInfo baseActivityDetailInfo : baseActivityDetailInfos) {
                if (baseActivityDetailInfo instanceof PlayerLevelPackDetailInfo info) {
                    activity.detailInfos.add(info);
                }
            }
        }
        return cardTypeInfo;
    }

    /**
     * 构建前端活动信息
     */
    @Override
    public ActivityInfo buildActivityInfo(long playerId, ActivityData activityData) {
        Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
        int claimStatus = 0;
        if (CollectionUtil.isNotEmpty(playerActivityData)) {
            for (PlayerActivityData data : playerActivityData.values()) {
                if (data.getClaimStatus() == ActivityConstant.ClaimStatus.CAN_CLAIM) {
                    claimStatus = data.getClaimStatus();
                    break;
                }
            }
        }
        return ActivityBuilder.buildActivityInfo(activityData, claimStatus);
    }

    @Override
    public List<BaseCfgBean> getDetailCfgBean() {
        return new ArrayList<>(GameDataManager.getPlayerLevelPackCfgList());
    }

    @Override
    public Class<PlayerLevelPackCfg> getDetailDataClass() {
        return PlayerLevelPackCfg.class;
    }
}
