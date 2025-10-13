package com.jjg.game.activity.piggybank.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.piggybank.data.PiggyBankData;
import com.jjg.game.activity.piggybank.message.bean.PiggyBankActivityInfo;
import com.jjg.game.activity.piggybank.message.bean.PiggyBankDetailInfo;
import com.jjg.game.activity.piggybank.message.res.ResPiggyBankActivityInfos;
import com.jjg.game.activity.piggybank.message.res.ResPiggyBankClaimRewards;
import com.jjg.game.activity.piggybank.message.res.ResPiggyBankDetailInfo;
import com.jjg.game.activity.privilegecard.data.PlayerPrivilegeCard;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.PiggyBankCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PiggyBankController
 * 储钱罐活动控制器
 * 继承自 BaseActivityController，实现储钱罐相关活动逻辑
 */
@Component
public class PiggyBankController extends BaseActivityController {

    // 日志记录
    private final Logger log = LoggerFactory.getLogger(PiggyBankController.class);

    // 邮件服务，用于发放奖励
    private final MailService mailService;

    // 构造方法，注入 MailService
    public PiggyBankController(MailService mailService) {
        this.mailService = mailService;
    }

    /**
     * 玩家参加储钱罐活动
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动子项ID
     * @param times        购买次数（一般为1）
     * @return 返回响应对象
     */
    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        ResPiggyBankDetailInfo res = null;
        long playerId = player.getId();

        // 获取活动详细配置
        Map<Integer, PiggyBankCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        PiggyBankCfg cfg = baseCfgBeanMap.get(detailId);
        // 判断配置是否有储钱罐活动配置
        if (cfg != null) {
            long timeMillis = System.currentTimeMillis();
            String lockKey = playerActivityDao.getLockKey(playerId, activityData.getId());

            // 分布式锁，防止并发购买
            redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);

            PiggyBankData piggyBankData = null;
            try {
                // 获取玩家活动数据
                Map<Integer, PiggyBankData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());

                // 获取或初始化子活动数据
                piggyBankData = playerActivityData.computeIfAbsent(detailId, key -> new PiggyBankData(activityData.getId(), activityData.getRound()));

                // 玩家已参加过，直接返回错误
                if (piggyBankData.getBuyTime() > 0) {
                    log.error("玩家参加活动失败 玩家已经参加过 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
                    return res;
                }

                // 设置购买时间
                piggyBankData.setBuyTime(timeMillis);
                piggyBankData.setClaimStatus(ActivityConstant.ClaimStatus.ALREADY_BUG);
                // 判断是否已满额
                if (piggyBankData.getProgress() >= cfg.getFullup()) {
                    piggyBankData.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
                }

                // 保存玩家活动数据
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), playerActivityData);
            } catch (Exception e) {
                log.error("玩家参加活动失败 出现异常,playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
            } finally {
                redisLock.unlock(lockKey);
            }

            // 日志记录玩家参加活动
            if (piggyBankData != null) {
                activityLogger.sendPiggyBankJoin(player, activityData, piggyBankData, cfg.getName(), detailId);
            }

            // 构建响应
            res = new ResPiggyBankDetailInfo(Code.SUCCESS);
            res.detailInfo = new ArrayList<>();
            res.detailInfo.add(buildPlayerActivityDetail(activityData.getId(), cfg, piggyBankData));
        } else {
            // 配置错误
            log.error("玩家参加活动失败 活动配置为空playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
        }

        return res;
    }

    /**
     * 添加玩家活动进度
     *
     * @param playerId             玩家ID
     * @param activityData         活动数据
     * @param progress             增加进度
     * @param activityTargetKey    触发key
     * @param additionalParameters 附加参数（用于过滤非金币道具）
     * @return 是否可以领取奖励
     */
    @Override
    public boolean addPlayerProgress(long playerId, ActivityData activityData, long progress, long activityTargetKey, Object additionalParameters) {
        // 如果不是金币，则不增加储钱罐进度
        if (additionalParameters instanceof Integer itemId && !itemId.equals(ItemUtils.getGoldItemId())) {
            return false;
        }

        // 获取全局配置，计算每万元金币进度
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.PiggyBank.INCOME_PER_TEN_THOUSAND);
        BigDecimal baseAdd = BigDecimal.valueOf(progress)
                .multiply(BigDecimal.valueOf(globalConfigCfg.getIntValue()))
                .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN);

        long activityId = activityData.getId();
        Map<Integer, PiggyBankCfg> baseCfgBeanMap = getDetailCfgBean(activityData);

        boolean changeStatus = false;
        String lockKey = playerActivityDao.getLockKey(playerId, activityId);
        // 分布式锁
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            Map<Integer, PiggyBankData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);

            // 遍历所有储钱罐子活动
            for (Map.Entry<Integer, PiggyBankCfg> entry : baseCfgBeanMap.entrySet()) {
                PiggyBankCfg cfg = entry.getValue();
                PiggyBankData piggyBankData = playerActivityData.computeIfAbsent(entry.getKey(),
                        key -> new PiggyBankData(activityData.getId(), activityData.getRound()));

                // 如果进度已经满了，跳过
                if (piggyBankData.getProgress() >= cfg.getFullup()) {
                    continue;
                }

                // 计算加成值
                long addValue = baseAdd.multiply(BigDecimal.valueOf(cfg.getWeight()))
                        .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN)
                        .longValue();

                // 更新进度
                piggyBankData.setProgress(Math.min(cfg.getFullup(), piggyBankData.getProgress() + addValue));

                // 判断是否可领取奖励
                if (piggyBankData.getProgress() >= cfg.getFullup()) {
                    if (piggyBankData.getBuyTime() > 0) {
                        piggyBankData.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
                    }
                    piggyBankData.setFullTime(System.currentTimeMillis());
                    changeStatus = true;
                }
            }

            // 保存更新后的数据
            if (!playerActivityData.isEmpty()) {
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityId, playerActivityData);
            }
        } catch (Exception e) {
            log.error("储钱罐添加玩家进度异常 playerId:{}  activityId:{} ", playerId, activityData.getId(), e);
        } finally {
            redisLock.unlock(lockKey);
        }

        return changeStatus;
    }


    /**
     * 玩家领取储钱罐奖励
     */
    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        ResPiggyBankClaimRewards res = new ResPiggyBankClaimRewards(Code.SUCCESS);
        long playerId = player.getId();
        String lockKey = playerActivityDao.getLockKey(playerId, activityData.getId());

        Map<Integer, PiggyBankCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        PiggyBankCfg cfg = baseCfgBeanMap.get(detailId);

        if (cfg != null) {
            PiggyBankData data = null;
            CommonResult<ItemOperationResult> addedItems = null;

            redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
            try {
                // 获取玩家储钱罐数据
                Map<Integer, PiggyBankData> dataMap = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
                data = dataMap.get(detailId);

                // 数据不存在，返回参数错误
                if (data == null) {
                    res.code = Code.PARAM_ERROR;
                    return res;
                }

                // 如果不能领取，返回请求错误
                if (data.getClaimStatus() != ActivityConstant.ClaimStatus.CAN_CLAIM) {
                    res.code = Code.ERROR_REQ;
                    return res;
                }

                // 添加奖励到背包
                addedItems = playerPackService.addItems(playerId, cfg.getGetitem(), "privilegeCardRewords");
                if (!addedItems.success()) {
                    res.code = Code.UNKNOWN_ERROR;
                    return res;
                }

                // 重置储钱罐数据
                resetPiggyBankData(data);

                // 保存数据
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), dataMap);
            } catch (Exception e) {
                log.error("领取每日奖金异常 playerId:{} activityId:{}", playerId, activityData.getId(), e);
            } finally {
                redisLock.unlock(lockKey);
            }

            // 记录日志
            if (data != null && addedItems != null && addedItems.success()) {
                activityLogger.sendPiggyBankRewards(player, activityData, data, cfg.getWeight(), cfg.getName(),
                        addedItems.data, cfg.getGetitem());
            }

            // 构建响应
            res.activityId = activityData.getId();
            res.detailId = detailId;
            res.infoList = ItemUtils.buildItemInfo(cfg.getGetitem());
            res.detailInfo = buildPlayerActivityDetail(activityData.getId(), cfg, data);
        }

        return res;
    }

    /**
     * 获取玩家储钱罐活动详情
     */
    @Override
    public AbstractResponse getPlayerActivityDetail(long playerId, ActivityData activityData, int detailId) {
        long activityId = activityData.getId();
        ResPiggyBankDetailInfo detailInfo = new ResPiggyBankDetailInfo(Code.SUCCESS);

        // 获取活动配置与玩家数据
        ActivityData data = activityManager.getActivityData().get(activityId);
        Map<Integer, PiggyBankCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        Map<Integer, PlayerPrivilegeCard> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, data.getType(), activityId);

        // 构建返回详情
        detailInfo.detailInfo = new ArrayList<>();
        PiggyBankDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(activityId, baseCfgBeanMap.get(detailId), playerActivityData.get(detailId));
        detailInfo.detailInfo.add(baseActivityDetailInfo);

        return detailInfo;
    }

    /**
     * 构建玩家储钱罐活动详情
     */
    @Override
    public PiggyBankDetailInfo buildPlayerActivityDetail(long activityId, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (baseCfgBean instanceof PiggyBankCfg cfg) {
            PiggyBankDetailInfo info = new PiggyBankDetailInfo();
            info.activityId = activityId;
            info.detailId = baseCfgBean.getId();
            info.rechargePrice = cfg.getPay(); // 充值金额
            info.rewardItems = ItemUtils.buildItemInfo(cfg.getGetitem()); // 奖励道具

            // 设置玩家数据
            if (data instanceof PiggyBankData piggyBankData) {
                info.claimStatus = piggyBankData.getClaimStatus();
                info.progress = piggyBankData.getProgress();
                if (piggyBankData.getFullTime() > 0) {
                    info.remainTime = (piggyBankData.getFullTime() + (long) cfg.getResetime() * TimeHelper.ONE_DAY_OF_MILLIS) - System.currentTimeMillis();
                }
                info.isFull = piggyBankData.getFullTime() > 0;
            }
            return info;
        }
        return null;
    }

    /**
     * 根据类型构建玩家活动信息列表
     */
    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(long playerId, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResPiggyBankActivityInfos activityInfos = new ResPiggyBankActivityInfos(Code.SUCCESS);

        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return activityInfos;
        }

        activityInfos.activityData = new ArrayList<>();
        for (Map.Entry<Long, List<BaseActivityDetailInfo>> entry : allDetailInfo.entrySet()) {
            PiggyBankActivityInfo activityInfo = new PiggyBankActivityInfo();
            activityInfo.detailInfos = new ArrayList<>();
            activityInfos.activityData.add(activityInfo);

            // 只添加储钱罐类型的详情
            for (BaseActivityDetailInfo baseActivityDetailInfo : entry.getValue()) {
                if (baseActivityDetailInfo instanceof PiggyBankDetailInfo info) {
                    activityInfo.detailInfos.add(info);
                }
            }
        }
        return activityInfos;
    }

    @Override
    public Map<Integer, PlayerActivityData> checkPlayerDataAndResetOnRequest(long playerId, ActivityData activityData) {
        return resetData(playerId, activityData);
    }

    /**
     * 检查玩家数据并在条件满足时重置
     */
    @Override
    public void checkPlayerDataAndResetOnLogin(long playerId, ActivityData activityData) {
        super.checkPlayerDataAndResetOnLogin(playerId, activityData);
        resetData(playerId, activityData);
    }

    /**
     * 储钱罐重置
     *
     * @param playerId     玩家id
     * @param activityData 活动数据
     */
    private Map<Integer, PlayerActivityData> resetData(long playerId, ActivityData activityData) {
        long timeMillis = System.currentTimeMillis();
        boolean change = false;
        Map<Integer, PiggyBankCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        Map<Integer, PlayerActivityData> playerActivityData = null;
        //加锁防止重置数据时请求领奖导致多发奖励
        String lockKey = playerActivityDao.getLockKey(playerId, activityData.getId());
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
            // 遍历所有储钱罐数据
            for (Map.Entry<Integer, PlayerActivityData> piggyBankDataEntry : playerActivityData.entrySet()) {
                if (piggyBankDataEntry.getValue() instanceof PiggyBankData piggyBankData) {
                    PiggyBankCfg cfg = baseCfgBeanMap.get(piggyBankDataEntry.getKey());
                    if (cfg != null && piggyBankData.getFullTime() > 0) {
                        long resetTime = piggyBankData.getFullTime() + (long) TimeHelper.ONE_DAY_OF_MILLIS * cfg.getResetime();
                        // 判断是否到达重置时间
                        if (resetTime <= timeMillis) {
                            if (piggyBankData.getClaimStatus() == ActivityConstant.ClaimStatus.CAN_CLAIM) {
                                // 邮件发奖
                                mailService.addCfgMail(playerId, ActivityConstant.PiggyBank.MAIL_ID, ItemUtils.buildItems(cfg.getGetitem()));
                            }
                            // 重置数据
                            resetPiggyBankData(piggyBankData);
                            change = true;
                        }
                    }
                }
                // 保存数据
                if (change) {
                    playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), playerActivityData);
                }
            }
        } catch (Exception e) {
            log.info("储钱罐重置数据失败");
        } finally {
            redisLock.unlock(lockKey);
        }
        return playerActivityData;
    }

    /**
     * 重置玩家储钱罐数据
     */
    private void resetPiggyBankData(PiggyBankData piggyBankData) {
        piggyBankData.setFullTime(0);
        piggyBankData.setProgress(0);
        piggyBankData.setBuyTime(0);
        piggyBankData.setClaimStatus(ActivityConstant.ClaimStatus.NOT_CLAIM);
    }

    @Override
    public Map<Integer, PiggyBankCfg> getDetailCfgBean(ActivityData activityData) {
        return GameDataManager.getPiggyBankCfgList()
                .stream()
                .filter(cfg -> activityData.getValue().contains(cfg.getId()))
                .collect(Collectors.toMap(BaseCfgBean::getId, cfg -> cfg));
    }

}
