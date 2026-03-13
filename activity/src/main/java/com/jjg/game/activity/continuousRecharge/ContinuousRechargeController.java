package com.jjg.game.activity.continuousRecharge;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.continuousRecharge.data.ContinuousRechargeActivityData;
import com.jjg.game.activity.continuousRecharge.data.DailyContinuousData;
import com.jjg.game.activity.continuousRecharge.message.*;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.gameevent.*;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 连续充值活动控制器
 * <p>
 * 活动分为两个阶段：
 * Phase 1: 七日连充 —— 玩家需连续7天充值，每天达成不同档位的充值目标可累计返利比例，
 * 7天全部完成后（或月底结算时）可领取累计充值 * 返利比例对应的金币奖励。
 * Phase 2: 累计福利 —— 七日连充结算后进入此阶段，包含每日充值任务和本月累计充值任务，
 * 达标后可领取配置的道具奖励。
 *
 * @author 11
 * @date 2026/3/5
 */
@Component
public class ContinuousRechargeController extends BaseActivityController implements GameEventListener, ConfigExcelChangeListener, GmListener {

    @Autowired
    private MailService mailService;

    private final int DETAIL_ID = 1;

    //连续充值活动按天分组 dayIndex ->cfgList
    private Map<Integer, List<ContinuouschargingCfg>> continuouschargingCfgMap = null;
    //福利每天活动分组 group ->cfgList
    private Map<Integer, List<CumulativebenefitsCfg>> welfareDailyCfgMap = null;
    //福利活动中月度配置
    private List<CumulativebenefitsCfg> welfareMonthCfgList = null;

    //福利活动的配置下标值，即当天轮到第几套配置
    private int wefareCfgIndexId = 0;
    //今天的配置
    private Map<Integer, CumulativebenefitsCfg> todayWelfareCfgMap = null;

    //GM调试用：时间(毫秒)
    private long debugMills = 0;

    private BigDecimal TEN_THOUSAND = BigDecimal.valueOf(10000);

    /**
     * 获取当前时间毫秒数（GM可覆盖）
     */
    private long currentTimeMillis() {
        return this.debugMills > 0 ? this.debugMills : System.currentTimeMillis();
    }

    /**
     * 获取当前日期的yyyyMMdd格式整数（GM可覆盖）
     */
    private int getToday() {
        if (this.debugMills > 0) {
            LocalDate date = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(this.debugMills), ZoneId.systemDefault());
            return date.getYear() * 10000 + date.getMonthValue() * 100 + date.getDayOfMonth();
        }
        return TimeHelper.getDayNumerical();
    }

    /**
     * 获取当前日期的yyMMdd格式整数
     */
    private int getToday2() {
        if (this.debugMills > 0) {
            LocalDate date = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(this.debugMills), ZoneId.systemDefault());
            return (date.getYear() % 2000) * 10000 + date.getMonthValue() * 100 + date.getDayOfMonth();
        }
        return TimeHelper.getDayNumerical2();
    }

    @Override
    public void init() {
        loadContinuouschargingCfg();
        loadCumulativebenefitsCfg();
    }

    /**
     * 玩家参加连续充值活动
     * <p>
     * 为了兼容 joinActivity 协议，这里的detailId可以固定为1
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动子项ID
     * @param times        请求参加的次数
     * @return 活动响应消息
     */
    @Override
    public ResContinuousRecharge joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        ResContinuousRecharge res = new ResContinuousRecharge(Code.SUCCESS);
        try {
            Pair<Integer, long[]> phase = getPhase(activityData, false);
            if (phase.getFirst() == ActivityConstant.ContinuousRecharge.PHASE_NO_BEGIN) {
                log.warn("该活动还未开启，无法参加  playerId = {}", player.getId());
                res.code = Code.FORBID;
                return res;
            } else if (phase.getFirst() == ActivityConstant.ContinuousRecharge.PHASE_OVER) {
                log.warn("该活动已结束，无法参加  playerId = {}", player.getId());
                res.code = Code.FORBID;
                return res;
            }

            Map<Integer, PlayerActivityData> dataMap = playerActivityDao.getPlayerActivityData(player.getId(), ActivityType.CONTINUOUS_RECHARGE, activityData.getId());
            if (CollectionUtil.isNotEmpty(dataMap)) {
                log.warn("不能重复参加活动 playerId = {},activityId = {}", player.getId(), activityData.getId());
                res.code = Code.REPEAT_OP;
                return res;
            }

            ContinuousRechargeActivityData data = new ContinuousRechargeActivityData();
            data.setActivityId(activityData.getId());
            data.setCurrentDayIndex(0);
            data.setTotalRebateRate(0);
            data.setJoinTime(currentTimeMillis());
            data.setLastCheckDate(getToday());

            dataMap = new HashMap<>();
            dataMap.put(DETAIL_ID, data);

            playerActivityDao.savePlayerActivityData(player.getId(), activityData.getType(), activityData.getId(), dataMap);

            res.activityData = new ArrayList<>();
            res.activityData.add((ContinuousRechargeDetailInfo) buildPlayerActivityDetail(player, activityData, null, data));
            log.info("玩家参加连续充值活动 playerId={}", player.getId());
            return res;
        } catch (Exception e) {
            log.warn("参加连续充值活动异常", e);
            res.code = Code.EXCEPTION;
            return res;
        }
    }

    @Override
    public List<BaseActivityDetailInfo> getBaseActivityDetailInfos(ActivityData activityData, Map<Integer, ? extends BaseCfgBean> baseCfgBeanMap, Player player, Map<Integer, PlayerActivityData> playerActivityDataMap) {
        PlayerActivityData playerActivityData = null;
        if (playerActivityDataMap != null && !playerActivityDataMap.isEmpty()) {
            playerActivityData = playerActivityDataMap.get(DETAIL_ID);
        }

        if (playerActivityData == null) {
            playerActivityData = new ContinuousRechargeActivityData();
            playerActivityData.setActivityId(activityData.getId());
        }
        return List.of(buildPlayerActivityDetail(player, activityData, null, playerActivityData));
    }

    @Override
    public Map<Integer, ContinuouschargingCfg> getDetailCfgBean(ActivityData activityData) {
        return GameDataManager.getContinuouschargingCfgMap();
    }

    /**
     * @param player       玩家对象
     * @param activityData 活动数据
     * @return 重置后的玩家活动数据，null表示无需重置
     */
    @Override
    public Map<Integer, PlayerActivityData> checkPlayerDataAndResetOnRequest(Player player, ActivityData activityData) {
        Map<Integer, PlayerActivityData> dataMap = playerActivityDao.getPlayerActivityData(player.getId(), activityData.getType(), activityData.getId());
        return resetCheck(player.getId(), activityData, dataMap);
    }

    @Override
    public ResContinuousRecharge getPlayerActivityInfoByTypeRes(Player player, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResContinuousRecharge res = new ResContinuousRecharge(Code.SUCCESS);

        res.activityData = new ArrayList<>();
        for (List<BaseActivityDetailInfo> baseActivityDetailInfos : allDetailInfo.values()) {
            for (BaseActivityDetailInfo baseActivityDetailInfo : baseActivityDetailInfos) {
                if (baseActivityDetailInfo instanceof ContinuousRechargeDetailInfo info) {
                    res.activityData.add(info);
                }
            }
        }
        return res;
    }

    /**
     * 七日连充天数切换辅助方法
     * <p>
     * 如果前一天完成了任务（finished=true），则推进到下一天；
     * 如果前一天未完成，重置回第1天（index=0），清空所有每日数据。
     *
     * @param data          活动数据
     * @param lastCheckDate 上次检查的日期（yyyyMMdd）
     * @param today         今天的日期（yyyyMMdd）
     * @return true表示数据发生了变更
     */
    private boolean handleContinuousDaySwitch(long playerId, ContinuousRechargeActivityData data, int lastCheckDate, int today) {
        if (lastCheckDate == 0 || lastCheckDate == today) {
            return false;
        }

        //这是天数推进前的index
        int currentDayIndex = data.getCurrentDayIndex();
        //最近一天的数据
        DailyContinuousData lastdayData = data.queryDailyContinuousByDay(currentDayIndex);
        if (lastdayData == null) {
            // 前一天未完成，无需重置
//            data.setCurrentDayIndex(0);
//            data.setTotalRebateRate(0);
//            data.setDailyRechargeMap(null);
//            log.info("七日连充前一天未完成，重置到第1天,playerId={},dayIndex = {}", playerId, currentDayIndex);
            return false;
        }

        if (lastdayData.canNext()) {
            // 前一天完成了，推进到下一天
            int nextDayIndex = currentDayIndex + 1;
            if (nextDayIndex < ActivityConstant.ContinuousRecharge.CONTINUOUS_DAYS) {
                data.setCurrentDayIndex(nextDayIndex);
                log.debug("七日连充推进，playerId={},dayIndex = {}", playerId, nextDayIndex);
            }
        } else {
            // 前一天未完成，只将前一天的数据清除
            data.clearContinuousCurrentData();
            log.info("七日连充前一天未完成，清除最近一天的数据,playerId={},currentDayIndex = {}", playerId, currentDayIndex);
        }
        return true;
    }

    /**
     * 累计福利跨天切换辅助方法
     * <p>
     * 跨天后重置每日福利数据中的领取状态，准备新一天的任务。
     *
     * @param data          活动数据
     * @param lastCheckDate 上次检查日期
     * @param today         今天日期
     * @return true表示数据发生了变更
     */
    private boolean handleWelfareDaySwitch(long playerId, ContinuousRechargeActivityData data, int lastCheckDate, int today) {
        if (lastCheckDate == 0 || lastCheckDate == today) {
            return false;
        }

        if (data.getDailyWelfareData() == null) {
            return false;
        }

        int date = getToday();
        if (data.getDailyWelfareData().getDate() == date) {
            return false;
        }
        data.setDailyWelfareData(null);
        log.debug("跨天重置连续充值之福利每日活动 playerId = {}", playerId);
        return true;
    }

    /**
     * @param playerId     玩家ID
     * @param activityData 活动数据
     */
    @Override
    public void checkPlayerDataAndResetOnLogin(long playerId, ActivityData activityData) {
        Map<Integer, PlayerActivityData> dataMap = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
        resetCheck(playerId, activityData, dataMap);
    }

    /**
     * 七日连充奖励邮件补发
     * <p>
     * 计算逻辑与 claimContinuousReward 一致：
     * 累计充值 * 返利比例 / 10000 * global#123配置的兑换比例
     * 通过邮件发放金币奖励。
     *
     * @param playerId 玩家ID
     * @param data     活动数据
     */
    private void sendContinuousRewardByMail(long playerId, ContinuousRechargeActivityData data) {
        try {
            if (data.getRebateGoldNum() > 0) {
                return;
            }
            BigDecimal rebateMoney = calRebateMoney(data);
            long goldAmount = calRebateGoldNum(rebateMoney);
            if (goldAmount <= 0) {
                log.info("七日连充邮件补发：奖励金额为0，跳过 playerId={}", playerId);
                return;
            }
            data.setRebateGoldNum(goldAmount);

            // 构建Item列表
            List<Item> items = new ArrayList<>();
            items.add(new Item(ItemUtils.getGoldItemId(), goldAmount));
            // 通过邮件发放
            mailService.addCfgMail(playerId, ActivityConstant.ContinuousRecharge.CONTINUOUS_MAIL_ID, items, AddType.ACTIVITY_CONTINUOUS_RECHARGE_REWARD);
            log.info("七日连充邮件补发成功 playerId={}, goldAmount={}", playerId, goldAmount);
        } catch (Exception e) {
            log.error("七日连充邮件补发异常 playerId={}", playerId, e);
        }
    }

    /**
     * 累计福利未领取每日任务奖励邮件补发
     * <p>
     * 遍历 dailyWelfarMap 中的历史数据，对于未领取（rece=false）的 WelfareData，
     * 检查每个 CumulativebenefitsCfg（type=WELFARE_DAILY_TYPE）的达标条件，
     * 如果玩家充值额已达标，则通过邮件补发奖励道具。
     *
     * @param playerId 玩家ID
     * @param data     活动数据
     */
    private void sendWelfareDailyRewardsByMail(long playerId, ContinuousRechargeActivityData data) {
        try {
            if (data.getDailyWelfareData() == null) {
                return;
            }

            // 获取所有每日任务类型的配置
            Map<Integer, CumulativebenefitsCfg> cfgMap = GameDataManager.getCumulativebenefitsCfgMap();
            if (CollectionUtil.isEmpty(cfgMap)) {
                return;
            }

            // 今天的日期（今天的数据不补发，玩家今天还有机会手动领取）
            int date = getToday();
            // 跳过今天的数据（今天还可以手动领取）
            if (data.getDailyWelfareData().getDate() == date) {
                return;
            }

            for (CumulativebenefitsCfg cfg : this.todayWelfareCfgMap.values()) {
                if (cfg.getType() != ActivityConstant.ContinuousRecharge.WELFARE_DAILY_TYPE) {
                    continue;
                }

                // 跳过已经领取过的
                if (data.getDailyWelfareData().rece(cfg.getId())) {
                    continue;
                }

                long target = cfg.getCondition();
                if (data.getDailyWelfareData().getRechargeNum().longValue() >= target) {
                    // 达标但未领取，通过邮件补发
                    mailService.addCfgMail(playerId, ActivityConstant.ContinuousRecharge.WELFARE_DAILY_MAIL_ID, ItemUtils.buildItems(cfg.getRewards()), AddType.ACTIVITY_WELFARE_DAILY_REWARD);

                    // 标记已补发，避免重复补发
                    data.getDailyWelfareData().receReward(cfg.getId());
                    log.info("累计福利每日任务邮件补发 playerId={}, day={}, cfgId={}, rechargeNum={}", playerId, data.getDailyWelfareData().getDate(), cfg.getId(), data.getDailyWelfareData().getRechargeNum());
                }
            }
        } catch (Exception e) {
            log.error("累计福利未领取奖励邮件补发异常 playerId={}", playerId, e);
        }
    }

    /**
     * 累计福利未领取任务奖励邮件补发
     *
     * @param playerId
     * @param data
     */
    private void sendWelfareMonthRewardsByMail(long playerId, ContinuousRechargeActivityData data) {
        try {
            //获取月累计充值
            if (data.getWelfarMonthRechargeNum() == null) {
                return;
            }
            long monthRecharge = data.getWelfarMonthRechargeNum().longValue();
            for (CumulativebenefitsCfg cfg : this.welfareMonthCfgList) {
                //是否配置奖励
                if (CollectionUtil.isEmpty(cfg.getRewards())) {
                    continue;
                }

                //检查是否达成条件
                if (monthRecharge < cfg.getCondition()) {
                    continue;
                }

                //检查是否领取
                if (data.welfarRece(cfg.getId())) {
                    continue;
                }
                // 发送邮件
                mailService.addCfgMail(playerId, ActivityConstant.ContinuousRecharge.WELFARE_MONTH_MAIL_ID, ItemUtils.buildItems(cfg.getRewards()), AddType.ACTIVITY_WELFARE_MONTHLY_REWARD);
                // 标记已补发，避免重复补发
                data.welfareReward(cfg.getId());
                log.info("福利月奖励邮件补发 playerId={}, cfgId={}, rechargeNum={}", playerId, cfg.getId(), monthRecharge);
            }
        } catch (Exception e) {
            log.error("福利月奖励邮件补发异常 playerId={}", playerId, e);
        }
    }


    /**
     * 处理游戏事件（充值事件）
     * <p>
     * 监听玩家充值事件，获取连续充值类型的活动数据，
     * 根据当前阶段分发到对应的充值处理逻辑。
     *
     * @param gameEvent 游戏事件
     */
    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof PlayerEventCategory.PlayerRechargeEvent event) {
            Player player = event.getPlayer();
            Order order = event.getOrder();
            long playerId = player.getId();

            try {
                if (StringUtils.isEmpty(order.getDesc())) {
                    log.debug("该订单没有备注字段，不参与七日连充活动 orderId = {},playerId = {}", order.getId(), playerId);
                    return;
                }

                JSONObject json = JSONObject.parseObject(order.getDesc());
                Integer subType = json.getIntValue("subType");

                if (subType != ActivityType.CONTINUOUS_RECHARGE.getType()) {
                    return;
                }
                Integer cid = json.getIntValue("cid");
                if (cid == null || (cid != ActivityConstant.ContinuousRecharge.PHASE_CONTINUOUS && cid != ActivityConstant.ContinuousRecharge.PHASE_WELFARE)) {
                    log.warn("订单备注信息中的cid错误 cid = {}", cid);
                    return;
                }

                // 获取 CONTINUOUS_RECHARGE 类型的所有活动数据
                Map<Long, ActivityData> activityDataMap = activityManager.getActivityTypeData().get(ActivityType.CONTINUOUS_RECHARGE);
                if (CollectionUtil.isEmpty(activityDataMap)) {
                    log.warn("未获取到连续充值活动的数据 playerId={}", playerId);
                    return;
                }

                // 遍历所有连续充值活动
                for (ActivityData activityData : activityDataMap.values()) {
                    if (!activityData.canRun()) {
                        log.warn("该活动没有开启 playerId={},activityId = {}", playerId, activityData.getId());
                        continue;
                    }

                    // 获取玩家的活动数据
                    Map<Integer, PlayerActivityData> dataMap = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
                    dataMap = resetCheck(playerId, activityData, dataMap);
                    if (CollectionUtil.isEmpty(dataMap)) {
                        continue;
                    }

                    ContinuousRechargeActivityData data = (ContinuousRechargeActivityData) dataMap.get(DETAIL_ID);
                    if (data == null) {
                        continue;
                    }

                    Pair<Integer, long[]> phase = getPhase(activityData, true);
                    if (phase.getFirst() == ActivityConstant.ContinuousRecharge.PHASE_CONTINUOUS) {
                        // Phase 1: 七日连充充值处理
                        handleContinuousRecharge(player, activityData, data.getCurrentDayIndex(), data, dataMap, order, phase);
                    } else if (phase.getFirst() == ActivityConstant.ContinuousRecharge.PHASE_WELFARE) {
                        // Phase 2: 累计福利充值处理
                        handleWelfareRecharge(player, activityData, data, dataMap, order, phase);
                    }
                }
            } catch (Exception e) {
                log.error("连续充值活动处理充值事件异常 playerId={}", playerId, e);
            }
        } else if (gameEvent instanceof ClockEvent event) {
            if (event.getHour() == 0) {
                genTodayWefareCfgIds();
            }
        }
    }

    /**
     * 七日连充充值处理
     * <p>
     * 更新当日充值金额，检查各档任务是否达成：
     * - 使用 isTaskAlreadyClaimed / markTaskClaimed 防止重复累加返利比例
     * - 达成任意一档即标记当天已完成（finished）
     * - 保存数据并推送更新消息
     *
     * @param player       玩家ID
     * @param activityData 活动配置
     * @param data         连续充值活动数据
     * @param dataMap      玩家活动数据Map
     * @param order        充值订单
     */
    private void handleContinuousRecharge(Player player, ActivityData activityData, int dayIndex, ContinuousRechargeActivityData data,
                                          Map<Integer, PlayerActivityData> dataMap, Order order, Pair<Integer, long[]> phase) {
        long now = currentTimeMillis();
        if (now < phase.getSecond()[0] || now > phase.getSecond()[1]) {
            log.warn("当前连充活动不在时间范围内 playerId = {},now = {},begin = {},end = {}", player.getId(), now, phase.getSecond()[0], phase.getSecond()[1]);
            return;
        }
        log.debug("七日连充充值处理 playerId = {},dayIndex = {}", player.getId(), dayIndex);

        // 更新连续充值中当天充值记录
        DailyContinuousData dailyContinuousData = data.updateDailyContinuousData(dayIndex, order.getPrice(), getToday());

        // 按天分组获取当天的配置
        List<ContinuouschargingCfg> todayCfgList = this.continuouschargingCfgMap.get(dayIndex);
        if (todayCfgList == null) {
            return;
        }

        for (ContinuouschargingCfg cfg : todayCfgList) {
            long rechargeNum = dailyContinuousData.getRechargeNum().longValue();
            if (rechargeNum >= cfg.getTask() && !dailyContinuousData.containsClaimedTaskId(cfg.getId())) {
                // 达成该档任务，累加返利比例
                dailyContinuousData.markTaskClaimed(cfg.getId());
                data.setTotalRebateRate(data.getTotalRebateRate() + cfg.getRebate());
                log.info("七日连充达成任务 playerId={}, dayIndex={}, cfgId={}, rebate={}", player.getId(), dayIndex, cfg.getId(), cfg.getRebate());
            }
        }

        // 保存数据
        dataMap.put(DETAIL_ID, data);
        playerActivityDao.savePlayerActivityData(player.getId(), activityData.getType(), activityData.getId(), dataMap);
        activityLogger.sendContinuousLog(player, order.getPrice(), activityData, data);
    }

    /**
     * 累计福利充值处理
     * <p>
     * 更新本月累计充值和今日福利充值金额，保存并推送更新。
     *
     * @param player       玩家ID
     * @param activityData 活动配置
     * @param data         连续充值活动数据
     * @param dataMap      玩家活动数据Map
     * @param order        充值订单
     */
    private void handleWelfareRecharge(Player player, ActivityData activityData, ContinuousRechargeActivityData data, Map<Integer, PlayerActivityData> dataMap, Order order, Pair<Integer, long[]> phase) {
        long now = currentTimeMillis();
        if (now < phase.getSecond()[0] || now > phase.getSecond()[1]) {
            log.warn("当前连充福利活动不在时间范围内 playerId = {},now = {},begin = {},end = {}", player.getId(), now, phase.getSecond()[0], phase.getSecond()[1]);
            return;
        }
        log.debug("处理七日连充活动的福利充值 playerId = {}", player.getId());
        // 更新今日福利充值
        int date = getToday();
        data.updateWelfareRechargeData(date, order.getPrice());
        // 保存数据
        dataMap.put(DETAIL_ID, data);
        playerActivityDao.savePlayerActivityData(player.getId(), activityData.getType(), activityData.getId(), dataMap);
        activityLogger.sendWelfarLog(player, order.getPrice(), activityData, data);
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.RECHARGE, EGameEventType.CLOCK_EVENT);
    }

    /**
     * 领取活动奖励
     * <p>
     * 根据当前阶段分发到不同的领取方法：
     * - Phase 1: 领取七日连充奖励
     * - Phase 2: 领取累计福利奖励
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     领取详情ID（Phase2中作为配置ID使用）
     * @return 响应消息
     */
    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        log.info("领取奖励 playerId = {}, detailId = {}", player.getId(), detailId);
        ResContinuousRechargeClaimRewards res = new ResContinuousRechargeClaimRewards(Code.SUCCESS);
        try {
            long playerId = player.getId();
            Map<Integer, PlayerActivityData> dataMap = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
            if (CollectionUtil.isEmpty(dataMap)) {
                log.warn("获取玩家活动数据失败 playerId = {},activityType = {}, detailId = {}", playerId, activityData.getType(), detailId);
                res.code = Code.NOT_FOUND;
                return res;
            }

            ContinuousRechargeActivityData data = (ContinuousRechargeActivityData) dataMap.get(DETAIL_ID);
            if (data == null) {
                log.warn("获取玩家活动数据失败1 playerId = {},activityType = {}, detailId = {}", playerId, activityData.getType(), detailId);
                res.code = Code.NOT_FOUND;
                return res;
            }

            Pair<Integer, long[]> phase = getPhase(activityData, true);
            CommonResult<List<Item>> rewardResult;
            if (phase.getFirst() == ActivityConstant.ContinuousRecharge.PHASE_CONTINUOUS) {
                rewardResult = claimContinuousReward(playerId, activityData, data, dataMap, phase);
            } else {
                rewardResult = claimWelfareReward(playerId, activityData, data, dataMap, detailId);
            }

            if (!rewardResult.success()) {
                res.code = rewardResult.code;
                return res;
            }

            res.rewards = ItemUtils.buildItemInfosByItem(rewardResult.data);
            res.detailInfos = new ArrayList<>();
            res.detailInfos.add((ContinuousRechargeDetailInfo) buildPlayerActivityDetail(player, activityData, null, data));
            return res;
        } catch (Exception e) {
            log.error("连续充值活动领取奖励异常", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 领取七日连充奖励
     * <p>
     * 领取条件：
     * 1. 7天全部完成（currentDayIndex >= 6 且最后一天 finished）
     * 2. 或者是月底最后一天（可提前结算）
     * <p>
     * 奖励计算：累计充值 * 返利比例 / 10000 * global#123.intValue
     * <p>
     * 领取后标记已结算，切换到Phase 2（累计福利）。
     *
     * @param playerId     玩家ID
     * @param activityData 活动配置
     * @param data         活动数据
     * @param dataMap      玩家活动数据Map
     * @return 领取结果响应
     */
    private CommonResult<List<Item>> claimContinuousReward(long playerId, ActivityData activityData, ContinuousRechargeActivityData data,
                                                           Map<Integer, PlayerActivityData> dataMap, Pair<Integer, long[]> phase) {
        CommonResult<List<Item>> result = new CommonResult<>(Code.SUCCESS);
        // 已经结算过
        if (data.getRebateGoldNum() > 0) {
            result.code = Code.REPEAT_OP;
            log.warn("该奖励已经领取过 playerId = {}", playerId);
            return result;
        }

        //检查是不是最后一天
        long entTime = phase.getSecond()[1];
        long now = currentTimeMillis();
        if (!TimeHelper.inSameDay(entTime, now)) {
            log.warn("未到阶段1活动的结算时间 playerId = {},endTime = {}", playerId, entTime);
            result.code = Code.FORBID;
            return result;
        }

        //计算七日充值返利金额
        BigDecimal rebateMoney = calRebateMoney(data);
        long goldAmount = calRebateGoldNum(rebateMoney);

        if (goldAmount > 0) {
            // 发放金币
            Map<Integer, Long> rewardItems = new HashMap<>();
            rewardItems.put(ItemUtils.getGoldItemId(), goldAmount);
            CommonResult<ItemOperationResult> addResult = playerPackService.addItems(playerId, rewardItems, AddType.ACTIVITY_CONTINUOUS_RECHARGE_REWARD);
            if (!addResult.success()) {
                result.code = addResult.code;
                return result;
            }
        }

        // 标记已结算
        data.setRebateGoldNum(goldAmount);
        // 保存数据
        dataMap.put(DETAIL_ID, data);
        playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), dataMap);

        result.data = ItemUtils.buildItemList(ItemUtils.getGoldItemId(), goldAmount);
        log.info("七日连充领取奖励 playerId={}, totalRecharge={}, rebateRate={}, goldAmount={}", playerId, data.calculateContinuousTotalRecharge(), data.getTotalRebateRate(), goldAmount);
        return result;
    }

    /**
     * 领取累计福利奖励
     * <p>
     * 根据配置类型区分：
     * - 每日任务（type=1）：检查今日充值是否达标，发放 CumulativebenefitsCfg.rewards
     * - 本月累计任务（type=2）：检查月累计充值是否达标，发放奖励
     *
     * @param playerId     玩家ID
     * @param activityData 活动配置
     * @param data         活动数据
     * @param dataMap      玩家活动数据Map
     * @param detailId     配置ID（CumulativebenefitsCfg的id）
     * @return 领取结果响应
     */
    private CommonResult<List<Item>> claimWelfareReward(long playerId, ActivityData activityData, ContinuousRechargeActivityData data, Map<Integer, PlayerActivityData> dataMap, int detailId) {
        CommonResult<List<Item>> result = new CommonResult<>(Code.SUCCESS);

        // 获取配置
        CumulativebenefitsCfg cfg = GameDataManager.getCumulativebenefitsCfgMap().get(detailId);
        if (cfg == null) {
            result.code = Code.SAMPLE_ERROR;
            log.warn("未获取到配置 playerId = {}，cfgId = {}", playerId, detailId);
            return result;
        }

        int taskType = cfg.getType();
        long target = cfg.getCondition();
        Map<Integer, Long> rewards = cfg.getRewards();
        int date = getToday();

        if (taskType == ActivityConstant.ContinuousRecharge.WELFARE_DAILY_TYPE) {
            if (!this.todayWelfareCfgMap.containsKey(cfg.getId())) {
                result.code = Code.FORBID;
                log.warn("该配置id不在今天随机配置中 playerId = {}", playerId);
                return result;
            }

            if (data.getDailyWelfareData() == null) {
                return result;
            }

            if (data.getDailyWelfareData().getDate() != date) {
                result.code = Code.PARAM_ERROR;
                log.warn("福利奖励只能领取当天的 playerId = {}，dataDate = {},date = {}", playerId, data.getDailyWelfareData().getDate(), date);
                return result;
            }

            if (data.getDailyWelfareData().getRechargeNum() == null || data.getDailyWelfareData().getRechargeNum().longValue() < target) {
                result.code = Code.PARAM_ERROR;
                log.warn("获取福利数据失败 playerId = {}", playerId);
                return result;
            }

            // 检查是否已领取（以配置ID为key检查当日是否已领取）
            if (data.getDailyWelfareData().rece(cfg.getId())) {
                result.code = Code.REPEAT_OP;
                log.warn("该奖励已经领取过 playerId = {},cfgId = {}", playerId, detailId);
                return result;
            }

            // 发放奖励
            if (CollectionUtil.isNotEmpty(rewards)) {
                CommonResult<ItemOperationResult> addResult = playerPackService.addItems(playerId, rewards, AddType.ACTIVITY_WELFARE_DAILY_REWARD);
                if (!addResult.success()) {
                    result.code = addResult.code;
                    return result;
                }
            }

            // 标记已领取
            data.getDailyWelfareData().receReward(cfg.getId());
            result.data = ItemUtils.buildItems(rewards);
            log.info("玩家成功领取七日福利的每日奖励 playerId = {}", playerId);
        } else if (taskType == ActivityConstant.ContinuousRecharge.WELFARE_MONTHLY_TYPE) {
            // 本月累计任务：检查月累计充值是否达标
            if (data.getWelfarMonthRechargeNum() == null) {
                result.code = Code.PARAM_ERROR;
                log.warn("未达到领取条件 playerId = {}，target = {}", playerId, target);
                return result;
            }
            long monthlyTotal = data.getWelfarMonthRechargeNum().longValue();
            if (monthlyTotal < target) {
                result.code = Code.PARAM_ERROR;
                log.warn("未达到领取条件2 playerId = {}，monthlyTotal = {},target = {}", playerId, monthlyTotal, target);
                return result;
            }

            // 检查是否已领取
            if (data.welfarRece(detailId)) {
                result.code = Code.REPEAT_OP;
                log.warn("玩家已领取过该活动 playerId = {}，detailId = {}", playerId, detailId);
                return result;
            }

            // 发放奖励
            if (CollectionUtil.isNotEmpty(rewards)) {
                CommonResult<ItemOperationResult> addResult = playerPackService.addItems(playerId, rewards, AddType.ACTIVITY_WELFARE_MONTHLY_REWARD);
                if (!addResult.success()) {
                    result.code = addResult.code;
                    return result;
                }
            }

            // 标记已领取
            data.welfareReward(cfg.getId());
            result.data = ItemUtils.buildItems(rewards);
            log.info("玩家成功领取七日福利的月奖励 playerId = {}", playerId);
        } else {
            result.code = Code.PARAM_ERROR;
            return result;
        }

        // 保存数据
        dataMap.put(DETAIL_ID, data);
        playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), dataMap);

        log.info("累计福利领取奖励 playerId={}, cfgId={}, taskType={}", playerId, detailId, taskType);
        return result;
    }

    /**
     * 构建连续充值信息
     *
     * @param info
     * @param data
     * @return
     */
    private ContinuousRechargeDetailInfo buildContinuousInfo(ContinuousRechargeDetailInfo info, ContinuousRechargeActivityData data) {
        if (info.phase < ActivityConstant.ContinuousRecharge.PHASE_CONTINUOUS) {
            return info;
        }

        // 重新整理配置：按天分组
        Map<Integer, List<ContinuouschargingCfg>> cfgMap = new HashMap<>();
        for (Map.Entry<Integer, ContinuouschargingCfg> en : GameDataManager.getContinuouschargingCfgMap().entrySet()) {
            ContinuouschargingCfg cfg = en.getValue();
            int dayIndex = cfg.getType() - 1;
            cfgMap.computeIfAbsent(dayIndex, k -> new ArrayList<>()).add(cfg);
        }

        int date = getToday();

        info.dailyAllRebateList = new ArrayList<>();
        info.dailyContinuousInfosList = new ArrayList<>();

        // 标记前一天是否完成
        boolean beforeDayFinish = true;
        String todayRecharge = null;

        for (int dayIndex = 0; dayIndex < ActivityConstant.ContinuousRecharge.CONTINUOUS_DAYS; dayIndex++) {
            List<ContinuouschargingCfg> cfgList = cfgMap.get(dayIndex);

            // 每日总的返利比例
            KVInfo dailyAllRebateInfo = new KVInfo();
            dailyAllRebateInfo.key = dayIndex;
            for (ContinuouschargingCfg cfg : cfgList) {
                dailyAllRebateInfo.value += cfg.getRebate();
            }
            info.dailyAllRebateList.add(dailyAllRebateInfo);

            // 连续充值中的每日充值信息
            if (beforeDayFinish) {
                DailyContinuousInfo dailyContinuousInfo = new DailyContinuousInfo();
                dailyContinuousInfo.index = dayIndex;

                // 进度信息
                dailyContinuousInfo.progressInfoList = new ArrayList<>();
                for (ContinuouschargingCfg cfg : cfgList) {
                    ContinuousProgressInfo continuousProgressInfo = new ContinuousProgressInfo();
                    continuousProgressInfo.target = cfg.getTask();
                    continuousProgressInfo.rebate = cfg.getRebate();
                    dailyContinuousInfo.progressInfoList.add(continuousProgressInfo);
                }

                DailyContinuousData dailyContinuousData = data.queryDailyContinuousByDay(dayIndex);
                if (dailyContinuousData != null) {
                    if (dailyContinuousData.getRechargeNum() != null) {
                        dailyContinuousInfo.todayValue = dailyContinuousData.getRechargeNum().toPlainString();
                    } else {
                        dailyContinuousInfo.todayValue = "0";
                    }
                    beforeDayFinish = dailyContinuousData.canNext();

                    if (date == dailyContinuousData.getDate()) {
                        todayRecharge = dailyContinuousInfo.todayValue;
                    }
                } else {
                    beforeDayFinish = false;
                }
                info.dailyContinuousInfosList.add(dailyContinuousInfo);
            }
        }

        //额外信息
        info.continuousTotalInfo = new ContinuousTotalInfo();
        info.continuousTotalInfo.totalValue = data.getContinuousTotalRecharge() == null ? "0" : data.getContinuousTotalRecharge().toPlainString();
        info.continuousTotalInfo.currentDayIndex = data.getCurrentDayIndex();
        info.continuousTotalInfo.currentRebateRate = data.getTotalRebateRate();

        BigDecimal rebateMoney = calRebateMoney(data);
        info.continuousTotalInfo.estimatedValue = rebateMoney.toPlainString();
        info.continuousTotalInfo.actualReward = calRebateGoldNum(rebateMoney);
        info.continuousTotalInfo.todayValue = todayRecharge == null ? "0" : todayRecharge;
        return info;
    }

    /**
     * 构建累计福利响应消息
     * <p>
     * 遍历 CumulativebenefitsCfg，根据 type 区分每日任务和累计任务，
     * 构建各任务的状态（NOT_CLAIM / CAN_CLAIM / CLAIMED）。
     *
     * @param data 活动数据
     * @return 累计福利信息响应
     */
    private ContinuousRechargeDetailInfo buildWelfareInfo(long playerId, ContinuousRechargeDetailInfo info, ContinuousRechargeActivityData data) {
        if (info.phase < ActivityConstant.ContinuousRecharge.PHASE_WELFARE) {
            return info;
        }

        info.welfareInfo = new WelfareInfo();

        //设置每日的信息
        info.welfareInfo.dailyTaskList = new ArrayList<>();

        for (CumulativebenefitsCfg cfg : this.todayWelfareCfgMap.values()) {
            WelfareTaskInfo welfareTaskInfo = new WelfareTaskInfo();
            welfareTaskInfo.cfgId = cfg.getId();

            welfareTaskInfo.target = cfg.getCondition();
            welfareTaskInfo.rewardItems = ItemUtils.buildItemInfo(cfg.getRewards());

            //判断是否可领取
            if (data.getDailyWelfareData() == null) {
                welfareTaskInfo.claimStatus = ActivityConstant.ClaimStatus.NOT_CLAIM;
            } else {
                if (data.getDailyWelfareData().getRechargeNum().longValue() >= welfareTaskInfo.target) {
                    welfareTaskInfo.claimStatus = data.getDailyWelfareData().rece(cfg.getId()) ? ActivityConstant.ClaimStatus.CLAIMED : ActivityConstant.ClaimStatus.CAN_CLAIM;
                } else {
                    welfareTaskInfo.claimStatus = ActivityConstant.ClaimStatus.NOT_CLAIM;
                }
            }
            info.welfareInfo.dailyTaskList.add(welfareTaskInfo);
        }

        //设置月累计信息
        info.welfareInfo.monthlyTaskList = new ArrayList<>();
        for (CumulativebenefitsCfg cfg : this.welfareMonthCfgList) {
            WelfareTaskInfo welfareTaskInfo = new WelfareTaskInfo();
            welfareTaskInfo.cfgId = cfg.getId();
            welfareTaskInfo.target = cfg.getCondition();
            welfareTaskInfo.rewardItems = ItemUtils.buildItemInfo(cfg.getRewards());

            //判断是否可领取
            if (data.getWelfarMonthRechargeNum() != null && data.getWelfarMonthRechargeNum().longValue() >= welfareTaskInfo.target) {
                welfareTaskInfo.claimStatus = data.welfarRece(cfg.getId()) ? ActivityConstant.ClaimStatus.CLAIMED : ActivityConstant.ClaimStatus.CAN_CLAIM;
            } else {
                welfareTaskInfo.claimStatus = ActivityConstant.ClaimStatus.NOT_CLAIM;
            }
            info.welfareInfo.monthlyTaskList.add(welfareTaskInfo);
        }

        info.welfareInfo.todayRecharge = data.getDailyWelfareData() == null ? "0" : data.getDailyWelfareData().getRechargeNum().toPlainString();
        info.welfareInfo.monthlyTotalRecharge = data.getWelfarMonthRechargeNum() == null ? "0" : data.getWelfarMonthRechargeNum().toPlainString();
        return info;
    }

    @Override
    public AbstractResponse getPlayerActivityDetail(Player player, ActivityData activityData, int detailId) {
        return null;
    }

    @Override
    public BaseActivityDetailInfo buildPlayerActivityDetail(Player player, ActivityData activityData, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        ContinuousRechargeActivityData continuousRechargeActivityData = (ContinuousRechargeActivityData) data;
        ContinuousRechargeDetailInfo info = new ContinuousRechargeDetailInfo();
        info.activityId = activityData.getId();
        info.detailId = DETAIL_ID;

        //是否已经参加活动
        if (continuousRechargeActivityData.getJoinTime() > 0) {
            Pair<Integer, long[]> phase = getPhase(activityData, true);
            info.phase = phase.getFirst();
            if (info.phase < ActivityConstant.ContinuousRecharge.PHASE_OVER) {
                info.startTime = phase.getSecond()[0];
                info.endTime = phase.getSecond()[1];
            }
        }
        //构建连续充值信息
        info = buildContinuousInfo(info, continuousRechargeActivityData);
        //构建累计福利消息
        info = buildWelfareInfo(player.getId(), info, continuousRechargeActivityData);
        return info;
    }

    /**
     * 重置检查
     *
     * @param playerId
     * @param activityData
     */
    private Map<Integer, PlayerActivityData> resetCheck(long playerId, ActivityData activityData, Map<Integer, PlayerActivityData> dataMap) {
        try {
            ContinuousRechargeActivityData data = (ContinuousRechargeActivityData) dataMap.get(DETAIL_ID);
            if (data == null) {
                return dataMap;
            }

            int today = getToday();
            int lastCheckDate = data.getLastCheckDate();

            // 今天已经检查过，不需要再处理
            if (lastCheckDate == today) {
                return dataMap;
            }

            Pair<Integer, long[]> phase = getPhase(activityData, true);
            if (phase.getFirst() == ActivityConstant.ContinuousRecharge.PHASE_CONTINUOUS) {
                //天数检查切换
                handleContinuousDaySwitch(playerId, data, lastCheckDate, today);
            } else if (phase.getFirst() == ActivityConstant.ContinuousRecharge.PHASE_WELFARE) {  //如果是第二阶段，则要检查第一阶段的奖励是否发放
                //补发第一阶段的奖励邮件
                sendContinuousRewardByMail(playerId, data);
                // 检查昨日未领取的每日任务奖励 → 邮件补发
                sendWelfareDailyRewardsByMail(playerId, data);
                //跨天重置福利活动的数据
                handleWelfareDaySwitch(playerId, data, lastCheckDate, today);
            } else { //如果已结束，则要检查第二阶段的奖励是否发放
                sendWelfareDailyRewardsByMail(playerId, data);
                sendWelfareMonthRewardsByMail(playerId, data);
                playerActivityDao.deletePlayerActivityData(playerId, activityData.getType(), activityData.getId());
                return Collections.emptyMap();
            }

            // 更新最后检查日期并保存
            data.setLastCheckDate(today);
            dataMap.put(DETAIL_ID, data);
            playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), dataMap);
            return dataMap;
        } catch (Exception e) {
            log.error("连续充值活动检查异常 playerId={}", playerId, e);
        }
        return null;
    }

    /**
     * 计算七日充值返利金额
     * 公式： 累计充值 * 返利比例/10000 * global#123配置的兑换比例
     *
     * @param data
     * @return
     */
    private BigDecimal calRebateMoney(ContinuousRechargeActivityData data) {
        if (data.getTotalRebateRate() < 1) {
            return BigDecimal.ZERO;
        }

        //累计比例
        BigDecimal rate = BigDecimal.valueOf(data.getTotalRebateRate()).divide(TEN_THOUSAND, 4, RoundingMode.HALF_EVEN);
        //累计充值
        BigDecimal totalRecharge = data.calculateContinuousTotalRecharge();
        //返利的价值(预算价值)
        return totalRecharge.multiply(rate).setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * 返利金额对应的金币数量
     *
     * @param money
     * @return
     */
    private long calRebateGoldNum(BigDecimal money) {
        if (money.compareTo(BigDecimal.ZERO) < 1) {
            return 0;
        }

        GlobalConfigCfg cfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.ContinuousRecharge.GOLD_EXCHANGE_RATE_CFG_ID);
        if (cfg == null || cfg.getIntValue() < 1) {
            log.warn("获取global配置失败 id = {}", ActivityConstant.ContinuousRecharge.GOLD_EXCHANGE_RATE_CFG_ID);
            return 0;
        }

        //计算出最终的返利金币
        return money.multiply(BigDecimal.valueOf(cfg.getIntValue())).longValue();
    }


    @Override
    public void initSampleCallbackCollector() {
        // 添加活动表监听
        addChangeSampleFileObserveWithCallBack(ContinuouschargingCfg.EXCEL_NAME, this::loadContinuouschargingCfg);
        addChangeSampleFileObserveWithCallBack(CumulativebenefitsCfg.EXCEL_NAME, this::loadCumulativebenefitsCfg);
    }

    /**
     * 加载连续充值的配置
     */
    private void loadContinuouschargingCfg() {
        Map<Integer, List<ContinuouschargingCfg>> tmpMap = new HashMap<>();
        for (ContinuouschargingCfg cfg : GameDataManager.getContinuouschargingCfgMap().values()) {
            int dayIndex = cfg.getType() - 1;
            tmpMap.computeIfAbsent(dayIndex, k -> new ArrayList<>()).add(cfg);
        }
        this.continuouschargingCfgMap = tmpMap;
    }

    /**
     * 加载福利活动的配置
     */
    private void loadCumulativebenefitsCfg() {
        //整理配置
        Map<Integer, List<CumulativebenefitsCfg>> tmpWelfareDailyCfgMap = new HashMap<>();
        List<CumulativebenefitsCfg> tmpWelfareMonthCfgList = new ArrayList<>();
        for (Map.Entry<Integer, CumulativebenefitsCfg> en : GameDataManager.getCumulativebenefitsCfgMap().entrySet()) {
            CumulativebenefitsCfg cfg = en.getValue();
            if (cfg.getType() == ActivityConstant.ContinuousRecharge.WELFARE_DAILY_TYPE) {
                tmpWelfareDailyCfgMap.computeIfAbsent(cfg.getGroup(), k -> new ArrayList<>()).add(cfg);
            } else if (cfg.getType() == ActivityConstant.ContinuousRecharge.WELFARE_MONTHLY_TYPE) {
                tmpWelfareMonthCfgList.add(cfg);
            }
        }

        this.welfareDailyCfgMap = tmpWelfareDailyCfgMap;
        this.welfareMonthCfgList = tmpWelfareMonthCfgList;

        genTodayWefareCfgIds();
    }

    /**
     * 生成今天的配置id
     */
    private void genTodayWefareCfgIds() {
        Map<Long, ActivityData> activityData = activityManager.getActivityData();
        long now = currentTimeMillis();
        for (Map.Entry<Long, ActivityData> en : activityData.entrySet()) {
            ActivityData data = en.getValue();
            if (data.getType() != ActivityType.CONTINUOUS_RECHARGE) {
                continue;
            }
            Pair<Integer, long[]> phase = getPhase(en.getValue(), true);
            if (phase == null || phase.getFirst() != ActivityConstant.ContinuousRecharge.PHASE_WELFARE) {
                continue;
            }

            long startTime = phase.getSecond()[0];
            int diff = TimeHelper.getDateDifference(now, startTime);

            Map<Integer, CumulativebenefitsCfg> cfgMap = cfgIds(0);
            if (CollectionUtil.isEmpty(cfgMap)) {
                cfgMap = cfgIds(0);
                this.wefareCfgIndexId = 0;
            } else {
                this.wefareCfgIndexId = diff;
            }
            this.todayWelfareCfgMap = cfgMap;
        }
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("setActivityDay".equalsIgnoreCase(gmOrders[0])) {
                String dayStr = gmOrders[1];
                long beforeTime = currentTimeMillis();
                if ("0".equals(dayStr.trim())) {
                    this.debugMills = 0;
                    genTodayWefareCfgIds();
                } else {
                    String year = dayStr.substring(0, 4);
                    String month = dayStr.substring(4, 6);
                    String day = dayStr.substring(6, 8);

                    LocalDateTime now = LocalDateTime.now();
                    now = now.withYear(Integer.parseInt(year));
                    now = now.withMonth(Integer.parseInt(month));
                    now = now.withDayOfMonth(Integer.parseInt(day));

                    this.debugMills = TimeHelper.getTimestamp(now);
                }
                long afterTime = currentTimeMillis();
                if (!TimeHelper.inSameDay(beforeTime, afterTime)) {
                    genTodayWefareCfgIds();
                }
                return res;
            } else if ("rechargeAll".equalsIgnoreCase(gmOrders[0])) {
                int activityId = Integer.parseInt(gmOrders[1]);
                ActivityData activityData = activityManager.getActivityData().get(activityId);
                if (activityData == null) {
                    log.warn("未找到该活动配置数据 activityId = {}", activityId);
                    res.code = Code.NOT_FOUND;
                    return res;
                }

                Map<Integer, PlayerActivityData> dataMap = playerActivityDao.getPlayerActivityData(playerController.playerId(), ActivityType.CONTINUOUS_RECHARGE, activityData.getId());
                if (CollectionUtil.isEmpty(dataMap)) {
                    log.warn("获取dataMap为空 playerId = {}", playerController.playerId());
                    res.code = Code.REPEAT_OP;
                    return res;
                }

                ContinuousRechargeActivityData playerActivityData = (ContinuousRechargeActivityData) dataMap.get(DETAIL_ID);
                playerActivityData.setCurrentDayIndex(6);
                Order order = new Order();
                order.setPrice(BigDecimal.valueOf(9999999));

                Pair<Integer, long[]> phase = getPhase(activityData, true);

                for (int i = 0; i < ActivityConstant.ContinuousRecharge.CONTINUOUS_DAYS; i++) {
                    handleContinuousRecharge(playerController.getPlayer(), activityData, i, playerActivityData, dataMap, order, phase);
                }
                return res;
            }

            return null;
        } catch (Exception e) {
            log.error("GM命令执行异常", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 根据时间计算当前所处的活动阶段
     *
     * @param activityData
     * @return
     */
    private Pair<Integer, long[]> getPhase(ActivityData activityData, boolean getTime) {
        long now = currentTimeMillis();
        if (now < activityData.getTimeStart()) {
            return new Pair<>(ActivityConstant.ContinuousRecharge.PHASE_NO_BEGIN, null);
        }

        //连充活动的结束时间
        long tmpTime = 7 * TimeHelper.ONE_DAY_OF_MILLIS + activityData.getTimeStart();
        //获取当前的23:59
        long continuousRechargeEndTime = TimeHelper.getEndOfDayTimestamp(tmpTime);
        if (continuousRechargeEndTime >= activityData.getTimeEnd()) {
            throw new IllegalArgumentException("连充活动时间不能小于7天 startTime = " + activityData.getTimeStart() + ", endTime = " + activityData.getTimeEnd());
        }

        if (now <= continuousRechargeEndTime) {
            if (getTime) {
                long[] arr = {activityData.getTimeStart(), continuousRechargeEndTime};
                return new Pair<>(ActivityConstant.ContinuousRecharge.PHASE_CONTINUOUS, arr);
            } else {
                return new Pair<>(ActivityConstant.ContinuousRecharge.PHASE_CONTINUOUS, null);
            }
        }

        if (now <= activityData.getTimeEnd()) {
            if (getTime) {
                continuousRechargeEndTime += TimeHelper.ONE_HOUR_OF_MILLIS;
                long startTime = TimeHelper.getDateZeroMilliTime(continuousRechargeEndTime);
                long[] arr = {startTime, activityData.getTimeEnd()};
                return new Pair<>(ActivityConstant.ContinuousRecharge.PHASE_WELFARE, arr);
            } else {
                return new Pair<>(ActivityConstant.ContinuousRecharge.PHASE_WELFARE, null);
            }
        }
        return new Pair<>(ActivityConstant.ContinuousRecharge.PHASE_OVER, null);

    }

    private Map<Integer, CumulativebenefitsCfg> cfgIds(int index) {
        if (this.welfareDailyCfgMap == null || this.welfareDailyCfgMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, CumulativebenefitsCfg> map = new HashMap<>();
        for (Map.Entry<Integer, List<CumulativebenefitsCfg>> en : this.welfareDailyCfgMap.entrySet()) {
            map.put(en.getKey(), en.getValue().get(index));
        }
        return map;
    }
}
