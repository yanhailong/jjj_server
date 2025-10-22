package com.jjg.game.hall.pointsaward.turntable;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.PointsAwardType;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.hall.pointsaward.PointsAwardService;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardTurntableConfig;
import com.jjg.game.hall.pointsaward.pb.PointsAwardTurntableHistory;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.PointsAwardTurntableCfg;
import org.redisson.api.RDeque;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 积分大奖转盘服务
 */
@Service
public class PointsAwardTurntableService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 配置初始化时间
     */
    private LocalDate configDate;

    private final PointsAwardService pointsAwardService;
    private final PlayerPackService playerPackService;
    private final RedissonClient redissonClient;
    private final RedisLock redisLock;

    private final TreeMap<Integer, PointsAwardTurntableCfg> cfgTreeMap = new TreeMap<>();

    /**
     * 次数消耗map
     */
    private RMap<Long, Integer> countMap;
    /**
     * 通过充值增加的转盘次数
     */
    private RMap<Long, Integer> addCountMap;

    public PointsAwardTurntableService(PointsAwardService pointsAwardService,
                                       PlayerPackService playerPackService,
                                       RedissonClient redissonClient,
                                       RedisLock redisLock) {
        this.pointsAwardService = pointsAwardService;
        this.redissonClient = redissonClient;
        this.playerPackService = playerPackService;
        this.redisLock = redisLock;
    }

    public void init() {
        initConfig();
        initMap();
    }

    /**
     * 初始化Map数据结构
     * 使用分布式锁确保多节点环境下的数据一致性
     * 移除过期时间设置，统一使用定时清除机制
     */
    public void initMap() {
        redisLock.lockAndRun(PointsAwardConstant.RedisLockKey.POINTS_AWARD_DATA_LOCK_TURNTABLE_INIT, PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS, () -> {
            // 初始化转盘次数统计Map
            countMap = redissonClient.getMap(PointsAwardConstant.RedisKey.TURNTABLE_COUNT);

            // 初始化充值增加次数Map
            addCountMap = redissonClient.getMap(PointsAwardConstant.RedisKey.TURNTABLE_ADD_COUNT);

            log.info("转盘数据Map初始化完成");
        });
    }

    /**
     * 配置重载和每日重置
     * 使用分布式锁确保多节点环境下只有一个节点执行重置操作
     */
    public void dailyReset() {
        LocalDate now = LocalDate.now();
        if (now.getMonthValue() != configDate.getMonthValue()) {
            //重新初始化配置
            initConfig();
        }

        // 使用分布式锁确保多节点环境下的数据一致性
        redisLock.lockAndRun(PointsAwardConstant.RedisLockKey.POINTS_AWARD_DATA_LOCK_TURNTABLE_INIT, PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS, () -> {
            try {
                // 原子性清除所有相关数据
                if (countMap != null) {
                    countMap.clear();
                }
                if (addCountMap != null) {
                    addCountMap.clear();
                }
                log.info("转盘每日数据重置完成");
            } catch (Exception e) {
                log.error("转盘每日数据重置失败", e);
                throw e;
            }
        });
    }

    /**
     * 初始化配置
     */
    public void initConfig() {
        configDate = LocalDate.now();
        LocalDate now = LocalDate.now();
        //当前月最大天数
        int totalDays = now.lengthOfMonth();
        List<PointsAwardTurntableCfg> cfgList = GameDataManager.getPointsAwardTurntableCfgList();
        if (cfgList != null && !cfgList.isEmpty()) {
            //先保存一份默认配置
            List<PointsAwardTurntableCfg> resultList = cfgList.stream().filter(cfg -> {
                String time = cfg.getTime();
                //没有时间限制 并且小于本月最大天数 不加载多余配置有几天就加载几条
                return (time == null || time.isEmpty()) && cfg.getId() <= totalDays;
            }).toList();
            if (resultList.isEmpty()) {
                log.warn("积分大奖转盘配置没有默认配置!");
            }
            //根据当前月份筛选一份新的配置
            List<PointsAwardTurntableCfg> mothConfigList = cfgList.stream().filter(cfg -> {
                String time = cfg.getTime();
                //有时间限制
                if (time != null && !time.isEmpty()) {
                    long timestamp = TimeHelper.getTimestamp(time.trim());
                    //有时间限制 并且小于本月最大天数 不加载多余配置有几天就加载几条
                    if (timestamp > 0) {
                        LocalDate dateTime = LocalDate.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                        return dateTime.isEqual(now) && cfg.getId() <= totalDays;
                    }
                }
                return false;
            }).toList();
            cfgTreeMap.clear();
            if (!mothConfigList.isEmpty()) {
                mothConfigList.forEach(cfg -> cfgTreeMap.put(cfg.getId(), cfg));
            } else {
                resultList.forEach(cfg -> cfgTreeMap.put(cfg.getId(), cfg));
            }
            if (cfgTreeMap.isEmpty()) {
                log.error("积分大奖转盘配置加载失败!");
            }
        }
    }

    /**
     * 获取转盘配置
     */
    public PointsAwardTurntableCfg getCfg(int gridId) {
        return cfgTreeMap.get(gridId);
    }

    /**
     * 获取当前的转盘配置列表
     */
    public List<PointsAwardTurntableConfig> getConfigList() {
        List<PointsAwardTurntableConfig> result = new ArrayList<>();
        if (cfgTreeMap.isEmpty()) {
            return result;
        }
        return cfgTreeMap.values().stream().map(cfg -> {
            PointsAwardTurntableConfig config = new PointsAwardTurntableConfig();
            config.setGridId(cfg.getId());
            config.setIntegralNum(cfg.getIntegralNum());
            config.setItemList(ItemUtils.buildItemInfos(cfg.getGetItem()));
            return config;
        }).toList();
    }

    /**
     * 玩家旋转转盘
     *
     * @param playerId 玩家id
     * @return true 成功
     */
    public int spin(long playerId) {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(40);
        if (globalConfigCfg == null) {
            return -1;
        }
        // 固定的最大次数
        int maxCount = GameDataManager.getGlobalConfigCfg(43).getIntValue();
        //积分消耗数量
        int consume = globalConfigCfg.getIntValue();
        //转盘验证条件
        Supplier<Boolean> supplier = () -> {
            int count = 0;
            if (countMap != null) {
                count = countMap.getOrDefault(playerId, 0);
            }
            int dayMaxCount = maxCount;
            //查看是否有增加的转盘次数
            if (addCountMap != null) {
                dayMaxCount = getAddCount(playerId) + maxCount;
            }
            return count < dayMaxCount;
        };
        //验证扣除并且返回结果
        return pointsAwardService.deduct(playerId, consume, supplier, () -> {
            int selectedId = -1;
            // 通过校验与扣费后再出结果，缩短锁持有时间，避免锁嵌套
            Map<Integer, Integer> probabilityMap = cfgTreeMap.values().stream()
                    .collect(Collectors.toMap(PointsAwardTurntableCfg::getId, PointsAwardTurntableCfg::getProbability, (a, b) -> b));
            Set<Integer> selectedIds = RandomUtils.getRandomByWeight(probabilityMap, 1);
            if (!selectedIds.iterator().hasNext()) {
                return selectedId;
            }
            selectedId = selectedIds.iterator().next();
            // 发送奖励
            PointsAwardTurntableCfg awardTurntableCfg = getCfg(selectedId);
            if (awardTurntableCfg != null) {
                int integralPoints = awardTurntableCfg.getIntegralNum();
                // 积分奖励（内部自带锁，与扣费不再嵌套）
                if (integralPoints > 0) {
                    pointsAwardService.add(playerId, integralPoints, PointsAwardType.TURNTABLE);
                }
                // 道具奖励
                if (awardTurntableCfg.getGetItem() != null && !awardTurntableCfg.getGetItem().isEmpty()) {
                    playerPackService.addItems(playerId, ItemUtils.buildItems(awardTurntableCfg.getGetItem()), "积分大奖转盘奖励");
                }
                PointsAwardTurntableHistory history = new PointsAwardTurntableHistory();
                history.setPlayerId(playerId);
                history.setAwardId(selectedId);
                history.setTime(System.currentTimeMillis());
                history.setIntegralNum(integralPoints);
                history.getItemInfoList().addAll(ItemUtils.buildItemInfos(awardTurntableCfg.getGetItem()));
                addHistory(history);
                //增加玩家转盘次数
                countMap.fastPut(playerId, countMap.getOrDefault(playerId, 0) + 1);
            } else {
                log.warn("玩家[{}]积分大奖转盘奖励发送失败!中奖id[{}]配置不存在!", playerId, selectedId);
            }
            return selectedId;
        }, -1, PointsAwardType.TURNTABLE);
    }

    public String historyKey(long playerId) {
        return PointsAwardConstant.RedisKey.POINTS_AWARD_TURNTABLE_HISTORY + playerId;
    }

    /**
     * 添加一条历史记录 会自动维护记录长度
     *
     * @param history 历史记录
     */
    public void addHistory(PointsAwardTurntableHistory history) {
        RDeque<PointsAwardTurntableHistory> dequeHistory = redissonClient.getDeque(historyKey(history.getPlayerId()));
        dequeHistory.addFirst(history);
        if (dequeHistory.size() > PointsAwardConstant.Turntable.HISTORY_MAX_SIZE) {
            dequeHistory.removeLast();
        }
    }

    /**
     * 获取历史记录
     *
     * @param playerId 玩家id
     * @return 记录列表
     */
    public List<PointsAwardTurntableHistory> getHistoryList(long playerId) {
        RDeque<PointsAwardTurntableHistory> dequeHistory = redissonClient.getDeque(historyKey(playerId));
        return dequeHistory.readAll();
    }

    /**
     * 当前的旋转次数
     */
    public int getCount(long playerId) {
        return countMap.getOrDefault(playerId, 0);
    }

    /**
     * 最大旋转次数
     */
    public int getMaxCount(long playerId) {
        // 固定的最大次数
        int maxCount = GameDataManager.getGlobalConfigCfg(43).getIntValue();
        // 额外增加的次数
        int addCount = getAddCount(playerId);
        return maxCount + addCount;
    }

    /**
     * 增加玩家转盘次数
     *
     * @param playerId 玩家id
     * @param count    增加次数
     */
    public void replaceCount(long playerId, int count) {
        RLock lock = addCountMap.getReadWriteLock(playerId).writeLock();
        if (lock.tryLock()) {
            addCountMap.fastPut(playerId, count);
            lock.unlock();
        }
    }

    /**
     * 获取玩家增加的充值次数
     *
     * @param playerId 玩家id
     */
    public int getAddCount(long playerId) {
        return addCountMap.getOrDefault(playerId, 0);
    }

    /**
     * 获取配置的金额
     */
    public int getRechargeCheckValue() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(44);
        if (globalConfigCfg == null) {
            return 0;
        }
        //每充值这么多就奖励一次
        return globalConfigCfg.getIntValue();
    }

    /**
     * 玩家充值
     *
     * @param order 订单信息
     */
    public void recharge(Order order) {
        long playerId = order.getPlayerId();
        int checkValue = getRechargeCheckValue();
        long recharge = pointsAwardService.getRecharge(playerId);
        if (recharge <= 0 || recharge < checkValue) {
            return;
        }
        int resultCount = Math.toIntExact(recharge / checkValue);
        if (resultCount > 0) {
            replaceCount(playerId, resultCount);
        }
    }
}
