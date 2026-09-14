package com.jjg.game.poker.game.douxian.cardlib;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.poker.game.common.cardlib.AbstractCardLibManager;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.douxian.room.DouXianGameController;
import com.jjg.game.poker.game.douxian.room.data.DouXianGameDataVo;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ImmortalCardCfg;
import com.jjg.game.sampledata.bean.PoolResultsCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** 斗仙牌首轮32张结果库管理器。 */
@Component
public class DouXianCardLibManager
        extends AbstractCardLibManager<DouXianCardLib, DouXianCardLibDao> {

    private static final int DEFAULT_ROLLOUT_COUNT = 8;
    private static final int MAX_ROLLOUT_COUNT = 64;
    private static final int ONLINE_CANDIDATE_COUNT = 3;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Autowired
    private DouXianCardLibDao cardLibDao;

    private volatile ImmortalCardCfg generationCfg;
    private volatile Room_ChessCfg generationRoomCfg;
    private volatile int generationPoolId;
    private volatile int generationRolloutCount = DEFAULT_ROLLOUT_COUNT;
    private final AtomicBoolean localGenerationActive = new AtomicBoolean();
    private final AtomicInteger generationCompletedCount = new AtomicInteger();
    private final ExecutorService generationExecutor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "douxian-cardlib-generator");
        thread.setDaemon(true);
        return thread;
    });
    private volatile int generationRequestedCount;
    private volatile long generationStartedAt;
    private volatile long generationFinishedAt;
    private volatile String generationState = "尚未在本节点启动";

    @Override
    protected int getGameType() {
        return CoreConst.GameType.DOU_XIAN;
    }

    @Override
    protected DouXianCardLibDao getCardLibDao() {
        return cardLibDao;
    }

    @Override
    protected boolean prepareGeneration() {
        String poolConfigError = validatePoolResultsConfig();
        if (poolConfigError != null) {
            log.error("生成斗仙牌结果库失败：poolResults配置无效，{}", poolConfigError);
            return false;
        }
        List<ImmortalCardCfg> cfgList = GameDataManager.getImmortalCardCfgList();
        if (cfgList == null || cfgList.isEmpty()) {
            log.error("生成斗仙牌结果库失败：缺少ImmortalCardCfg");
            return false;
        }
        ImmortalCardCfg cfg = cfgList.stream()
                .filter(item -> item.getGameID() == CoreConst.GameType.DOU_XIAN)
                .findFirst().orElse(null);
        if (cfg == null || cfg.getPoolId() <= 0 || cfg.getBetList() <= 0 || cfg.getMaxCap() <= 0) {
            log.error("生成斗仙牌结果库失败：ImmortalCardCfg关键字段缺失 cfg:{}", cfg);
            return false;
        }
        Room_ChessCfg roomCfg = GameDataManager.getRoom_ChessCfg(cfg.getId());
        if (roomCfg == null) {
            log.error("生成斗仙牌结果库失败：找不到对应Room_ChessCfg cfgId:{}", cfg.getId());
            return false;
        }
        generationCfg = cfg;
        generationRoomCfg = roomCfg;
        generationPoolId = cfg.getPoolId();
        return true;
    }

    @Override
    protected List<DouXianCardLib> simulateOneGame() {
        try {
            if (generationCfg == null) {
                return List.of();
            }
            return DouXianCardLibGenerator.simulateOneInitialDeal(
                    generationPoolId, generationCfg, generationRoomCfg, generationRolloutCount);
        } finally {
            int completed = generationCompletedCount.incrementAndGet();
            int progressStep = Math.max(100, generationRequestedCount / 100);
            if (completed == generationRequestedCount || completed % progressStep == 0) {
                log.info("斗仙牌结果库生成进度 {}/{}，rollout={}",
                        completed, generationRequestedCount, generationRolloutCount);
            }
        }
    }

    /** 提交到独立单线程执行器，避免长时间模拟占用公共异步线程池。 */
    public boolean startGeneration(int count, int rolloutCount) {
        if (!localGenerationActive.compareAndSet(false, true)) {
            return false;
        }
        try {
            if (isGenerating()) {
                localGenerationActive.set(false);
                return false;
            }
        } catch (RuntimeException e) {
            generationState = "Redis不可用，未提交";
            localGenerationActive.set(false);
            log.error("查询斗仙牌结果库生成锁失败", e);
            return false;
        }
        generationRequestedCount = count;
        generationCompletedCount.set(0);
        generationRolloutCount = Math.max(1, Math.min(MAX_ROLLOUT_COUNT, rolloutCount));
        generationStartedAt = 0;
        generationFinishedAt = 0;
        generationState = "等待执行";
        try {
            generationExecutor.execute(() -> {
                generationStartedAt = System.currentTimeMillis();
                generationState = "生成中";
                try {
                    boolean success = super.generateCardLib(count);
                    generationState = success ? "生成完成" : "生成失败或被其他节点占用";
                } catch (Exception e) {
                    generationState = "生成异常：" + e.getClass().getSimpleName();
                    log.error("斗仙牌结果库生成任务异常", e);
                } finally {
                    generationFinishedAt = System.currentTimeMillis();
                    localGenerationActive.set(false);
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            generationState = "生成线程已关闭";
            localGenerationActive.set(false);
            return false;
        }
    }

    public void shutdownGenerationExecutor() {
        generationExecutor.shutdownNow();
    }

    /** 返回生成进度、当前双库指针及三个真人人数分区的容量。 */
    public String getGenerationStatus() {
        StringBuilder result = new StringBuilder(256).append("状态=").append(generationState);
        if (generationRequestedCount > 0) {
            result.append("，本节点进度=")
                    .append(Math.min(generationCompletedCount.get(), generationRequestedCount))
                    .append('/').append(generationRequestedCount)
                    .append("，rollout=").append(generationRolloutCount);
        }
        if (generationStartedAt > 0) {
            long end = generationFinishedAt > 0 ? generationFinishedAt : System.currentTimeMillis();
            result.append("，耗时=").append((end - generationStartedAt) / 1000).append("秒");
        }

        String configError = validatePoolResultsConfig();
        result.append("；配置=").append(configError == null ? "有效" : "无效：" + configError);
        try {
            boolean distributedGenerating = isGenerating();
            result.append("；Redis生成锁=").append(distributedGenerating ? "占用" : "空闲");
            appendRedisLibraryStatus(result);
        } catch (RuntimeException e) {
            result.append("；Redis状态查询失败：").append(e.getClass().getSimpleName());
            log.error("查询斗仙牌结果库状态失败", e);
        }
        return result.toString();
    }

    private void appendRedisLibraryStatus(StringBuilder result) {
        String activeLib = cardLibDao.getCurrentLibNameFromRedis();
        result.append("；当前库=").append(activeLib == null ? "无" : activeLib);
        long lastGenerateTime = cardLibDao.getLastGenerateTime();
        if (lastGenerateTime > 0) {
            result.append("，最近成功切库=").append(TIME_FORMATTER.format(Instant.ofEpochMilli(lastGenerateTime)));
        }
        if (activeLib == null || activeLib.isEmpty()) {
            return;
        }

        List<Integer> sectionKeys = getSortedSectionKeys();
        for (int realCount = 1; realCount <= 3; realCount++) {
            String partitionKey = "real-" + realCount;
            long total = 0;
            StringJoiner emptySections = new StringJoiner(",");
            for (int sectionKey : sectionKeys) {
                long size = cardLibDao.getCardLibSize(sectionKey, partitionKey);
                total += size;
                if (size == 0) {
                    emptySections.add(String.valueOf(sectionKey));
                }
            }
            result.append("；").append(partitionKey).append('=').append(total).append("条");
            if (emptySections.length() > 0) {
                result.append("，空桶[").append(emptySections).append(']');
            }
        }
    }

    /** 校验水池范围、收益桶和连续胜负权重，返回 null 表示有效。 */
    public String validatePoolResultsConfig() {
        List<PoolResultsCfg> cfgList = new ArrayList<>(getPoolResultsCfgListByGameType());
        if (cfgList.isEmpty()) {
            return "缺少gameType=" + getGameType() + "的配置";
        }
        cfgList.sort(Comparator.comparingInt(PoolResultsCfg::getEnterLimitMin));
        Set<Integer> modelIds = new HashSet<>();
        Set<Integer> expectedSectionKeys = null;
        Set<Integer> expectedStreakKeys = null;
        int previousMax = Integer.MIN_VALUE;
        boolean hasDefaultModel = false;
        for (int i = 0; i < cfgList.size(); i++) {
            PoolResultsCfg cfg = cfgList.get(i);
            if (!modelIds.add(cfg.getModelId())) {
                return "modelId重复：" + cfg.getModelId();
            }
            hasDefaultModel |= cfg.getModelId() == 4;
            if (cfg.getEnterLimitMin() >= cfg.getEnterLimitMax()) {
                return "modelId=" + cfg.getModelId() + "的进入范围无效";
            }
            if (i == 0 && cfg.getEnterLimitMin() > -999999) {
                return "水池范围缺少下限兜底";
            }
            if (i > 0 && cfg.getEnterLimitMin() != previousMax) {
                return "水池范围存在空档或重叠：" + previousMax + "到" + cfg.getEnterLimitMin();
            }
            previousMax = cfg.getEnterLimitMax();

            Map<Integer, Integer> baseWeights = cfg.getTypeProp();
            if (baseWeights == null || baseWeights.isEmpty()) {
                return "modelId=" + cfg.getModelId() + "的typeProp为空";
            }
            Set<Integer> sectionKeys = new HashSet<>(baseWeights.keySet());
            if (expectedSectionKeys == null) {
                expectedSectionKeys = sectionKeys;
            } else if (!expectedSectionKeys.equals(sectionKeys)) {
                return "modelId=" + cfg.getModelId() + "的收益桶边界与其他模型不一致";
            }
            if (!hasPositiveWeight(baseWeights)) {
                return "modelId=" + cfg.getModelId() + "的typeProp权重无效";
            }

            Map<Integer, Map<Integer, Integer>> streakWeights = cfg.getAddTypeProp();
            Set<Integer> streakKeys = streakWeights == null
                    ? Collections.emptySet() : new HashSet<>(streakWeights.keySet());
            if (expectedStreakKeys == null) {
                expectedStreakKeys = streakKeys;
            } else if (!expectedStreakKeys.equals(streakKeys)) {
                return "modelId=" + cfg.getModelId() + "的连续胜负档位不一致";
            }
            if (streakWeights != null) {
                for (Map.Entry<Integer, Map<Integer, Integer>> entry : streakWeights.entrySet()) {
                    Map<Integer, Integer> weights = entry.getValue();
                    if (weights == null || !sectionKeys.equals(weights.keySet()) || !hasPositiveWeight(weights)) {
                        return "modelId=" + cfg.getModelId() + "的连续档位" + entry.getKey() + "权重无效";
                    }
                }
            }
        }
        if (previousMax < 999999) {
            return "水池范围缺少上限兜底";
        }
        if (!hasDefaultModel) {
            return "缺少默认modelId=4";
        }
        return null;
    }

    private boolean hasPositiveWeight(Map<Integer, Integer> weights) {
        long total = 0;
        for (Integer weight : weights.values()) {
            if (weight == null || weight < 0) {
                return false;
            }
            total += weight;
        }
        return total > 0;
    }

    /**
     * 开局时按水池选择收益桶，并把结果库的32张牌排到牌堆顶部。
     * 余下20张只取补集并现场洗牌，因此结果库不会固定后续回合的发牌顺序。
     */
    public boolean tryApplyInitialDeal(DouXianGameController controller) {
        DouXianGameDataVo gameDataVo = controller.getGameDataVo();
        List<PlayerSeatInfo> activePlayers = gameDataVo.getPlayerSeatInfoList().stream()
                .filter(seat -> !seat.isDelState())
                .toList();
        if (activePlayers.size() != 4) {
            return false;
        }

        List<PlayerSeatInfo> realPlayers = new ArrayList<>();
        List<PlayerSeatInfo> robotPlayers = new ArrayList<>();
        for (PlayerSeatInfo seat : activePlayers) {
            if (gameDataVo.getGamePlayer(seat.getPlayerId()) instanceof GameRobotPlayer) {
                robotPlayers.add(seat);
            } else {
                realPlayers.add(seat);
            }
        }
        int realCount = realPlayers.size();
        if (realCount <= 0 || realCount >= 4 || robotPlayers.size() != 4 - realCount || !hasCardLib()) {
            return false;
        }

        PoolResultsCfg poolCfg = selectPoolResultsCfg(controller.getRoom().getRoomCfgId());
        if (poolCfg == null || poolCfg.getTypeProp() == null || poolCfg.getTypeProp().isEmpty()) {
            return false;
        }
        StreakAdjustment streakAdjustment = selectStreakAdjustment(realPlayers, poolCfg.getAddTypeProp());
        Map<Integer, Integer> effectiveWeights = applyStreakWeights(
                poolCfg.getTypeProp(), poolCfg.getAddTypeProp(), streakAdjustment.streakKey());
        int requestedSectionKey = selectSectionKey(effectiveWeights);
        SelectedCardLib selected = selectAvailableCardLib(
                requestedSectionKey, effectiveWeights, realCount);
        DouXianCardLib cardLib = selected == null ? null : selected.cardLib();
        Map<Integer, PokerCard> cardMap = PokerDataHelper.getCardListMap(gameDataVo.getPoolId());
        if (!DouXianCardLibGenerator.isValidInitialDeal(cardLib, cardMap, realCount)) {
            log.warn("斗仙牌结果库记录无效或对应场景为空 roomCfgId:{} realCount:{} sectionKey:{}",
                    controller.getRoom().getRoomCfgId(), realCount, requestedSectionKey);
            return false;
        }

        PlayerSeatInfo targetRealPlayer = null;
        if (streakAdjustment.playerId() != 0) {
            targetRealPlayer = realPlayers.stream()
                    .filter(player -> player.getPlayerId() == streakAdjustment.playerId())
                    .findFirst().orElse(null);
        }
        Collections.shuffle(robotPlayers);
        List<Integer> realSeatIndexes = new ArrayList<>(cardLib.getRealSeatIndexes());
        List<Integer> robotSeatIndexes = new ArrayList<>();
        Set<Integer> realSeatSet = new HashSet<>(realSeatIndexes);
        for (int seat = 0; seat < 4; seat++) {
            if (!realSeatSet.contains(seat)) {
                robotSeatIndexes.add(seat);
            }
        }
        Collections.shuffle(robotSeatIndexes);

        Map<Long, List<Integer>> assignedHands = new HashMap<>();
        if (targetRealPlayer != null) {
            Comparator<Integer> byExpectedProfit = Comparator.comparingLong(
                    index -> cardLib.getSeatExpectedMultipliers().get(index));
            int targetSeatIndex = (streakAdjustment.streakKey() < 0
                    ? realSeatIndexes.stream().max(byExpectedProfit)
                    : realSeatIndexes.stream().min(byExpectedProfit))
                    .orElseThrow();
            assignedHands.put(targetRealPlayer.getPlayerId(),
                    new ArrayList<>(cardLib.getSeatCards().get(targetSeatIndex)));
            realPlayers.remove(targetRealPlayer);
            realSeatIndexes.remove(Integer.valueOf(targetSeatIndex));
        }
        Collections.shuffle(realPlayers);
        Collections.shuffle(realSeatIndexes);
        for (int i = 0; i < realPlayers.size(); i++) {
            assignedHands.put(realPlayers.get(i).getPlayerId(),
                    new ArrayList<>(cardLib.getSeatCards().get(realSeatIndexes.get(i))));
        }
        for (int i = 0; i < robotPlayers.size(); i++) {
            assignedHands.put(robotPlayers.get(i).getPlayerId(),
                    new ArrayList<>(cardLib.getSeatCards().get(robotSeatIndexes.get(i))));
        }

        List<Integer> fixedCards = new ArrayList<>(32);
        List<Integer> orderedDeck = new ArrayList<>(52);
        for (Long playerId : gameDataVo.getActivePlayerIds()) {
            List<Integer> hand = assignedHands.get(playerId);
            if (hand == null || hand.size() != 8) {
                return false;
            }
            orderedDeck.addAll(hand);
            fixedCards.addAll(hand);
        }
        List<Integer> remainder = new ArrayList<>(cardMap.keySet());
        remainder.removeAll(fixedCards);
        if (remainder.size() != 20) {
            return false;
        }
        Collections.shuffle(remainder);
        orderedDeck.addAll(remainder);
        gameDataVo.setCards(orderedDeck);
        log.info("斗仙牌使用结果库首轮发牌 roomCfgId:{} realCount:{} modelId:{} sectionKey:{} "
                        + "expectedMultiplier:{} stdDev:{} rolloutCount:{} winRateBps:{} simulatorVersion:{} "
                        + "streakPlayerId:{} streak:{} streakKey:{}",
                controller.getRoom().getRoomCfgId(), realCount, poolCfg.getModelId(), selected.sectionKey(),
                cardLib.getMultiplier(), cardLib.getProfitStdDev(), cardLib.getRolloutCount(),
                cardLib.getWinRateBps(), cardLib.getSimulatorVersion(), streakAdjustment.playerId(),
                streakAdjustment.streak(), streakAdjustment.streakKey());
        return true;
    }

    /** 每回合按全部真人的实际净输赢更新池：真人净赢为负，真人净输为正。 */
    public void updatePoolBalance(DouXianGameDataVo gameDataVo, Map<Long, Long> roundChanges) {
        long realPlayerNet = 0;
        for (Map.Entry<Long, Long> entry : roundChanges.entrySet()) {
            if (!(gameDataVo.getGamePlayer(entry.getKey()) instanceof GameRobotPlayer)) {
                realPlayerNet += entry.getValue();
            }
        }
        if (realPlayerNet == 0) {
            return;
        }
        int roomCfgId = gameDataVo.getRoomCfg().getId();
        long poolChange = -realPlayerNet;
        long after = addPoolBalance(roomCfgId, poolChange);
        log.info("斗仙牌水池更新 roomCfgId:{} realPlayerNet:{} poolChange:{} after:{}",
                roomCfgId, realPlayerNet, poolChange, after);
    }

    /** 大结算后按整局净输赢更新真人连续胜负次数。 */
    public void updatePlayerWinStreak(DouXianGameDataVo gameDataVo) {
        for (Long playerId : gameDataVo.getGameStartBalance().keySet()) {
            if (gameDataVo.getGamePlayer(playerId) == null
                    || gameDataVo.getGamePlayer(playerId) instanceof GameRobotPlayer) {
                continue;
            }
            long gameChange = gameDataVo.getRoundChangeList().getOrDefault(playerId, List.of())
                    .stream().mapToLong(Long::longValue).sum();
            if (gameChange == 0) {
                continue;
            }
            int oldStreak = cardLibDao.getPlayerWinStreak(playerId);
            int newStreak = gameChange > 0
                    ? Math.max(0, oldStreak) + 1
                    : Math.min(0, oldStreak) - 1;
            cardLibDao.setPlayerWinStreak(playerId, newStreak);
            log.info("斗仙牌玩家连续胜负更新 playerId:{} gameChange:{} streak:{}->{}",
                    playerId, gameChange, oldStreak, newStreak);
        }
    }

    public int getPlayerWinStreak(long playerId) {
        return cardLibDao.getPlayerWinStreak(playerId);
    }

    public void setPlayerWinStreak(long playerId, int streak) {
        cardLibDao.setPlayerWinStreak(playerId, streak);
    }

    private int selectSectionKey(Map<Integer, Integer> configuredWeights) {
        Map<Integer, Integer> weights = new LinkedHashMap<>();
        List<Integer> keys = new ArrayList<>(configuredWeights.keySet());
        Collections.sort(keys);
        long totalWeight = 0;
        for (int key : keys) {
            int weight = Math.max(0, configuredWeights.getOrDefault(key, 0));
            weights.put(key, weight);
            totalWeight += weight;
        }
        if (totalWeight <= 0) {
            throw new IllegalStateException("斗仙牌结果库分区权重总和必须大于0");
        }
        long random = ThreadLocalRandom.current().nextLong(totalWeight);
        long cumulative = 0;
        for (Map.Entry<Integer, Integer> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (random < cumulative) {
                return entry.getKey();
            }
        }
        return keys.getLast();
    }

    private StreakAdjustment selectStreakAdjustment(List<PlayerSeatInfo> realPlayers,
                                                    Map<Integer, Map<Integer, Integer>> addTypeProp) {
        if (addTypeProp == null || addTypeProp.isEmpty()) {
            return StreakAdjustment.NONE;
        }
        List<Integer> positiveThresholds = addTypeProp.keySet().stream()
                .filter(key -> key > 0)
                .sorted(Comparator.reverseOrder())
                .toList();
        List<Integer> negativeThresholds = addTypeProp.keySet().stream()
                .filter(key -> key < 0)
                .sorted()
                .toList();

        if (!positiveThresholds.isEmpty()) {
            int minimumPositiveThreshold = positiveThresholds.getLast();
            long playerId = 0;
            int bestStreak = 0;
            for (PlayerSeatInfo player : realPlayers) {
                int streak = cardLibDao.getPlayerWinStreak(player.getPlayerId());
                if (streak >= minimumPositiveThreshold && streak > bestStreak) {
                    playerId = player.getPlayerId();
                    bestStreak = streak;
                }
            }
            if (playerId != 0) {
                for (int threshold : positiveThresholds) {
                    if (bestStreak >= threshold) {
                        return new StreakAdjustment(playerId, bestStreak, threshold);
                    }
                }
            }
        }

        if (!negativeThresholds.isEmpty()) {
            int maximumNegativeThreshold = negativeThresholds.getLast();
            long playerId = 0;
            int worstStreak = 0;
            for (PlayerSeatInfo player : realPlayers) {
                int streak = cardLibDao.getPlayerWinStreak(player.getPlayerId());
                if (streak <= maximumNegativeThreshold && streak < worstStreak) {
                    playerId = player.getPlayerId();
                    worstStreak = streak;
                }
            }
            if (playerId != 0) {
                for (int threshold : negativeThresholds) {
                    if (worstStreak <= threshold) {
                        return new StreakAdjustment(playerId, worstStreak, threshold);
                    }
                }
            }
        }
        return StreakAdjustment.NONE;
    }

    private Map<Integer, Integer> applyStreakWeights(
            Map<Integer, Integer> baseWeights,
            Map<Integer, Map<Integer, Integer>> addTypeProp,
            int streakKey) {
        Map<Integer, Integer> effective = new LinkedHashMap<>(baseWeights);
        if (streakKey == 0 || addTypeProp == null) {
            return effective;
        }
        Map<Integer, Integer> streakWeights = addTypeProp.get(streakKey);
        if (streakWeights == null) {
            return effective;
        }
        for (Map.Entry<Integer, Integer> entry : streakWeights.entrySet()) {
            if (effective.containsKey(entry.getKey())) {
                effective.put(entry.getKey(), entry.getValue());
            }
        }
        return effective;
    }

    /** 所选收益桶暂时为空时，按收益边界距离向相邻的非零权重桶降级。 */
    private SelectedCardLib selectAvailableCardLib(int requestedSectionKey,
                                                    Map<Integer, Integer> configuredWeights,
                                                    int realCount) {
        String partitionKey = "real-" + realCount;
        DouXianCardLib exact = sampleStableCardLib(requestedSectionKey, partitionKey);
        if (exact != null) {
            return new SelectedCardLib(requestedSectionKey, exact);
        }
        List<Integer> fallbackKeys = configuredWeights.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .filter(key -> key != requestedSectionKey)
                .sorted(Comparator.comparingLong(key -> Math.abs((long) key - requestedSectionKey)))
                .toList();
        for (int sectionKey : fallbackKeys) {
            DouXianCardLib fallback = sampleStableCardLib(sectionKey, partitionKey);
            if (fallback != null) {
                log.warn("斗仙牌结果库目标桶为空，降级到相邻桶 realCount:{} requested:{} actual:{}",
                        realCount, requestedSectionKey, sectionKey);
                return new SelectedCardLib(sectionKey, fallback);
            }
        }
        return null;
    }

    /** 同一收益桶随机抽少量候选，优先使用后续随机模拟标准差较低的记录。 */
    private DouXianCardLib sampleStableCardLib(int sectionKey, String partitionKey) {
        DouXianCardLib best = null;
        for (int i = 0; i < ONLINE_CANDIDATE_COUNT; i++) {
            DouXianCardLib candidate = cardLibDao.getCardLib(sectionKey, partitionKey);
            if (candidate != null && (best == null || candidate.getProfitStdDev() < best.getProfitStdDev())) {
                best = candidate;
            }
        }
        return best;
    }

    private record SelectedCardLib(int sectionKey, DouXianCardLib cardLib) {
    }

    private record StreakAdjustment(long playerId, int streak, int streakKey) {
        private static final StreakAdjustment NONE = new StreakAdjustment(0, 0, 0);
    }
}
