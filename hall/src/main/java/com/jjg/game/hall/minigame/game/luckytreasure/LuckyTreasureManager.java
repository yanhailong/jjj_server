package com.jjg.game.hall.minigame.game.luckytreasure;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.listener.IGameClusterLeaderListener;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.config.ConfigManager;
import com.jjg.game.core.config.bean.LuckyTreasureConfig;
import com.jjg.game.core.constant.AwardCodeType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.LuckyTreasureConstant;
import com.jjg.game.core.dao.luckytreasure.LuckyTreasureDao;
import com.jjg.game.core.dao.luckytreasure.LuckyTreasureRedisDao;
import com.jjg.game.core.data.LuckyTreasure;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.manager.AwardCodeManager;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.hall.logger.MinigameLogger;
import com.jjg.game.hall.minigame.MinigameManager;
import com.jjg.game.hall.minigame.event.MinigameReadyEvent;
import com.jjg.game.hall.minigame.game.luckytreasure.bean.LuckyTreasureTimerEvent;
import com.jjg.game.hall.minigame.game.luckytreasure.service.LuckyTreasureService;
import com.jjg.game.hall.minigame.game.luckytreasure.util.LuckyTreasureStatusUtil;
import com.jjg.game.hall.service.HallPlayerService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.MailCfg;
import com.jjg.game.sampledata.bean.RobotCfg;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 夺宝奇兵管理器
 */
@Component
public class LuckyTreasureManager implements IGameClusterLeaderListener, TimerListener<LuckyTreasureTimerEvent> {
    private static final Logger log = LoggerFactory.getLogger(LuckyTreasureManager.class);

    private final LuckyTreasureDao luckyTreasureDao;
    private final LuckyTreasureRedisDao luckyTreasureRedisDao;
    private final RedisLock redisLock;
    private final MarsCurator marsCurator;
    private final TimerCenter timerCenter;
    private final LuckyTreasureService luckyTreasureService;
    private final MinigameManager minigameManager;
    private final ConfigManager configManager;
    private final HallPlayerService hallPlayerService;
    private final MinigameLogger minigameLogger;
    private final MailService mailService;


    /**
     * 活动定时器映射：期号 -> 定时器事件
     */
    private final Map<Long, TimerEvent<LuckyTreasureTimerEvent>> activityTimers = new ConcurrentHashMap<>();
    /**
     * 活动机器人购买定时器映射：期号 -> 定时器事件
     */
    private final Map<Long, TimerEvent<LuckyTreasureTimerEvent>> activityBuyTimers = new ConcurrentHashMap<>();
    private final AwardCodeManager awardCodeManager;

    private AtomicBoolean isInit = new AtomicBoolean(false);

    public LuckyTreasureManager(LuckyTreasureDao luckyTreasureDao,
                                LuckyTreasureRedisDao luckyTreasureRedisDao,
                                ConfigManager configManager,
                                RedisLock redisLock,
                                MarsCurator marsCurator,
                                TimerCenter timerCenter,
                                LuckyTreasureService luckyTreasureService,
                                MinigameManager minigameManager,
                                HallPlayerService hallPlayerService,
                                MinigameLogger minigameLogger,
                                AwardCodeManager awardCodeManager,
                                MailService mailService) {
        this.luckyTreasureDao = luckyTreasureDao;
        this.luckyTreasureRedisDao = luckyTreasureRedisDao;
        this.redisLock = redisLock;
        this.marsCurator = marsCurator;
        this.timerCenter = timerCenter;
        this.luckyTreasureService = luckyTreasureService;
        this.minigameManager = minigameManager;
        this.configManager = configManager;
        this.hallPlayerService = hallPlayerService;
        this.minigameLogger = minigameLogger;
        this.awardCodeManager = awardCodeManager;
        this.mailService = mailService;
    }

    /**
     * 初始化
     */
    @EventListener(MinigameReadyEvent.class)
    public void init(MinigameReadyEvent event) {
        //幸运夺宝小游戏开启了才初始化
        if (event.getGameId() == LuckyTreasureConstant.Common.GAME_ID) {
            if (marsCurator.isMaster()) {
                //检测服务器重启后是否有未处理数据
                recoverUnsettledRounds();
                //检查并启动缺失的活动
                startMissingActivities();
                // 启动新活动（只有主节点才执行）
                startNewActivitiesIfNeeded();
                //初始化机器人购买定时器
                initRobotTimer();
            }
            //监听配置文件变化 如果有新增的夺宝奇兵配置则直接开始
            configManager.addUpdateConfigListener(LuckyTreasureConfig.class, (a, b, c) -> {
                log.info("夺宝奇兵配置更新!检测是否需要新增!id={},b = {}", c.getId(), b);
                startNewActivityForConfig(c);
            });

        }
        //设置初始化标记
        isInit.set(true);
    }

    /**
     * 成为主节点时触发
     */
    @Override
    public void isLeader() {
        // 初始化标记未设置 不执行 以下步骤
        if (!isInit.get()) {
            return;
        }
        log.info("夺宝奇兵管理器成为主节点，开始启动活动管理");
        try {
            //检测服务器重启后是否有未处理数据
            recoverUnsettledRounds();
            //检查并启动缺失的活动
            startMissingActivities();
            // 启动新活动（只有主节点才执行）
            startNewActivitiesIfNeeded();
            //初始化机器人购买定时器
            initRobotTimer();
        } catch (Exception e) {
            log.error("夺宝奇兵管理器启动异常", e);
        }
    }

    /**
     * 失去主节点身份时触发
     */
    @Override
    public void notLeader() {
        log.info("夺宝奇兵管理器失去主节点身份");
    }

    /**
     * 定时器事件回调
     *
     * @param timerEvent 定时器事件
     */
    @Override
    public void onTimer(TimerEvent<LuckyTreasureTimerEvent> timerEvent) {
        LuckyTreasureTimerEvent event = timerEvent.getParameter();
        if (event == null) {
            return;
        }
        long issueNumber = event.issueNumber();
        LuckyTreasureTimerEvent.TimerType timerType = event.timerType();
        try {
            // 只有主节点才处理定时器事件
            if (!marsCurator.isMaster()) {
                return;
            }
            //结束
            if (timerType == LuckyTreasureTimerEvent.TimerType.ACTIVITY_REWARD) {
                handleActivityRewardTimer(issueNumber);
            } else if (timerType == LuckyTreasureTimerEvent.TimerType.ACTIVITY_END) {
                handleActivityEndTimer(issueNumber);
            } else if (timerType == LuckyTreasureTimerEvent.TimerType.ROBOT_BUY) {
                //机器人购买
                handleActivityRobotBuyTimer(issueNumber);
            } else {
                log.warn("未知的夺宝奇兵定时器事件类型: {}", timerType);
            }
        } catch (Exception e) {
            log.error("处理夺宝奇兵定时器事件失败，期号: {}, 类型: {}", issueNumber, timerType, e);
        }
    }


    /**
     * 初始化机器人购买定时器
     */
    public void initRobotTimer() {
        RMapCache<Long, LuckyTreasure> map = luckyTreasureRedisDao.getActiveTreasures();
        if (map == null || map.isEmpty()) {
            return;
        }

        map.forEach((k, v) -> {
            TimerEvent<LuckyTreasureTimerEvent> event = activityBuyTimers.get(v.getIssueNumber());
            if (event == null) {
                if (checkRobotBuy(v)) {
                    //添加机器人购买定时器
                    addRobotBuyTimer(v);
                }
            }
        });
    }

    /**
     * 检查是否符合机器人购买的逻辑。
     *
     * @param activeTreasure 当前活跃的夺宝奇兵活动实例。
     * @return 如果符合机器人购买条件返回true，否则返回false。
     */
    public boolean checkRobotBuy(LuckyTreasure activeTreasure) {
        if (activeTreasure == null) {
            return false;
        }
        if (activeTreasure.getStatus() != LuckyTreasureStatusUtil.STATUS_CAN_BUY) {
            return false;
        }
        LuckyTreasureConfig config = activeTreasure.getConfig();
        //机器人购买限制
        List<Integer> robotSinglePurchase = config.getRobotSinglePurchase();
        if (robotSinglePurchase == null || robotSinglePurchase.isEmpty()) {
            return false;
        }
        List<RobotCfg> robotCfgList = GameDataManager.getRobotCfgList();
        if (robotCfgList == null || robotCfgList.isEmpty()) {
            return false;
        }
        RobotCfg robotCfg = robotCfgList.get(RandomUtils.randomMinMax(0, robotCfgList.size() - 1));
        if (robotCfg == null) {
            return false;
        }
        int total = config.getTotal();
        //机器人购买上限万分比
        int robotHaveMax = config.getRobotHaveMax();
        // 当前售出的数量
        int soldCount = activeTreasure.getSoldCount();
        //库存限制 机器人不能在低于这个库存的时候再进行购买逻辑
        int limitCount = (int) (((double) robotHaveMax / 10000) * total);
        //不买了
        return soldCount <= limitCount;
    }

    /**
     * 机器人购买逻辑
     */
    public void robotBuy(long issueNumber) {
        LuckyTreasure treasureDetails = luckyTreasureRedisDao.getTreasureByIssueNumber(issueNumber);
        if (treasureDetails == null) {
            return;
        }
        LuckyTreasureConfig config = treasureDetails.getConfig();
        //机器人购买限制
        List<Integer> robotSinglePurchase = config.getRobotSinglePurchase();
        if (robotSinglePurchase == null || robotSinglePurchase.isEmpty()) {
            return;
        }
        List<RobotCfg> robotCfgList = GameDataManager.getRobotCfgList();
        if (robotCfgList == null || robotCfgList.isEmpty()) {
            return;
        }
        RobotCfg robotCfg = robotCfgList.get(RandomUtils.randomMinMax(0, robotCfgList.size() - 1));
        if (robotCfg == null) {
            return;
        }
        //不能购买
        if (!checkRobotBuy(treasureDetails)) {
            return;
        }
        //总库存
        int total = config.getTotal();
        //机器人购买上限万分比
        int robotHaveMax = config.getRobotHaveMax();
        //库存限制 机器人不能在低于这个库存的时候再进行购买逻辑
        int limitCount = (int) (((double) robotHaveMax / 10000) * total);
        //随机购买数量万分比
        int buyCountPr = RandomUtils.randomMinMax(robotSinglePurchase.getFirst(), robotSinglePurchase.getLast());
        //当前总购买数量
        int totalBuy = (int) (((double) buyCountPr / 10000) * total);
        int soldCount = treasureDetails.getSoldCount();
        if (soldCount + totalBuy > limitCount) {
            return;
        }
        //在写锁中重新获取最新数据
        LuckyTreasure latestTreasure = luckyTreasureRedisDao.getTreasureByIssueNumber(issueNumber);
        //购买
        int resultCode = robotBuyWithoutLock(robotCfg.getId(), latestTreasure, totalBuy);
        if (resultCode == Code.SUCCESS) {
            //购买成功通知更新 广播到所有节点
            luckyTreasureService.broadcastUpdate(latestTreasure.getIssueNumber());
            //购买失败的话继续添加定时器
            addRobotBuyTimer(treasureDetails);
        }
    }

    /**
     * 执行购买逻辑（在写锁内执行，道具已扣除）
     */
    private int robotBuyWithoutLock(long playerId, LuckyTreasure latestTreasure, int count) {
        try {
            if (latestTreasure == null || luckyTreasureService.calculateStatus(latestTreasure, playerId) != LuckyTreasureStatusUtil.STATUS_CAN_BUY) {
                return Code.FAIL;
            }
            // 检查剩余数量
            int remainingCount = latestTreasure.getConfig().getTotal() - latestTreasure.getSoldCount();
            if (count <= 0 || remainingCount < count) {
                return Code.FAIL;
            }

            // 执行购买
            latestTreasure = luckyTreasureRedisDao.buyTreasure(latestTreasure.getIssueNumber(), playerId, count);
            if (latestTreasure == null) {
                return Code.FAIL;
            }
            // 购买成功，更新数据库
            luckyTreasureDao.save(latestTreasure);
            log.info("夺宝奇兵机器人购买成功, 机器人ID:{}, 期号:{}, 购买数量:{}", playerId, latestTreasure.getIssueNumber(), count);
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("执行购买逻辑失败", e);
            return Code.EXCEPTION;
        }
    }

    /**
     * 查看是否有关服前未处理数据
     */
    public void recoverUnsettledRounds() {
        log.info("执行。。。。。。检测服务器重启后是否有未处理数据");
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(LuckyTreasureConstant.Common.LUCKY_TREASURE_GLOBAL_CONFIG_ID);
        int intValue = globalConfigCfg.getIntValue();
        //查询数据库中是否存在上次未开奖的
        List<LuckyTreasure> luckyTreasures = luckyTreasureDao.findAllNotEnd(intValue);
        for (LuckyTreasure luckyTreasure : luckyTreasures) {
            log.info("夺宝奇兵 查看是否有关服前未处理数据 luckyTreasure{}", JSONObject.toJSONString(luckyTreasure));
            long now = System.currentTimeMillis();
            long endTime = luckyTreasure.getEndTime();
            if (isExpired(endTime, now)) {
                long rewardTime = LuckyTreasureStatusUtil.calculateRewardTimeMillis(luckyTreasure);
                if (isExpired(rewardTime, now)) {
                    log.debug("该幸运夺宝活动已过期，立即处理开奖 isserNum = {}", luckyTreasure.getIssueNumber());
                    // 已过期，立即处理
                    handleActivityRewardTimer(luckyTreasure.getIssueNumber());
                } else {
                    log.debug("该幸运夺宝活动已过期，立即结束 isserNum = {}", luckyTreasure.getIssueNumber());
                    // 已过期，立即处理
                    handleActivityEndTimer(luckyTreasure.getIssueNumber());
                }
            } else {
                log.debug("该幸运夺宝活动未过期，添加过期定时器 isserNum = {}", luckyTreasure.getIssueNumber());
                // 未过期，添加定时器
                addActivityEndTimer(luckyTreasure);
            }
        }
    }


    /**
     * 启动新活动如果需要的话
     */
    public void startNewActivitiesIfNeeded() {
        List<LuckyTreasureConfig> configs = configManager.getConfigs(LuckyTreasureConfig.class);
        for (LuckyTreasureConfig config : configs) {
            log.info("===============>检查配置 {} 是否需要启动新活动", config.getId());
            if (isOpen() && config.isRepeated() && !luckyTreasureRedisDao.hasActiveRound(config.getId())) {
                startNewActivityForConfig(config);
            }
        }
    }

    /**
     * 检查并启动缺失的活动
     * 确保每个配置都有对应的活跃夺宝奇兵活动
     */
    private void startMissingActivities() {
        try {
            List<LuckyTreasureConfig> configs = configManager.getConfigs(LuckyTreasureConfig.class);
            if (configs.isEmpty()) {
                log.warn("夺宝奇兵配置列表为空，跳过活动检查");
                return;
            }

            int startedCount = 0;
            for (LuckyTreasureConfig config : configs) {
                // 检查该配置是否已有活跃活动
                if (!luckyTreasureRedisDao.hasActiveRound(config.getId())) {
                    log.info("检测到配置 {} 没有活跃活动，准备启动新活动", config.getId());
                    if (isOpen() && config.isRepeated()) {
                        startNewActivityForConfig(config);
                        startedCount++;
                    } else {
                        log.debug("配置 {} 未开启活动!", config.getId());
                    }
                } else {
                    log.debug("配置 {} 已有活跃活动，跳过", config.getId());
                }
            }

            log.info("夺宝奇兵活动检查完成，共启动 {} 个新活动", startedCount);

        } catch (Exception e) {
            log.error("检查并启动缺失活动时发生错误", e);
        }
    }

    /**
     * 为指定配置启动新活动
     */
    private void startNewActivityForConfig(LuckyTreasureConfig config) {
        if (!marsCurator.isMaster()) {
            log.warn("非主节点，无法开启幸运夺宝新活动 cfgIf = {}", config.getId());
            return;
        }

        // 检查该配置ID是否已有活跃活动
        if (luckyTreasureRedisDao.hasActiveRound(config.getId())) {
            return;
        }
        // 创建新活动
        LuckyTreasure newRound = createNewRound(config);
        // 保存到数据库
        luckyTreasureDao.save(newRound);
        // 保存到Redis
        saveActiveRoundToRedis(newRound);
        // 添加活动结束定时器
        addActivityEndTimer(newRound);
        //通知更新
        luckyTreasureService.broadcastUpdate(newRound.getIssueNumber());
        log.info("启动新的夺宝奇兵活动，配置ID: {}, 期号: {}", config.getId(), newRound.getIssueNumber());

        //添加机器人购买定时器
        if (checkRobotBuy(newRound)) {
            addRobotBuyTimer(newRound);
        }
    }

    /**
     * 注册机器人购买定时器
     */
    public void addRobotBuyTimer(LuckyTreasure newRound) {
        long issueNumber = newRound.getIssueNumber();
        LuckyTreasureConfig config = newRound.getConfig();
        if (config.getRobotHaveMax() <= 0) {
            return;
        }
        //机器人购买间隔
        List<Integer> buyTimeList = config.getRobotTime();
        if (buyTimeList == null || buyTimeList.isEmpty()) {
            return;
        }
        //间隔时间
        int interval = RandomUtils.randomMinMax(buyTimeList.getFirst(), buyTimeList.getLast());
        //注册机器人购买定时器
        TimerEvent<LuckyTreasureTimerEvent> buyTimer = new TimerEvent<>(
                this,
                new LuckyTreasureTimerEvent(issueNumber, LuckyTreasureTimerEvent.TimerType.ROBOT_BUY),
                0, // 不重复执行
                1, // 只执行一次
                interval, // 延迟时间
                false // 相对时间
        );
        // 添加到定时器中心
        timerCenter.add(buyTimer);
        activityBuyTimers.put(issueNumber, buyTimer);
        log.info("夺宝奇兵期号[{}],configId[{}]增加机器人购买定时器!", issueNumber, config.getId());
    }

    /**
     * 创建新的活动轮次
     */
    private LuckyTreasure createNewRound(LuckyTreasureConfig config) {
        LuckyTreasure round = new LuckyTreasure();
        // 生成期号
        long issueNumber = generateIssueNumber(config.getId());
        round.setIssueNumber(issueNumber);
        //记录开启时的配置
        round.setConfig(config);
        // 设置时间信息
        long now = System.currentTimeMillis();
        round.setStartTime(now);
        round.setEndTime(calculateEndTimeMillis(now, config.getTime()));
        round.setStatus(LuckyTreasureStatusUtil.STATUS_CAN_BUY);
        // 初始化购买数据
        round.setBuyMap(new HashMap<>());
        return round;
    }

    /**
     * 生成期号 格式：年月日 + 配置ID(2位) + 5位序号 (如：202509170800001)
     */
    private long generateIssueNumber(int configId) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long dailyCounter = luckyTreasureRedisDao.generateIssueNumber(configId, today);
        // 组合期号：年月日 + 配置ID(2位) + 序号(5位)
        String issueNumberStr = today + String.format("%02d", configId) + String.format("%05d", dailyCounter);
        return Long.parseLong(issueNumberStr);
    }

    /**
     * 保存活跃活动到Redis
     */
    private void saveActiveRoundToRedis(LuckyTreasure round) {
        // 领奖时间
        long rewardTime = 0L;
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(LuckyTreasureConstant.Common.LUCKY_TREASURE_GLOBAL_REWARED_CONFIG_ID);
        if (globalConfigCfg == null || globalConfigCfg.getIntValue() < 1) {
            rewardTime = TimeUnit.SECONDS.toMillis(globalConfigCfg.getIntValue());
        }

//         设置过期时间
        int expireMinutes = Math.toIntExact(round.getConfig().getTime() + rewardTime + round.getConfig().getCollectTime() + 10);
        luckyTreasureRedisDao.saveActiveRound(round, expireMinutes);
    }


    /**
     * 为活动添加结束定时器
     *
     * @param round 活动数据
     */
    private void addActivityEndTimer(LuckyTreasure round) {
        long issueNumber = round.getIssueNumber();

        // 如果已有定时器，先移除
        removeActivityTimer(issueNumber);

        // 计算活动结束时间
        long endTime = round.getEndTime();
        long currentTime = System.currentTimeMillis();

        if (endTime <= currentTime) {
            // 如果已经过期，立即处理
            handleActivityEndTimer(issueNumber);
            return;
        }

        // 创建定时器事件
        LuckyTreasureTimerEvent timerEvent = new LuckyTreasureTimerEvent(
                issueNumber, LuckyTreasureTimerEvent.TimerType.ACTIVITY_END);

        // 计算延迟时间（毫秒）
        long delayMillis = endTime - currentTime;

        log.info("夺宝奇兵{}结束延迟时间millis: {}", issueNumber, delayMillis);
        // 确保延迟时间不为负数
        if (delayMillis <= 0) {
            log.warn("活动 {} 延迟时间计算异常，延迟时间: {}ms，立即处理", issueNumber, delayMillis);
            handleActivityEndTimer(issueNumber);
            return;
        }

        TimerEvent<LuckyTreasureTimerEvent> timer = new TimerEvent<>(
                this,
                timerEvent,
                0, // 不重复执行
                1, // 只执行一次
                (int) delayMillis, // 延迟时间
                false // 相对时间
        );

        // 添加到定时器中心
        timerCenter.add(timer);
        activityTimers.put(issueNumber, timer);

        log.debug("为夺宝奇兵活动 {} 添加结束定时器，结束时间: {}", issueNumber, endTime);
    }

    /**
     * 为活动添加开奖定时器
     *
     * @param round 活动数据
     */
    private void addActivityRewardTimer(LuckyTreasure round) {
        long issueNumber = round.getIssueNumber();

        // 如果已有定时器，先移除
        removeActivityTimer(issueNumber);

        // 计算活动开奖时间
        long rewardTime = LuckyTreasureStatusUtil.calculateRewardTimeMillis(round);
        long currentTime = System.currentTimeMillis();

        if (rewardTime <= currentTime) {
            log.debug("number = {}, rewardTime = {},currentTime = {}", round.getIssueNumber(), rewardTime, currentTime);
            // 如果已经过期，立即处理
            handleActivityRewardTimer(issueNumber);
            return;
        }

        // 创建定时器事件
        LuckyTreasureTimerEvent timerEvent = new LuckyTreasureTimerEvent(
                issueNumber, LuckyTreasureTimerEvent.TimerType.ACTIVITY_REWARD);

        // 计算延迟时间（毫秒）
        long delayMillis = rewardTime - currentTime;

        log.info("夺宝奇兵{}开奖延迟时间millis: {}", issueNumber, delayMillis);
        // 确保延迟时间不为负数
        if (delayMillis <= 0) {
            log.warn("活动 {} 延迟开奖时间计算异常，延迟时间: {}ms，立即处理", issueNumber, delayMillis);
            handleActivityEndTimer(issueNumber);
            return;
        }

        TimerEvent<LuckyTreasureTimerEvent> timer = new TimerEvent<>(
                this,
                timerEvent,
                0, // 不重复执行
                1, // 只执行一次
                (int) delayMillis, // 延迟时间
                false // 相对时间
        );

        // 添加到定时器中心
        timerCenter.add(timer);
        activityTimers.put(issueNumber, timer);

        log.debug("为夺宝奇兵活动 {} 添加开奖定时器，开奖时间: {}", issueNumber, rewardTime);
    }


    /**
     * 移除活动的定时器
     *
     * @param issueNumber 期号
     */
    private void removeActivityTimer(long issueNumber) {
        TimerEvent<LuckyTreasureTimerEvent> timer = activityTimers.remove(issueNumber);
        if (timer != null) {
            timerCenter.remove(timer);
            log.debug("移除夺宝奇兵活动 {} 的定时器", issueNumber);
        }
    }

    /**
     * 处理活动结束定时器事件
     *
     * @param issueNumber 期号
     */
    private void handleActivityEndTimer(long issueNumber) {
        // 移除定时器
        removeActivityTimer(issueNumber);

        //获取配置的开奖时间
        int rewardTime = 0;
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(LuckyTreasureConstant.Common.LUCKY_TREASURE_GLOBAL_REWARED_CONFIG_ID);
        if (globalConfigCfg != null && globalConfigCfg.getIntValue() > 0) {
            rewardTime = globalConfigCfg.getIntValue();
            log.debug("获取配置的延迟开奖时间 rewardTime = {}", rewardTime);
        }

        if (rewardTime < 1) {
            handleActivityRewardTimer(issueNumber);
        } else {
            RMapCache<Long, LuckyTreasure> activeTreasures = luckyTreasureRedisDao.getActiveTreasures();
            // 从redis获取活动数据
            LuckyTreasure round = activeTreasures.get(issueNumber);
            if (round == null || round.getStatus() != LuckyTreasureStatusUtil.STATUS_CAN_BUY) {
                log.warn("幸运夺宝开奖时出错 issueNumber = {},roundNull = {},status = {}", issueNumber, round == null, round == null ? "null" : round.getStatus());
                return;
            }

            //标记等待开奖
            round.setStatus(LuckyTreasureStatusUtil.STATUS_WAIT_DRAW);
            activeTreasures.put(issueNumber, round);
            addActivityRewardTimer(round);
            luckyTreasureService.broadcastUpdate(issueNumber);
            log.debug("finalRewardTime = {},添加开奖倒计时", rewardTime);
        }
    }

    /**
     * 处理活动开奖定时器事件
     *
     * @param issueNumber 期号
     */
    private void handleActivityRewardTimer(long issueNumber) {
        log.debug("开始处理幸运夺宝开奖事件 issueNumber = {}", issueNumber);
        // 移除定时器
        removeActivityTimer(issueNumber);

        RMapCache<Long, LuckyTreasure> activeTreasures = luckyTreasureRedisDao.getActiveTreasures();
        // 从redis获取活动数据
        LuckyTreasure round = activeTreasures.get(issueNumber);
        if (round == null || (round.getStatus() != LuckyTreasureStatusUtil.STATUS_CAN_BUY && round.getStatus() != LuckyTreasureStatusUtil.STATUS_WAIT_DRAW)) {
            log.debug("status = {}", round == null ? "null" : round.getStatus());
            return;
        }

        reward(round);
        luckyTreasureDao.save(round);

        luckyTreasureService.broadcastUpdate(issueNumber);

        log.info("夺宝奇兵活动结束并开奖完成，期号= {}, 中奖玩家= {}", issueNumber, round.getAwardPlayerId());
        LuckyTreasureConfig luckyTreasureConfig = round.getConfig();
        // 清理活跃状态
        luckyTreasureRedisDao.removeActiveRoundByIssueNumber(issueNumber);
        // 立即启动下一期活动
        startNextRoundForConfig(luckyTreasureConfig.getId());
    }

    /**
     * 处理活动机器人购买
     *
     * @param issueNumber 期号
     */
    private void handleActivityRobotBuyTimer(long issueNumber) {
        robotBuy(issueNumber);

    }

    /**
     * 为指定配置启动下一期活动
     *
     * @param configId 配置ID
     */
    private void startNextRoundForConfig(int configId) {
        //验证小游戏是否开启
        if (isOpen()) {
            List<LuckyTreasureConfig> configs = configManager.getConfigs(LuckyTreasureConfig.class);
            if (configs == null || configs.isEmpty()) {
                log.debug("配置为空 ");
            }

            configs.stream()
                    .filter(c -> c.getId() == configId && c.isRepeated())
                    .findFirst().ifPresent(this::startNewActivityForConfig);
        } else {
            log.debug("活动未开启");
        }
    }

    /**
     * 计算活动实际结束时间（毫秒）
     */
    private long calculateEndTimeMillis(LuckyTreasure luckyTreasure) {
        return calculateEndTimeMillis(luckyTreasure.getStartTime(), luckyTreasure.getConfig().getTime());
    }


    private long calculateEndTimeMillis(long startTime, long time) {
        return startTime + TimeUnit.MINUTES.toMillis(time);
    }

    /**
     * 判断是否过期
     */
    private boolean isExpired(long endTimeMillis, long nowMillis) {
        return endTimeMillis <= nowMillis;
    }

    /**
     * 对指定的夺宝奇兵活动进行奖励发放
     */
    public void reward(LuckyTreasure luckyTreasure) {
        Map<Long, Integer> buyMap = luckyTreasure.getBuyMap();
        LuckyTreasureConfig config = luckyTreasure.getConfig();
        //计算中奖玩家
        long winnerPlayerId = randomKey(buyMap, config.getTotal());
        //有玩家中奖
        if (winnerPlayerId > 0) {
            Player player = hallPlayerService.getFromAllDB(winnerPlayerId);
            if (player != null) {
                luckyTreasure.setAwardPlayerHeadFrameId(player.getHeadFrameId());
                luckyTreasure.setAwardPlayerNickName(player.getNickName());
                luckyTreasure.setAwardPlayerHeadImgId(player.getHeadImgId());
                luckyTreasure.setAwardPlayerNationalId(player.getNationalId());
                luckyTreasure.setAwardPlayerLevel(player.getLevel());

                luckyTreasure.setAwardPlayerId(winnerPlayerId);
                // 根据type类型处理奖励
                if (config.getType() == 1) {
                    String rewardCode = awardCodeManager.generateCode(winnerPlayerId, AwardCodeType.LUCK_TREASURE);
                    log.info("夺宝奇兵[{}]结束,玩家[{}]中奖,生成领奖码[{}]", luckyTreasure.getIssueNumber(), winnerPlayerId, rewardCode);
                    luckyTreasure.setRewardCode(rewardCode);
                }
                //标记未领取
                luckyTreasure.setReceived(false);

                //获取奖励邮件配置
                MailCfg mailCfg = GameDataManager.getMailCfg(LuckyTreasureConstant.MailId.REWARD_MAIL_ID);
                //发送邮件奖励
                mailService.addCfgMail(player.getId(), mailCfg.getTitle(), mailCfg.getText(), ItemUtils.buildItemList(config.getItemId(), config.getItemNum()), Collections.emptyList());
            }

        }
        //更新状态
        luckyTreasure.setStatus(LuckyTreasureStatusUtil.STATUS_WAIT_RECEIVE);
        //记录开奖日志
        minigameLogger.finish(luckyTreasure);
    }

    /**
     * 随机获取一个key
     * 根据map的value和传入的总权重计算随机key
     *
     * @param buyMap     购买映射表，key为用户ID，value为购买数量
     * @param totalCount 总权重（可能大于map中value的总和）
     * @return 随机选中的key，如果没有选中则返回0
     */
    public long randomKey(Map<Long, Integer> buyMap, long totalCount) {
        // 如果总权重小于等于0或map为空，返回0
        if (totalCount <= 0 || buyMap == null || buyMap.isEmpty()) {
            return 0;
        }
        // 计算map中所有value的总和
        long mapValueSum = buyMap.values().stream().mapToLong(Integer::longValue).sum();
        // 如果map的value总和为0，返回0
        if (mapValueSum <= 0) {
            return 0;
        }
        // 生成随机数，范围是[0, totalCount)
        long randomValue = RandomUtils.randomLongMinMax(0L, totalCount);
        // 如果随机数大于等于map的value总和，说明没有选中任何key（按既有业务语义：可能不开奖）
        if (randomValue >= mapValueSum) {
            return 0;
        }
        // 遍历map，累加value直到找到对应的key
        long currentSum = 0;
        for (Map.Entry<Long, Integer> entry : buyMap.entrySet()) {
            currentSum += entry.getValue();
            if (randomValue < currentSum) {
                return entry.getKey();
            }
        }
        // 理论上不会到达这里，但为了安全起见返回0
        return 0;
    }

    /**
     * 检查当前游戏是否处于开启状态。
     */
    public boolean isOpen() {
        return minigameManager.isOpenGame(LuckyTreasureConstant.Common.GAME_ID);
    }

}
