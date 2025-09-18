package com.jjg.game.activity.cashcow.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import com.jjg.game.activity.cashcow.dao.CashCowDao;
import com.jjg.game.activity.cashcow.data.CashCowPlayerActivityData;
import com.jjg.game.activity.cashcow.data.CashCowRecordData;
import com.jjg.game.activity.cashcow.message.bean.CashCowActivityInfo;
import com.jjg.game.activity.cashcow.message.bean.CashCowDetailInfo;
import com.jjg.game.activity.cashcow.message.bean.CashCowShowRecord;
import com.jjg.game.activity.cashcow.message.req.ReqCashCowFreeRewards;
import com.jjg.game.activity.cashcow.message.req.ReqCashCowRecord;
import com.jjg.game.activity.cashcow.message.req.ReqCashCowTotalPool;
import com.jjg.game.activity.cashcow.message.res.*;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.ActivityBuilder;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.listener.IGameClusterLeaderListener;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.CashcowCfg;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.RobotCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 摇钱树控制器
 * <p>
 * 主要职责：
 * - 管理摇钱树活动的生命周期（开始、结束、轮次切换）
 * - 处理玩家参加抽奖、领取累计奖励、获取活动详情等接口
 * - 管理机器人行为（定时触发机器人中奖、机器人向奖池注入）和定时器注册
 * - 与 DAO（cashCowDao）、玩家包（playerPackService）、玩家活动存储（playerActivityDao）、活动管理器（activityManager）交互
 * <p>
 * 注：
 * - 代码中使用了 Redis 锁（redisLock）和 ReentrantLock 做并发保护。很多关键操作依赖外部服务（玩家背包、DAO）返回值的正确性。
 * - 配置字符串（如概率、时间段）有固定格式，代码里按该格式解析，错误配置可能导致异常（已在注释处提示防护点）。
 */
@Component
public class CashCowController extends BaseActivityController implements TimerListener<String>, IGameClusterLeaderListener, ConfigExcelChangeListener {
    private final Logger log = LoggerFactory.getLogger(CashCowController.class);

    // 与数据库/存储层交互的 DAO：负责奖池、记录、玩家进度等持久化
    private final CashCowDao cashCowDao;
    // 定时器中心：用于注册/解绑周期任务（每秒轮询）
    private final TimerCenter timerCenter;
    // 保存活动 -> detailId -> 下次触发时间（毫秒）的映射
    // 结构： activityId -> (detailId -> nextTriggerMillis)
    private final Map<Long, Map<Integer, Long>> timerMap;
    // 上次机器人自动增加奖池的时间戳（毫秒）
    private long lastRobotAddTime;
    // 定时器 key（注册/移除时使用）
    private final String TIMER_KEY = "cashCow";
    // 当前注册的 TimerEvent（用于取消定时器）
    private volatile TimerEvent<String> timerEvent = null;
    // 局部锁，用于双重检查锁等场景（注意：使用后必须正确 unlock）
    private final ReentrantLock reentrantLock = new ReentrantLock();

    public CashCowController(CashCowDao cashCowDao, TimerCenter timerCenter) {
        this.cashCowDao = cashCowDao;
        this.timerCenter = timerCenter;
        // 使用并发 Map 以保证在并发环境下对 timerMap 的安全访问
        timerMap = new ConcurrentHashMap<>();
    }


    @Override
    public void activityLoadCompleted(ActivityData activityData) {
        // 活动配置加载完成时的回调
        long activityId = activityData.getId();
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        if (CollectionUtil.isEmpty(baseCfgBeanMap)) {
            return;
        }
        // 只有执行节点（leader）负责注册全局性的定时器与机器人任务
        if (activityManager.isExecutionNode()) {
            addTimerEvent(); // 确保定时器已注册
            LocalDateTime now = LocalDateTime.now();
            for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                if (cfgBean instanceof CashcowCfg cfg) {
                    // 为每个非累计领奖类型的 detail 添加机器人触发时间（基于当前时间判断哪个时间段匹配）
                    addRobotTimer(cfg, now, activityId, false);
                }
            }
        }
    }

    /**
     * 添加轮询定时器（按秒轮询）
     * <p>
     * 说明：
     * - 该方法使用了双重检查的写法防止重复注册 timerEvent。
     */
    public void addTimerEvent() {
        if (timerEvent == null) {
            reentrantLock.lock();
            try {
                if (timerEvent == null) {
                    // 添加一个 1 秒周期的定时事件，回调 this.onTimer(...)
                    timerEvent = new TimerEvent<>(this, TIMER_KEY, TimeHelper.ONE_SECOND_OF_MILLIS);
                    timerCenter.add(timerEvent);
                }
            } finally {
                reentrantLock.unlock();
            }
        }
    }

    @Override
    public void addActivityProgress(ActivityData activityData, long progress, Object additionalParameters) {
        if (notAddProgress(additionalParameters)) return;

        // 从全局配置读取“加入奖池的比例”（单位：万分比）
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.CashCow.CASH_COW_ADD_POOL_PROPORTION);
        // 计算真实增加的数量：progress * config / 10000，向下取整
        long realProgress = BigDecimal.valueOf(progress)
                .multiply(BigDecimal.valueOf(globalConfigCfg.getIntValue()))
                .divide(BigDecimal.valueOf(10000), RoundingMode.FLOOR)
                .longValue();

        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        // 对活动中所有非累计领奖（type != ActivityConstant.CashCow.CUMULATIVE_REWARDS_REWARD_TYPE）的奖池进行增加
        for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
            if (cfgBean instanceof CashcowCfg cfg && cfg.getType() != ActivityConstant.CashCow.CUMULATIVE_REWARDS_REWARD_TYPE) {
                cashCowDao.addActivityPool(activityData.getId(), cfgBean.getId(), realProgress);
            }
        }
    }

    // 辅助判断：只有当额外参数是金币道具时才累进到奖池，否则忽略
    private static boolean notAddProgress(Object additionalParameters) {
        return additionalParameters instanceof Integer itemId && !itemId.equals(ItemUtils.getGoldItemId());
    }

    @Override
    public boolean addPlayerProgress(long playerId, ActivityData data, long progress, Object additionalParameters) {
        // 当玩家发生某些行为导致个人进度增加时调用（例如玩家获得金币）
        if (notAddProgress(additionalParameters)) {
            return false;
        }
        // 将玩家的个人进度累加到玩家活动表（返回实际新增进度值或其他业务含义，具体实现见 DAO）
        long added = cashCowDao.addPlayerActivityProgress(playerId, data.getId(), progress);

        // 对玩家个人活动数据加分布式锁，确保并发安全
        String lockKey = playerActivityDao.getLockKey(playerId, data.getId());
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(data.getId());
        boolean canClaim = false;
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            // 获取玩家在该活动下的所有 detail 的数据（map: detailId -> CashCowPlayerActivityData）
            Map<Integer, CashCowPlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, data.getType(), data.getId());
            for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                if (cfgBean instanceof CashcowCfg cfg && cfg.getType() == ActivityConstant.CashCow.CUMULATIVE_REWARDS_REWARD_TYPE) {
                    // CUMULATIVE_REWARDS_REWARD_TYPE 表示“累计奖励”类型（需要累计进度才能领取）
                    CashCowPlayerActivityData activityData = playerActivityData.computeIfAbsent(cfg.getId(), key -> new CashCowPlayerActivityData(data.getId(), data.getRound()));
                    // 如果已领取则跳过
                    if (activityData.getClaimStatus() == ActivityConstant.ClaimStatus.CLAIMED) {
                        continue;
                    }
                    // 如果累计进度达到配置条件，则标记为可领取
                    if (added >= cfg.getCondition()) {
                        activityData.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
                        canClaim = true;
                    }
                }
            }
            // 持久化玩家活动数据
            playerActivityDao.savePlayerActivityData(playerId, data.getType(), data.getId(), playerActivityData);
        } catch (Exception e) {
            log.error("摇钱树增加玩家个人进度失败 playerId:{} addVelue:{}", playerId, progress);
        } finally {
            redisLock.unlock(lockKey);
        }
        return canClaim;
    }

    @Override
    public AbstractResponse joinActivity(Player oldPlayer, ActivityData activityData, int detailId, int times) {
        // 玩家参加摇钱树抽奖接口
        ResCashCowJoin res = new ResCashCowJoin(Code.SUCCESS);
        long playerId = oldPlayer.getId();
        long activityId = activityData.getId();
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(detailId);
        if (baseCfgBean instanceof CashcowCfg cfg) {
            // 累计领奖类型禁止通过 “join” 接口参与抽奖
            if (cfg.getType() == ActivityConstant.CashCow.CUMULATIVE_REWARDS_REWARD_TYPE) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            try {
                // 获取配置的中奖权重/概率列表（二维列表），以及单次分配的奖池数（distribution）
                List<List<Integer>> weight = cfg.getWeight();
                if (CollectionUtil.isEmpty(weight) || cfg.getDistribution() <= 0) {
                    // 配置异常
                    res.code = Code.SAMPLE_ERROR;
                    return res;
                }
                // 校验配表每一项的长度合法性（至少3个元素）
                for (List<Integer> list : weight) {
                    if (list.size() < 3) {
                        res.code = Code.SAMPLE_ERROR;
                        return res;
                    }
                }
                Player player = corePlayerService.get(playerId);
                CommonResult<ItemOperationResult> addedItem = null;
                CommonResult<ItemOperationResult> removed = null;
                long get = 0;
                // 对玩家参加活动的关键流程上分布式锁，保证玩家活动数据原子性
                String lockKey = playerActivityDao.getLockKey(playerId, activityId);
                redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
                try {
                    // 读取玩家在该活动的 detail 记录（包括 joinTimes 等）
                    Map<Integer, CashCowPlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
                    CashCowPlayerActivityData data = playerActivityData.computeIfAbsent(detailId, key -> new CashCowPlayerActivityData(activityId, activityData.getRound())
                    );
                    // 扣除消耗道具
                    removed = playerPackService.removeItems(player, cfg.getNeedItem(), "cashcow");
                    if (!removed.success()) {
                        // 扣除失败，返回对应错误码（可能是道具不足等）
                        res.code = removed.code;
                        return res;
                    }
                    int joinTimes = data.getJoinTimes();
                    // 根据权重配置判断是否触发中奖
                    for (List<Integer> list : weight) {
                        // 这里约定 list 的结构： [minJoinTimes, maxJoinTimes, probability?]
                        // 代码中只使用了 list.getFirst(), list.get(1), list.getLast() 三项：
                        //  - list.getFirst() 和 list.get(1) 用作 joinTimes 的范围
                        //  - list.getLast() 被当作万分比概率（0..9999）
                        if (list.getFirst() <= joinTimes && joinTimes < list.get(1)) {
                            // 按万分比概率判断是否中奖
                            Integer probability = list.getLast();
                            if (RandomUtil.randomInt(10000) < probability) {
                                // 从奖池中扣除 cfg.getDistribution() 并返回实际发奖数量（DAO 负责判空/原子减）
                                get = cashCowDao.reduceActivityPool(activityId, detailId, cfg.getDistribution());
                                if (get > 0) {
                                    // 给玩家发放金币（或其他道具，目前为金币）
                                    addedItem = playerPackService.addItem(playerId, ItemUtils.getGoldItemId(), get, "CashCowJoin");
                                    // 记录玩家中奖记录（写到玩家记录表或排行榜）
                                    CashCowRecordData cashCowRecordData = new CashCowRecordData(activityData.getRound(), System.currentTimeMillis(), player.getNickName(), cfg.getType(), get);
                                    cashCowDao.savePlayerRecordActivity(playerId, activityId, cashCowRecordData);
                                }
                            }
                            // 匹配到范围后跳出循环（每次 join 只匹配一个范围）
                            break;
                        }
                    }
                    // 增加玩家的参与次数
                    data.setJoinTimes(data.getJoinTimes() + 1);
                    // 持久化玩家 detail 数据
                    playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityId, playerActivityData);
                } catch (Exception e) {
                    // 捕获到异常后记录并设置通用异常码
                    log.error("玩家参加摇钱树加锁后出现异常 playerId:{} activityId:{} detailId:{}", playerId, activityId, detailId, e);
                    res.code = Code.EXCEPTION;
                } finally {
                    // 解锁（注意：确保 redisLock.unlock 在任何情况下都会被调用）
                    redisLock.unlock(lockKey);
                }
                // 业务日志：记录玩家参加并扣除/发放的明细（异步/日志落库）
                activityLogger.sendCashCowJoinLog(player, activityData, detailId
                        , cfg.getType(), cfg.getNeedItem(), removed.data, get, addedItem == null ? null : addedItem.data);
                // 构建返回数据
                res.activityId = activityId;
                res.detailId = detailId;
                res.num = get;
                // 获取当前活动总池（所有 detail 汇总或 DAO 实现的含义）
                res.pool = cashCowDao.getActivityPool(activityId);
                res.totalPool = cashCowDao.getActivityPool(activityId);
            } catch (Exception e) {
                // 捕获外层异常，避免接口抛出未捕获异常
                log.error("玩家参加摇钱树  出现异常 playerId:{} activityId:{} detailId:{}", playerId, activityId, detailId, e);
            }
        } else {
            // 找不到配置，记录错误
            log.error("玩家参加摇钱树 活动配置为空playerId:{} activityId:{} detailId:{}", playerId, activityId, detailId);
        }
        return res;
    }

    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        // 玩家领取累计奖励接口（type == ActivityConstant.CashCow.CUMULATIVE_REWARDS_REWARD_TYPE）
        ResCashCowClaimRewards res = new ResCashCowClaimRewards(Code.SUCCESS);
        long activityId = activityData.getId();
        long playerId = player.getId();
        String lockKey = playerActivityDao.getLockKey(playerId, activityId);
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(detailId);
        if (baseCfgBean instanceof CashcowCfg cfg) {
            // 非累计领奖类型禁止走此接口
            if (cfg.getType() != ActivityConstant.CashCow.CUMULATIVE_REWARDS_REWARD_TYPE) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            // 检查玩家的累计进度是否已达到领奖要求
            long activityProgress = cashCowDao.getPlayerActivityProgress(playerId, activityId);
            if (activityProgress < cfg.getCondition()) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            CashCowPlayerActivityData data = null;
            CommonResult<ItemOperationResult> addedItems = null;
            boolean send = false;
            redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
            try {
                // 获取玩家当前 detail 数据
                Map<Integer, CashCowPlayerActivityData> dataMap = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
                if (CollectionUtil.isEmpty(dataMap)) {
                    res.code = Code.PARAM_ERROR;
                    return res;
                }
                data = dataMap.computeIfAbsent(detailId, key -> new CashCowPlayerActivityData(activityId, activityData.getRound()));
                // 如果已领取过则返回重复操作
                if (data.getClaimStatus() == ActivityConstant.ClaimStatus.CLAIMED) {
                    res.code = Code.REPEAT_OP;
                    return res;
                }
                // 发放配置的奖励（addItems 负责实际发放到玩家包）
                addedItems = playerPackService.addItems(playerId, cfg.getRewards(), "CashCowRewords");
                if (!addedItems.success()) {
                    // 发放失败（例如背包已满/服务器错误）
                    res.code = Code.UNKNOWN_ERROR;
                    return res;
                }
                // 标记为已领取并持久化
                data.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED);
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityId, dataMap);
                send = true;
            } catch (Exception e) {
                log.error("领取摇钱树累计奖励异常 playerId:{} activityId:{}", playerId, activityId, e);
            } finally {
                redisLock.unlock(lockKey);
            }
            if (send) {
                // 记录日志并构建返回值
                if (addedItems.success()) {
                    activityLogger.sendCashCowRewards(player, activityData, detailId, addedItems.data, activityProgress, cfg.getRewards());
                }
                res.activityId = activityId;
                res.detailId = detailId;
                res.infoList = ItemUtils.buildItemInfo(cfg.getRewards());
                BaseActivityDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(activityId, cfg, data);
                if (baseActivityDetailInfo instanceof CashCowDetailInfo info) {
                    res.detailInfo = info;
                }
            }
        }
        // 返回响应
        return res;
    }

    @Override
    public void onActivityEnd(ActivityData activityData) {
        // 活动结束：将状态置为已结束，并清理内存中的 timerMap（定时器可能需要在 leader 角色退出时同步移除）
        activityData.setStatus(ActivityConstant.ActivityStatus.ENDED);
        timerMap.clear();
    }

    @Override
    public void onActivityStart(ActivityData activityData) {
        long activityId = activityData.getId();
        if (activityData.getStatus() == ActivityConstant.ActivityStatus.RUNNING) {
            log.error("摇钱树开始失败 活动正在进行中 activityId:{} ", activityId);
            return;
        }
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        // 初始化摇钱树活动（首次开活动 round==0）
        if (activityData.getRound() == 0) {
            activityData.setRound(activityData.getId());
            if (CollectionUtil.isNotEmpty(baseCfgBeanMap)) {
                // 按配置把每个非累计领奖类型的 detail 初始化奖池为配置的 initialprizepool
                for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                    if (cfgBean instanceof CashcowCfg cfg && cfg.getType() != ActivityConstant.CashCow.CUMULATIVE_REWARDS_REWARD_TYPE) {
                        cashCowDao.setActivityPool(activityId, cfg.getId(), cfg.getInitialprizepool());
                    }
                }
            } else {
                log.error("摇钱树初始化奖池失败 未找到配置");
            }
        } else {
            // 非首次开启，进入下一轮：将上一轮奖池按配置比例作为下一轮底池
            activityData.addRound();
            GlobalConfigCfg configCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.CashCow.CASH_COW_ADD_NEXT_ROUND_PROPORTION);
            for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                if (cfgBean instanceof CashcowCfg cfg) {
                    long pool = cashCowDao.getActivityPool(activityId, cfg.getId());
                    if (pool == 0 && cfg.getType() != ActivityConstant.CashCow.CUMULATIVE_REWARDS_REWARD_TYPE) {
                        // 如果上一轮奖池为 0，则初始化为配置的初始奖池
                        cashCowDao.addActivityPool(activityId, cfg.getId(), cfg.getInitialprizepool());
                    } else {
                        // 否则按配置比例计算下一轮底池（万分比）
                        long nextPoll = BigDecimal.valueOf(pool)
                                .multiply(BigDecimal.valueOf(configCfg.getIntValue()))
                                .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN)
                                .longValue();
                        cashCowDao.setActivityPool(activityId, cfg.getId(), nextPoll);

                    }
                }
            }
            // 清除在线玩家的进度，防止上一轮数据影响新一轮
            List<Long> onlinePlayerIds = activityManager.getOnlinePlayerIds();
            for (Long onlinePlayerId : onlinePlayerIds) {
                cashCowDao.delPlayerActivityProgress(onlinePlayerId, activityId);
                playerActivityDao.deletePlayerActivityData(onlinePlayerId, activityData.getType(), activityId);
            }
            // 修改活动状态为运行中
            activityData.setStatus(ActivityConstant.ActivityStatus.RUNNING);
        }
    }

    /**
     * 根据配置为指定 detail 添加机器人触发时间（将下次触发时间写入 timerMap）
     *
     * @param cfg           detail 的配置（包含 winningFrequency / distribution 等）
     * @param now           当前时间（用于判断当前小时段）
     * @param activityId    活动 id
     * @param targetRewards 如果为 true 则表示触发一次“机器人中奖”（即在当前时刻有可能执行一次减池并记录中奖）
     *                      <p>
     *                      配置说明（winningFrequency）：
     *                      每一项为一个整数列表，格式由配置约定。代码中使用了：
     *                      list.get(0) == hourStart（包含）， list.get(1) == hourEnd（不包含）,
     *                      list.get(2) == minIntervalSeconds, list.get(3) == maxIntervalSeconds, list.get(4) == probability(万分比)
     *                      <p>
     *                      注意：如果配置格式变更，需同步修改解析逻辑。
     */
    private void addRobotTimer(CashcowCfg cfg, LocalDateTime now, long activityId, boolean targetRewards) {
        List<List<Integer>> winningFrequency = cfg.getWinningFrequency();
        // 如果没有配置频率或是累计领奖类型（type == ActivityConstant.CashCow.CUMULATIVE_REWARDS_REWARD_TYPE），无需为机器人添加定时任务
        if (CollectionUtil.isEmpty(winningFrequency) || cfg.getType() == ActivityConstant.CashCow.CUMULATIVE_REWARDS_REWARD_TYPE) {
            return;
        }
        for (List<Integer> probabilityList : winningFrequency) {
            // 保证每一项长度至少包含 hourStart/hourEnd/minInterval/maxInterval/probability
            if (probabilityList.size() < 5 || !(probabilityList.get(0) <= now.getHour() && now.getHour() < probabilityList.get(1))) {
                continue;
            }
            // 如果是 targetRewards（表示此时需要尝试触发机器人中奖）
            if (targetRewards) {
                // 以万分比概率判断是否触发机器人中奖
                if (probabilityList.get(4) > RandomUtil.randomInt(10000)) {
                    // 随机选一个机器人（机器人配置由 GameDataManager 提供）
                    RobotCfg robotCfg = RandomUtil.randomEle(GameDataManager.getRobotCfgList());
                    // 从奖池中扣除分配额度（DAO 负责原子检查与扣减）
                    long get = cashCowDao.reduceActivityPool(activityId, cfg.getId(), cfg.getDistribution());
                    if (get > 0) {
                        // 记录机器人中奖（不发包给“机器人账户”，只是写记录）
                        CashCowRecordData cashCowRecordData = new CashCowRecordData(robotCfg.getId(), System.currentTimeMillis(), robotCfg.getName(), cfg.getType(), get);
                        cashCowDao.savePlayerRecordActivity(robotCfg.getId(), activityId, cashCowRecordData);
                    }
                }
            }
            // 根据配置的 min/max 随机生成下一次触发间隔（单位：秒），并存储为下次触发时间戳（毫秒）
            long nextTime = RandomUtil.randomInt(probabilityList.get(2), probabilityList.get(3));
            // 仅记录日志和在 timerMap 中保存 next trigger
            log.debug("摇钱树添加机器人获奖成功 activity:{} detailId:{} time:{}", activityId, cfg.getId(), nextTime);
            // 保存到 timerMap：activityId -> (detailId -> nextTriggerMillis)
            Map<Integer, Long> paramMap = timerMap.computeIfAbsent(activityId, key -> new HashMap<>());
            paramMap.put(cfg.getId(), System.currentTimeMillis() + nextTime * TimeHelper.ONE_SECOND_OF_MILLIS);
        }
    }

    @Override
    public int updateActivity(String jsonData) {
        // 配置变更/热更回调（当前实现为 no-op）
        return 0;
    }

    @Override
    public AbstractResponse getPlayerActivityDetail(long playerId, ActivityData data, int detailId) {
        // 查询并返回玩家对于某个 detail 的活动明细信息（界面显示）
        long activityId = data.getId();
        ResCashCowDetailInfo detailInfo = new ResCashCowDetailInfo(Code.SUCCESS);
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityId);
        Map<Integer, CashCowPlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, data.getType(), activityId);
        detailInfo.detailInfo = new ArrayList<>();
        BaseCfgBean baseCfgBean = baseCfgBeanMap.get(detailId);
        if (baseCfgBean instanceof CashcowCfg cfg) {
            CashCowDetailInfo cardDetailInfo = buildPlayerActivityDetail(activityId, cfg, playerActivityData.get(detailId));
            detailInfo.detailInfo.add(cardDetailInfo);
        }
        return detailInfo;
    }

    @Override
    public CashCowDetailInfo buildPlayerActivityDetail(long activityId, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        // 将配置与玩家数据组合成客户端所需的 detailInfo DTO
        if (baseCfgBean instanceof CashcowCfg cfg) {
            CashCowDetailInfo info = new CashCowDetailInfo();
            info.activityId = activityId;
            info.detailId = baseCfgBean.getId();
            info.type = cfg.getType();
            // 如果是累计奖励类型，返回所需进度、奖励项和领取状态
            if (cfg.getType() == ActivityConstant.CashCow.CUMULATIVE_REWARDS_REWARD_TYPE) {
                info.rewardItems = ItemUtils.buildItemInfo(cfg.getRewards());
                if (data != null) {
                    info.claimStatus = data.getClaimStatus();
                }
                info.needProgress = cfg.getCondition();
            } else {
                // 抽奖类型：返回消耗道具信息和当前奖池
                info.costItems = ItemUtils.buildItemInfo(cfg.getNeedItem());
                info.pool = cashCowDao.getActivityPool(activityId, baseCfgBean.getId());
            }
            return info;
        }
        return null;
    }

    /**
     * 获取配置中的“免费次数”道具（全局配置）
     * <p>
     * 返回值：
     * - 配置形式为 "itemId_count"（下划线分隔），例如 "1001_1"
     * - 如果格式正确返回 Item 对象，否则返回 null
     */
    private Item getConfigFreeRewards() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.CashCow.CASH_COW_FREE_ITEM);
        String[] itemInfo = StringUtils.split(globalConfigCfg.getValue(), "_");
        if (itemInfo.length == 2) {
            return new Item(Integer.parseInt(itemInfo[0]), Long.parseLong(itemInfo[1]));
        }
        return null;
    }

    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(long playerId, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        // 构建摇钱树，返回给客户端（包括每个活动的 detail 列表、当前进度、免费领取状态等）
        ResCashCowTypeInfo cashCowTypeInfo = new ResCashCowTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cashCowTypeInfo;
        }
        cashCowTypeInfo.activityData = new ArrayList<>();
        for (Map.Entry<Long, List<BaseActivityDetailInfo>> entry : allDetailInfo.entrySet()) {
            CashCowActivityInfo cashCowActiviTyInfo = new CashCowActivityInfo();
            cashCowActiviTyInfo.detailInfos = new ArrayList<>();
            cashCowTypeInfo.activityData.add(cashCowActiviTyInfo);
            for (BaseActivityDetailInfo baseActivityDetailInfo : entry.getValue()) {
                if (baseActivityDetailInfo instanceof CashCowDetailInfo info) {
                    cashCowActiviTyInfo.detailInfos.add(info);
                }
            }
            Long activityId = entry.getKey();
            // 当前玩家在该活动的累计进度
            cashCowActiviTyInfo.currentProgress = cashCowDao.getPlayerActivityProgress(playerId, activityId);
            ActivityData data = activityManager.getActivityData().get(activityId);
            cashCowActiviTyInfo.endTime = data.getTimeEnd();
            cashCowActiviTyInfo.round = data.getRound();
            // 返回距离本地周期重置的剩余秒数（通常指次日凌晨）
            cashCowActiviTyInfo.resetRemainTime = TimeHelper.getNextDayRemainTime();
            // 免费道具信息
            Item freeRewards = getConfigFreeRewards();
            if (freeRewards != null) {
                cashCowActiviTyInfo.freeItemInfo = ItemUtils.buildItemInfo(freeRewards.getId(), freeRewards.getItemCount());
            }
            // 玩家是否已领取本次免费道具
            cashCowActiviTyInfo.freeStatus = cashCowDao.getFreeRewardsStatus(playerId, activityId);
        }
        return cashCowTypeInfo;
    }


    @Override
    public ActivityInfo buildActivityInfo(long playerId, ActivityData activityData) {
        // 生成 ActivityInfo（用于活动红点/可领取提示）：只要有一项可以领取则标记 claimStatus 为可领取
        long activityProgress = cashCowDao.getPlayerActivityProgress(playerId, activityData.getId());
        Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
        int claimStatus = 0;
        Map<Integer, CashCowPlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
        if (CollectionUtil.isNotEmpty(baseCfgBeanMap)) {
            // 优先判断免费领取状态
            if (cashCowDao.getFreeRewardsStatus(playerId, activityData.getId())) {
                claimStatus = ActivityConstant.ClaimStatus.CAN_CLAIM;
                return ActivityBuilder.buildActivityInfo(activityData, claimStatus);
            }
            // 检查每个累计领奖 detail 是否满足可领取（进度达到 && data 标记为 CAN_CLAIM）
            for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                if (cfgBean instanceof CashcowCfg cfg) {
                    CashCowPlayerActivityData data = playerActivityData.get(cfg.getId());
                    if (cfg.getType() == ActivityConstant.CashCow.CUMULATIVE_REWARDS_REWARD_TYPE && activityProgress >= cfg.getCondition()) {
                        if (data != null && data.getClaimStatus() == ActivityConstant.ClaimStatus.CAN_CLAIM) {
                            claimStatus = ActivityConstant.ClaimStatus.CAN_CLAIM;
                            break;
                        }
                    }
                }
            }
        }
        return ActivityBuilder.buildActivityInfo(activityData, claimStatus);
    }


    @Override
    public List<BaseCfgBean> getDetailCfgBean() {
        // 返回所有摇钱树的 detail 配置（用于活动系统注册）
        return new ArrayList<>(GameDataManager.getCashcowCfgList());
    }

    @Override
    public Class<CashcowCfg> getDetailDataClass() {
        return CashcowCfg.class;
    }


    public AbstractResponse reqCashCowRecord(PlayerController playerController, ReqCashCowRecord req) {
        // 查询玩家或全局中奖记录（分页）
        ResCashCowRecord res = new ResCashCowRecord(Code.SUCCESS);
        res.activityId = req.activityId;
        res.type = req.type;
        Pair<List<CashCowRecordData>, Boolean> playerRecordActivities = null;
        // type == 1：个人记录，type == 2：全局记录
        if (req.type == 1) {
            playerRecordActivities = cashCowDao.getPlayerRecordActivities(playerController.playerId(), req.activityId,
                    req.startIndex, req.startIndex + Math.min(req.size, ActivityConstant.CashCow.DEFAULT_SIZE));
        } else if (req.type == 2) {
            playerRecordActivities = cashCowDao.getAllRecordActivities(req.activityId, req.startIndex, req.startIndex +
                    Math.min(req.size, ActivityConstant.CashCow.DEFAULT_SIZE));
        }
        if (playerRecordActivities != null && CollectionUtil.isNotEmpty(playerRecordActivities.getFirst())) {
            res.recordList = new ArrayList<>();
            for (CashCowRecordData playerRecordActivity : playerRecordActivities.getFirst()) {
                CashCowShowRecord cashCowShowRecord = new CashCowShowRecord();
                cashCowShowRecord.recordTime = playerRecordActivity.getRecordTime();
                cashCowShowRecord.type = playerRecordActivity.getType();
                cashCowShowRecord.num = playerRecordActivity.getNum();
                cashCowShowRecord.round = playerRecordActivity.getRound();
                cashCowShowRecord.name = playerRecordActivity.getName();
                res.recordList.add(cashCowShowRecord);
            }
            // 是否还有下一页（由 DAO 返回的布尔值）
            res.hasNext = playerRecordActivities.getSecond();
            res.startIndex = req.startIndex;
        }
        return res;
    }

    /**
     * 请求摇钱树总池子（所有 detail 的总和）
     */
    public AbstractResponse reqCashCowTotalPool(PlayerController playerController, ReqCashCowTotalPool req) {
        ResCashCowTotalPool res = new ResCashCowTotalPool(Code.SUCCESS);
        res.activityId = req.activityId;
        res.totalNum = cashCowDao.getActivityPool(req.activityId);
        return res;
    }

    @Override
    public void onTimer(TimerEvent<String> timerEvent) {
        // 定时器回调（每秒触发一次）：
        //  - 检查并执行 timerMap 中到期的机器人触发（addRobotTimer(targetRewards = true)）
        //  - 如果数据已加载完毕，按全局配置触发机器人自动增加奖池
        try {
            if (activityManager.isExecutionNode()) {
                long timeMillis = System.currentTimeMillis();
                // 遍历每个活动的定时任务 map，检查是否有到期项
                for (Map.Entry<Long, Map<Integer, Long>> entry : timerMap.entrySet()) {
                    // 取得该活动的 detail 配置表
                    Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(entry.getKey());
                    if (CollectionUtil.isEmpty(baseCfgBeanMap)) {
                        continue;
                    }
                    for (Map.Entry<Integer, Long> longEntry : entry.getValue().entrySet()) {
                        // 如果当前时间到达或超过触发时间，则调用 addRobotTimer 进行“可能的机器人中奖触发”及重新排期
                        if (timeMillis >= longEntry.getValue()) {
                            BaseCfgBean baseCfgBean = baseCfgBeanMap.get(longEntry.getKey());
                            if (baseCfgBean instanceof CashcowCfg cfg) {
                                addRobotTimer(cfg, LocalDateTime.now(), entry.getKey(), true);
                            }
                        }
                    }
                }
                // 当所有数据加载完成后，判断是否触发机器人自动“增加奖池”行为（按全局频率配置）
                if (GameDataManager.getInstance().isLoadAllFinished()) {
                    GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.CashCow.CASH_COW_ROBOT_ADD_FREQUENCY);
                    if (globalConfigCfg != null && StringUtils.isNotEmpty(globalConfigCfg.getValue())) {
                        // globalConfigCfg.getValue() 格式为 "intervalSeconds_probability" （代码按下划线分割）
                        String[] cfg = StringUtils.split(globalConfigCfg.getValue(), "_");
                        if (cfg.length == 2) {
                            // 判断是否满足触发时间（上次触发 + 配置间隔 < now）
                            if (lastRobotAddTime == 0 || lastRobotAddTime + Long.parseLong(cfg[0]) * TimeHelper.ONE_SECOND_OF_MILLIS < timeMillis) {
                                lastRobotAddTime = timeMillis;
                                // 触发中奖
                                if (Integer.parseInt(cfg[1]) > RandomUtil.randomInt(10000)) {
                                    // 触发自动增加：根据另一个配置 CASH_COW_ROBOT_ADD_VALUE 获取按小时段的增加区间
                                    GlobalConfigCfg addCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.CashCow.CASH_COW_ROBOT_ADD_VALUE);
                                    List<List<Integer>> cfgAdd = getCfgAdd(addCfg.getValue());
                                    if (CollectionUtil.isEmpty(cfgAdd)) {
                                        return;
                                    }
                                    int hour = LocalDateTime.now().getHour();
                                    for (List<Integer> list : cfgAdd) {
                                        // 这里 list 的格式为 [hourStart, hourEnd, minAdd, maxAdd]
                                        if (list.getFirst() >= hour && hour < list.get(1)) {
                                            int addValue = RandomUtil.randomInt(list.get(2), list.get(3));
                                            // 将 addValue 增加到所有该类型活动的每个非累计 detail 的奖池中
                                            Map<Long, ActivityData> activityDataMap = activityManager.getActivityTypeData().get(ActivityType.CASH_COW);
                                            for (ActivityData activityData : activityDataMap.values()) {
                                                Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
                                                for (BaseCfgBean baseCfgBean : baseCfgBeanMap.values()) {
                                                    if (baseCfgBean instanceof CashcowCfg cashcowCfg) {
                                                        if (cashcowCfg.getType() != ActivityConstant.CashCow.CUMULATIVE_REWARDS_REWARD_TYPE) {
                                                            cashCowDao.addActivityPool(activityData.getId(), baseCfgBean.getId(), addValue);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("摇钱树 定时任务执行失败", e);
        }
    }

    /**
     * 解析机器人自动增加奖池配置（形如 "h1_h2_x_y|h3_h4_x_y|..."）
     * <p>
     * 返回值：List of List<Integer>，每个小列表表示一段：
     * [hourStart, hourEnd, minAdd, maxAdd]
     * <p>
     */
    public List<List<Integer>> getCfgAdd(String cfg) {
        List<List<Integer>> arrayList = new ArrayList<>();
        if (StringUtils.isNotEmpty(cfg)) {
            try {
                String[] split = StringUtils.split(cfg, "|");
                for (String string : split) {
                    List<Integer> list = new ArrayList<>();
                    arrayList.add(list);
                    String[] split2 = StringUtils.split(string, "_");
                    for (String s : split2) {
                        list.add(Integer.parseInt(s));
                    }
                }
            } catch (NumberFormatException e) {
                log.error("摇钱树解析配置失败 cfg:{}", cfg, e);
            }
        }
        return arrayList;
    }

    @Override
    public void isLeader() {
        // 当当前节点成为活动执行节点（主节点）时调用：
        //  - 注册定时器（如果尚未注册）
        //  - 遍历所有该类型的活动，根据配置为每个 detail 添加机器人下一次触发时间（不立即触发）
        if (activityManager.isExecutionNode()) {
            addTimerEvent();
            // 获取所有 CASH_COW 类型的活动
            Map<Long, ActivityData> activityDataMap = activityManager.getActivityTypeData().get(ActivityType.CASH_COW);
            if (CollectionUtil.isEmpty(activityDataMap)) {
                return;
            }
            for (ActivityData activityData : activityDataMap.values()) {
                // 仅处理可以运行的活动
                if (!activityData.canRun()) {
                    Map<Integer, BaseCfgBean> baseCfgBeanMap = activityManager.getActivityDetailInfo().get(activityData.getId());
                    for (BaseCfgBean cfgBean : baseCfgBeanMap.values()) {
                        if (cfgBean instanceof CashcowCfg cfg) {
                            addRobotTimer(cfg, LocalDateTime.now(), activityData.getId(), false);
                        }
                    }
                }
            }
            log.debug("选举为主节点 摇钱树机器人添加");
        }
    }

    @Override
    public void notLeader() {
        // 当前节点不再是主节点：移除定时器并清理本地 timer 状态
        timerCenter.remove(this, TIMER_KEY);
        timerEvent = null;
        timerMap.clear();
        log.debug("退举 摇钱树机器人删除");
    }

    public AbstractResponse reqCashCowFreeRewards(PlayerController playerController, ActivityData data, ReqCashCowFreeRewards req) {
        // 玩家领取免费道具接口（有冷却/每日领取限制）
        ResCashCowFreeRewards res = new ResCashCowFreeRewards(Code.SUCCESS);
        Item freeRewards = getConfigFreeRewards();
        if (freeRewards == null) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        // 检查玩家是否已领过免费奖励
        boolean freeRewardsStatus = cashCowDao.getFreeRewardsStatus(playerController.playerId(), req.activityId);
        if (freeRewardsStatus) {
            res.code = Code.REPEAT_OP;
            return res;
        }
        CommonResult<ItemOperationResult> addItems = null;
        // 使用玩家免费锁，防止并发重复领取
        String playerFreeLockKey = cashCowDao.getPlayerFreeLockKey(playerController.playerId(), req.activityId);
        redisLock.lock(playerFreeLockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            // 再次校验（双重校验）
            freeRewardsStatus = cashCowDao.getFreeRewardsStatus(playerController.playerId(), req.activityId);
            if (freeRewardsStatus) {
                res.code = Code.REPEAT_OP;
                return res;
            }
            // 发放道具到玩家背包
            addItems = playerPackService.addItem(playerController.playerId(), freeRewards.getId(), freeRewards.getItemCount(), "CashCowFreeRewards");
            if (!addItems.success()) {
                res.code = Code.UNKNOWN_ERROR;
                return res;
            }
            // 标记玩家已领取
            cashCowDao.addFreeRewardsCount(playerController.playerId(), req.activityId);
        } catch (Exception e) {
            log.error("摇钱树请求领取免费道具失败 playerId:{} activityId:{}", playerController.playerId(), req.activityId);
        } finally {
            redisLock.unlock(playerFreeLockKey);
        }
        if (addItems != null && addItems.success()) {
            // 记录领取日志
            activityLogger.sendCashCowFreeRewards(playerController.getPlayer(), data, addItems.data, freeRewards);
        }
        res.activityId = req.activityId;
        res.itemInfos = ItemUtils.buildItemInfo(freeRewards.getId(), freeRewards.getItemCount());
        return res;
    }

    @Override
    public void checkPlayerDataAndReset(long playerId, ActivityData activityData) {
        cashCowDao.delPlayerActivityProgress(playerId, activityData.getId());
        playerActivityDao.deletePlayerActivityData(playerId, activityData.getType(), activityData.getId());
    }
}
