package com.jjg.game.poker.game.tosouthblood.gamephase;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.data.Card;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.gamephase.BaseStartGamePhase;
import com.jjg.game.poker.game.tosouthblood.cardlib.ToSouthBloodCardLib;
import com.jjg.game.poker.game.tosouthblood.cardlib.ToSouthBloodCardLibManager;
import com.jjg.game.poker.game.tosouthblood.data.ToSouthBloodDataHelper;
import com.jjg.game.poker.game.tosouthblood.data.ToSouthBloodSettlementContext;
import com.jjg.game.poker.game.tosouthblood.manager.ToSouthBloodStartManager;
import com.jjg.game.poker.game.tosouthblood.message.resp.RespToSouthBloodSendCardsInfo;
import com.jjg.game.poker.game.tosouthblood.room.ToSouthBloodGameController;
import com.jjg.game.poker.game.tosouthblood.room.data.ToSouthBloodGameDataVo;
import com.jjg.game.poker.game.tosouthblood.util.ToSouthBloodHandUtils;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PoolResultsCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.jjg.game.poker.game.tosouthblood.constant.ToSouthBloodConstant.*;

/**
 * 南方前进-血战开始游戏阶段 (洗牌发牌动画)
 */
public class ToSouthBloodStartGamePhase extends BaseStartGamePhase<ToSouthBloodGameDataVo> {
    
    /** 炸弹测试模式：true=每人发满手炸弹（用于验证炸弹结算逻辑），false=正常随机发牌 */
    private static final boolean BOMB_TEST_MODE = false;

    private ToSouthBloodSettlementContext instantWinContext;

    public ToSouthBloodStartGamePhase(AbstractPhaseGameController<Room_ChessCfg, ToSouthBloodGameDataVo> gameController, long executionGameId) {
        super(gameController, executionGameId);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        if (gameController instanceof ToSouthBloodGameController controller) {
            ToSouthBloodGameDataVo gameDataVo = controller.getGameDataVo();
            
            // 确保 playerSeatInfoList 已初始化
            if (gameDataVo.getPlayerSeatInfoList().isEmpty()) {
                controller.genPlayerSeatInfoList(gameDataVo.getSeatInfo(), gameDataVo.getPlayerSeatInfoList());
                log.info("初始化玩家列表完成，人数: {}", gameDataVo.getPlayerSeatInfoList().size());
            }
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(controller.getRoom().getRoomCfgId());
            gameDataVo.setRoomBet(warehouseCfg.getBetShow());
            log.debug("南方前进-血战开始游戏，房间底注为：{}", warehouseCfg.getBetShow());

            // 1. 洗牌发牌
            Map<Integer, PokerCard> cardListMap = ToSouthBloodDataHelper.getCardListMap(ToSouthBloodDataHelper.getPoolId(gameDataVo));
            if (BOMB_TEST_MODE) {
                log.info("炸弹测试模式已开启，跳过随机发牌，改为发炸弹牌");
                sendBombTestCards(cardListMap, gameDataVo);
            } else {
                sendCards(cardListMap, gameDataVo);
            }

            // 2. 确定首出玩家
            // 规则：同桌4人续局 → 上局赢家先出；有人变动或首局 → 黑桃3先出
            Set<Long> currentPlayerIds = gameDataVo.getPlayerSeatInfoList().stream()
                    .filter(s -> !s.isDelState())
                    .map(PlayerSeatInfo::getPlayerId)
                    .collect(Collectors.toSet());

            long lastWinner = gameDataVo.getLastGameWinnerPlayerId();
            Set<Long> lastPlayerIds = gameDataVo.getLastGamePlayerIds();

            boolean samePlayers = lastWinner > 0
                    && !lastPlayerIds.isEmpty()
                    && currentPlayerIds.equals(lastPlayerIds);

            PlayerSeatInfo firstPlayer;
            if (samePlayers) {
                // 同桌续局，上局赢家先出
                firstPlayer = gameDataVo.getPlayerSeatInfoMap().get(lastWinner);
                if (firstPlayer != null && !firstPlayer.isDelState()) {
                    gameDataVo.setIndex(firstPlayer.getSeatId());
                    gameDataVo.setRoundLeaderSeatId(firstPlayer.getSeatId());
                    gameDataVo.setFirstRound(false); // 非首局，无需出黑桃3
                    log.info("同桌续局，上局赢家 {} 先出", lastWinner);
                } else {
                    // 赢家异常，回退到黑桃3
                    firstPlayer = findSeatWithSpecifyCard(gameDataVo, cardListMap, RANK_3, SPADE_SUIT);
                    if (firstPlayer == null) {
                        log.warn("南方前进-血战牌组中没有黑桃3，请检查配置");
                        return;
                    }
                    gameDataVo.setIndex(firstPlayer.getSeatId());
                    gameDataVo.setRoundLeaderSeatId(firstPlayer.getSeatId());
                }
            } else {
                // 新桌或有人变动，黑桃3先出
                firstPlayer = findSeatWithSpecifyCard(gameDataVo, cardListMap, RANK_3, SPADE_SUIT);
                if (firstPlayer == null) {
                    log.warn("南方前进-血战牌组中没有黑桃3，请检查配置");
                    return;
                }
                gameDataVo.setIndex(firstPlayer.getSeatId());
                gameDataVo.setRoundLeaderSeatId(firstPlayer.getSeatId());
            }

            // 记录本局玩家集合，供下局判断是否同桌续局
            gameDataVo.setLastGamePlayerIds(currentPlayerIds);

            // 3. 检查通杀（炸弹测试模式下跳过，否则人人有炸弹把把触发通杀）
            if (!BOMB_TEST_MODE) {
                checkInstantWin(controller);
            }

            // 4. 将通杀上下文保存到 gameDataVo，供外部访问（如重连、日志等）
            gameDataVo.setInstantWinContext(instantWinContext);
        }
    }

    /** 牌库发牌总开关：true=走牌库权重抽牌, false=正常随机发牌（每人13张） */
    private static final boolean CARD_LIB_ENABLED = true;

    private void sendCards(Map<Integer, PokerCard> cardListMap, ToSouthBloodGameDataVo gameDataVo) {
        // ====== GM命令优先：有GM发牌命令时跳过牌库抽牌 ======
        boolean hasGmCommand = hasAnyGmCards(gameDataVo);
        if (CARD_LIB_ENABLED && !hasGmCommand) {
            // ====== 牌库抽牌检查 ======
            if (tryDealFromCardLib(cardListMap, gameDataVo)) {
                return; // 牌库发牌成功，跳过正常发牌
            }
        } else if (hasGmCommand) {
            log.info("[牌库] 检测到GM发牌命令，跳过牌库抽牌");
        } else {
            log.info("[牌库] 牌库发牌开关已关闭(CARD_LIB_ENABLED=false)，使用正常随机发牌");
        }

        List<Integer> list = new ArrayList<>(cardListMap.keySet());

        // ====== GM发牌处理：预分配GM指定的手牌，并从牌池中移除 ======
        Map<Long, List<Integer>> gmPreAssigned = new HashMap<>();

        // 收集同桌的机器人列表（按座位顺序），用于解析机器人编号
        List<PlayerSeatInfo> robotSeats = new ArrayList<>();
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (!info.isDelState() && RobotUtil.isRobot(info.getPlayerId())) {
                robotSeats.add(info);
            }
        }

        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.isDelState()) {
                continue;
            }

            // 处理玩家自身的GM手牌
            List<int[]> gmCards = ToSouthBloodStartManager.consumeGmCards(info.getPlayerId());
            if (gmCards != null && !gmCards.isEmpty()) {
                List<Integer> preAssignedIds = resolveGmCards(cardListMap, gmCards, list, info.getPlayerId());
                if (!preAssignedIds.isEmpty()) {
                    gmPreAssigned.put(info.getPlayerId(), preAssignedIds);
                    log.info("GM发牌 - 玩家: {}, 预分配手牌数: {}", info.getPlayerId(), preAssignedIds.size());
                }
            }

            // 处理该玩家发起的机器人GM手牌
            Map<Integer, List<int[]>> robotCardsMap = ToSouthBloodStartManager.consumeGmRobotCards(info.getPlayerId());
            if (robotCardsMap != null && !robotCardsMap.isEmpty()) {
                for (Map.Entry<Integer, List<int[]>> entry : robotCardsMap.entrySet()) {
                    int robotIndex = entry.getKey(); // 1-based
                    List<int[]> robotCards = entry.getValue();
                    if (robotIndex < 1 || robotIndex > robotSeats.size()) {
                        log.warn("GM机器人发牌 - 无效的机器人编号: {}, 当前机器人数量: {}", robotIndex, robotSeats.size());
                        continue;
                    }
                    PlayerSeatInfo robotSeat = robotSeats.get(robotIndex - 1);
                    List<Integer> preAssignedIds = resolveGmCards(cardListMap, robotCards, list, robotSeat.getPlayerId());
                    if (!preAssignedIds.isEmpty()) {
                        // 合并到已有的预分配（同一个机器人可能被多次指定）
                        gmPreAssigned.merge(robotSeat.getPlayerId(), preAssignedIds, (old, add) -> {
                            old.addAll(add);
                            return old;
                        });
                        log.info("GM机器人发牌 - 机器人编号: {}, 玩家: {}, 预分配手牌数: {}",
                                robotIndex, robotSeat.getPlayerId(), preAssignedIds.size());
                    }
                }
            }
        }
        // ====== GM发牌处理结束 ======

        Collections.shuffle(list);
        gameDataVo.setCards(list);

        List<Integer> cards = gameDataVo.getCards();
        int handPoker = gameDataVo.getRoomCfg().getHandPoker();
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.isDelState()) {
                continue;
            }

            List<Integer> playCard = new ArrayList<>();

            // GM预分配的手牌优先加入
            List<Integer> preAssigned = gmPreAssigned.get(info.getPlayerId());
            if (preAssigned != null) {
                playCard.addAll(preAssigned);
            }

            // 剩余手牌从洗好的牌堆中补全
            int remaining = handPoker - playCard.size();
            for (int i = 0; i < remaining; i++) {
                if (!cards.isEmpty()) {
                    playCard.add(cards.removeFirst());
                }
            }

            List<Card> handCards = new ArrayList<>();
            for (Integer id : playCard) {
                handCards.add(cardListMap.get(id));
            }
            // 按照 炸弹 > 连对 > 顺子 > 三条 > 对子 > 单张 的规则排列手牌，并计算高亮牌(2,炸弹，连对)
            List<Integer> highlightIds = ToSouthBloodHandUtils.sortAndGetHighlightCards(handCards);

            if (!highlightIds.isEmpty()) {
                gameDataVo.getPlayerHighlightCards().put(info.getPlayerId(), highlightIds);
            }

            playCard.clear();
            List<Integer> sortedHandCards = new ArrayList<>();
            for (Card c : handCards) {
                if (c instanceof PokerCard pc) {
                    playCard.add(pc.getPokerPoolId());
                    sortedHandCards.add(pc.getClientId());
                }
            }

            info.setCards(new ArrayList<>());
            info.getCards().add(playCard);

            if (log.isDebugEnabled()) {
                 // 此时 playCard 已经排序
                log.debug("发牌 - 玩家: {}, 座位: {}, 手牌: {}{}", info.getPlayerId(), info.getSeatId(),
                        gmPreAssigned.containsKey(info.getPlayerId()) ? "[GM] " : "",
                        ToSouthBloodHandUtils.cardListToString(handCards));
            }
            // 记录发牌到一局日志
            gameDataVo.getGameLog().recordDeal(info.getPlayerId(), info.getSeatId(),
                    ToSouthBloodHandUtils.cardListToString(handCards));

            RespToSouthBloodSendCardsInfo sendCardsInfo = new RespToSouthBloodSendCardsInfo();
            sendCardsInfo.sortedHandCards = sortedHandCards;
            // 简单打乱处理
            List<Integer> temp = new ArrayList<>(sortedHandCards);
            Collections.shuffle(temp);
            sendCardsInfo.originalHandCards = temp;
            sendCardsInfo.highlightCards = highlightIds;
            gameController.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(info.getPlayerId(), sendCardsInfo));
        }
    }

    /**
     * 尝试从牌库中发牌（基于PoolResultsCfg权重配置）
     *
     * 流程:
     * 1. 检查牌库是否存在、PoolResultsCfg配置是否存在
     * 2. 收集所有真人玩家的连赢/连输值
     * 3. 优先找连赢玩家（streak最大），其次找连输玩家（streak最小）
     * 4. 匹配addTypeProp中的streak key（一次只能修改一组，一桌只修改一次）
     * 5. 用addTypeProp的delta修改typeProp的基础权重
     * 6. 按修改后的权重随机选取一个分区(sectionKey)
     * 7. 从Redis该分区随机抽取牌库条目
     * 8. playerCards发给触发修改的真人玩家，robotCards发给其余3人
     *
     * @return true=牌库发牌成功, false=回退到正常发牌
     */
    private boolean tryDealFromCardLib(Map<Integer, PokerCard> cardListMap, ToSouthBloodGameDataVo gameDataVo) {
        if (!(gameController instanceof ToSouthBloodGameController controller)) {
            return false;
        }

        // 获取 ToSouthBloodCardLibManager
        ToSouthBloodCardLibManager cardLibManager = CommonUtil.getContext().getBean(ToSouthBloodCardLibManager.class);

        if (!cardLibManager.hasCardLib()) {
            return false;
        }

        // 收集活跃的玩家列表
        List<PlayerSeatInfo> activePlayers = gameDataVo.getPlayerSeatInfoList().stream()
                .filter(p -> !p.isDelState())
                .collect(Collectors.toList());

        // 需要恰好4人才能使用牌库
        if (activePlayers.size() != 4) {
            return false;
        }

        Map<Long, Integer> streakMap = gameDataVo.getPlayerWinStreakMap();

        // ====== 先用任意一条PoolResultsCfg提取addTypeProp的streak key ======
        List<PoolResultsCfg> allPoolCfgList = cardLibManager.getPoolResultsCfgListByGameType();
        if (allPoolCfgList.isEmpty()) {
            return false;
        }
        Map<Integer, Map<Integer, Integer>> addTypeProp = allPoolCfgList.getFirst().getAddTypeProp();

        // 收集所有addTypeProp中的streak key（排除0，因为0是基础偏差）
        List<Integer> positiveStreakKeys = new ArrayList<>(); // 连赢阈值
        List<Integer> negativeStreakKeys = new ArrayList<>(); // 连输阈值
        if (addTypeProp != null) {
            for (int key : addTypeProp.keySet()) {
                if (key > 0) positiveStreakKeys.add(key);
                else if (key < 0) negativeStreakKeys.add(key);
            }
        }
        // 正数key从大到小排序（优先匹配高阈值）
        positiveStreakKeys.sort(Collections.reverseOrder());
        // 负数key从小到大排序（优先匹配低阈值，即绝对值最大的）
        Collections.sort(negativeStreakKeys);

        // 最小触发阈值
        int minPositiveThreshold = positiveStreakKeys.isEmpty() ? Integer.MAX_VALUE : positiveStreakKeys.getLast();
        int maxNegativeThreshold = negativeStreakKeys.isEmpty() ? Integer.MIN_VALUE : negativeStreakKeys.getLast();

        PlayerSeatInfo targetPlayer = null;
        int matchedStreakKey = 0;

        // 优先找连赢的真人玩家（streak >= 最小正阈值）
        int bestPositiveStreak = 0;
        for (PlayerSeatInfo seat : activePlayers) {
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(seat.getPlayerId());
            if (gamePlayer instanceof GameRobotPlayer) continue;

            int streak = streakMap.getOrDefault(seat.getPlayerId(), 0);
            if (streak >= minPositiveThreshold && streak > bestPositiveStreak) {
                bestPositiveStreak = streak;
                targetPlayer = seat;
            }
        }

        if (targetPlayer != null) {
            // 匹配streakKey: 从大到小找第一个 <= streak的key
            for (int key : positiveStreakKeys) {
                if (bestPositiveStreak >= key) {
                    matchedStreakKey = key;
                    break;
                }
            }
        }

        // 其次找连输的真人玩家（streak <= 最大负阈值）
        if (targetPlayer == null) {
            int bestNegativeStreak = 0;
            for (PlayerSeatInfo seat : activePlayers) {
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(seat.getPlayerId());
                if (gamePlayer instanceof GameRobotPlayer) continue;

                int streak = streakMap.getOrDefault(seat.getPlayerId(), 0);
                if (streak <= maxNegativeThreshold && streak < bestNegativeStreak) {
                    bestNegativeStreak = streak;
                    targetPlayer = seat;
                }
            }

            if (targetPlayer != null) {
                // 匹配streakKey: 从小到大找第一个 >= streak的key（注意都是负数）
                for (int key : negativeStreakKeys) {
                    if (bestNegativeStreak <= key) {
                        matchedStreakKey = key;
                        break;
                    }
                }
            }
        }

        // 没有连赢/连输达到阈值的玩家 → 取第一个真人玩家，matchedStreakKey=0（使用基础权重偏差）
        if (targetPlayer == null) {
            for (PlayerSeatInfo seat : activePlayers) {
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(seat.getPlayerId());
                if (!(gamePlayer instanceof GameRobotPlayer)) {
                    targetPlayer = seat;
                    break;
                }
            }
        }

        // 全是机器人，无需牌库控制
        if (targetPlayer == null) {
            return false;
        }

        // ====== 根据水池偏差选择对应的PoolResultsCfg模型（参考slots水池控制） ======
        int roomCfgId = controller.getRoom().getRoomCfgId();
        PoolResultsCfg poolCfg = cardLibManager.selectPoolResultsCfg(roomCfgId);
        if (poolCfg == null || poolCfg.getTypeProp() == null || poolCfg.getTypeProp().isEmpty()) {
            return false;
        }

        // ====== 计算权重 ======
        Set<Integer> validSectionKeys = poolCfg.getTypeProp().keySet();
        Map<Integer, Integer> baseWeights = new LinkedHashMap<>(poolCfg.getTypeProp());
        // 触发连赢/连输时，直接使用 addTypeProp 中的权重替换基础权重（不再加减）
        addTypeProp = poolCfg.getAddTypeProp();
        if (matchedStreakKey != 0 && addTypeProp != null) {
            Map<Integer, Integer> streakWeights = addTypeProp.get(matchedStreakKey);
            if (streakWeights != null) {
                for (Map.Entry<Integer, Integer> entry : streakWeights.entrySet()) {
                    int sectionKey = entry.getKey();
                    int newWeight = entry.getValue();
                    if (validSectionKeys.contains(sectionKey)) {
                        baseWeights.put(sectionKey, newWeight); // 直接赋值，不做加减
                    }
                }
            }
        }
        // matchedStreakKey==0 时，使用 typeProp 基础权重，不做任何修改

        // 排序分区key并clamp权重到>=0
        List<Integer> sortedSectionKeys = new ArrayList<>(baseWeights.keySet());
        Collections.sort(sortedSectionKeys);

        // 计算总权重（权重<=0的分区不参与随机）
        long totalWeight = 0;
        for (int key : sortedSectionKeys) {
            int weight = Math.max(0, baseWeights.get(key));
            baseWeights.put(key, weight);
            totalWeight += weight;
        }

        if (totalWeight <= 0) {
            log.warn("[牌库] 权重总和为0，回退正常发牌 streakKey={}", matchedStreakKey);
            return false;
        }

        // ====== 按权重随机选取分区 ======
        long rand = ThreadLocalRandom.current().nextLong(totalWeight);
        int selectedSectionKey = sortedSectionKeys.getLast(); // 默认最后一个
        long cumulative = 0;
        for (int key : sortedSectionKeys) {
            cumulative += baseWeights.get(key);
            if (rand < cumulative) {
                selectedSectionKey = key;
                break;
            }
        }

        // ====== 从Redis抽取牌库 ======
        ToSouthBloodCardLib cardLib = cardLibManager.getCardLib(selectedSectionKey);
        if (cardLib == null) {
            log.info("[牌库] 分区{}牌库为空，回退正常发牌", selectedSectionKey);
            return false;
        }

        // 验证牌库数据有效性
        if (cardLib.getPlayerCards() == null || cardLib.getPlayerCards().size() != 13
                || cardLib.getRobotCards() == null || cardLib.getRobotCards().size() != 3) {
            log.warn("[牌库] 牌库数据无效，回退正常发牌");
            return false;
        }

        // 验证牌库中的牌ID在当前牌池中都存在
        for (int cardId : cardLib.getPlayerCards()) {
            if (!cardListMap.containsKey(cardId)) {
                log.warn("[牌库] 牌库中的牌ID不在当前牌池中 cardId={}, 回退正常发牌", cardId);
                return false;
            }
        }
        for (List<Integer> robotHand : cardLib.getRobotCards()) {
            for (int cardId : robotHand) {
                if (!cardListMap.containsKey(cardId)) {
                    log.warn("[牌库] 牌库中的牌ID不在当前牌池中 cardId={}, 回退正常发牌", cardId);
                    return false;
                }
            }
        }

        int playerStreak = streakMap.getOrDefault(targetPlayer.getPlayerId(), 0);
        long poolDiff = cardLibManager.getPoolDiff(roomCfgId);
        log.info("[牌库] 触发牌库发牌 玩家={}, streak={}, streakKey={}, poolDiff={}, modelId={}, 选中分区={}, 倍数={}, 权重总和={}",
                targetPlayer.getPlayerId(), playerStreak, matchedStreakKey,
                poolDiff, poolCfg.getModelId(),
                selectedSectionKey, cardLib.getMultiplier(), totalWeight);

        // ====== 分配手牌：目标玩家拿playerCards，其余3人拿robotCards ======
        int robotIdx = 0;
        int handPoker = gameDataVo.getRoomCfg().getHandPoker();
        gameDataVo.setCards(new ArrayList<>()); // 牌库发牌后牌堆为空

        for (PlayerSeatInfo info : activePlayers) {
            List<Integer> playCard;
            if (info.getPlayerId() == targetPlayer.getPlayerId()) {
                playCard = new ArrayList<>(cardLib.getPlayerCards());
            } else {
                if (robotIdx < cardLib.getRobotCards().size()) {
                    playCard = new ArrayList<>(cardLib.getRobotCards().get(robotIdx++));
                } else {
                    log.warn("[牌库] 机器人牌不足，回退正常发牌");
                    return false;
                }
            }

            // 截取到handPoker张（正常应该是13张）
            if (playCard.size() > handPoker) {
                playCard = playCard.subList(0, handPoker);
            }

            List<Card> handCards = new ArrayList<>();
            for (Integer id : playCard) {
                handCards.add(cardListMap.get(id));
            }

            // 排序和高亮
            List<Integer> highlightIds = ToSouthBloodHandUtils.sortAndGetHighlightCards(handCards);
            if (!highlightIds.isEmpty()) {
                gameDataVo.getPlayerHighlightCards().put(info.getPlayerId(), highlightIds);
            }

            playCard.clear();
            List<Integer> sortedHandCards = new ArrayList<>();
            for (Card c : handCards) {
                if (c instanceof PokerCard pc) {
                    playCard.add(pc.getPokerPoolId());
                    sortedHandCards.add(pc.getClientId());
                }
            }

            info.setCards(new ArrayList<>());
            info.getCards().add(playCard);

            log.debug("[牌库] 发牌 - 玩家: {}, 座位: {}, 手牌: {}", info.getPlayerId(), info.getSeatId(),
                    ToSouthBloodHandUtils.cardListToString(handCards));

            // 记录发牌到一局日志
            gameDataVo.getGameLog().recordDeal(info.getPlayerId(), info.getSeatId(),
                    "[Card Library] " + ToSouthBloodHandUtils.cardListToString(handCards));

            RespToSouthBloodSendCardsInfo sendCardsInfo = new RespToSouthBloodSendCardsInfo();
            sendCardsInfo.sortedHandCards = sortedHandCards;
            List<Integer> temp = new ArrayList<>(sortedHandCards);
            Collections.shuffle(temp);
            sendCardsInfo.originalHandCards = temp;
            sendCardsInfo.highlightCards = highlightIds;
            gameController.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(info.getPlayerId(), sendCardsInfo));
        }

        return true;
    }

    /**
     * 将GM指定的手牌(suit+rank)解析为实际的pokerPoolId列表，并从可用牌池中移除
     *
     * @param cardListMap  牌池映射
     * @param gmCards      GM指定的手牌列表 int[]{suit, rank}
     * @param availableIds 当前可用的牌ID列表（会被修改）
     * @param playerId     目标玩家ID（用于日志）
     * @return 匹配到的pokerPoolId列表
     */
    private List<Integer> resolveGmCards(Map<Integer, PokerCard> cardListMap, List<int[]> gmCards,
                                         List<Integer> availableIds, long playerId) {
        List<Integer> preAssignedIds = new ArrayList<>();
        for (int[] spec : gmCards) {
            int suit = spec[0];
            int rank = spec[1];
            Integer matchedId = findCardIdBySuitRank(cardListMap, suit, rank, availableIds);
            if (matchedId != null) {
                preAssignedIds.add(matchedId);
                availableIds.remove(matchedId);
            } else {
                log.warn("GM发牌 - 玩家: {}, 未找到匹配的牌: suit={}, rank={}", playerId, suit, rank);
            }
        }
        return preAssignedIds;
    }

    /**
     * 根据花色和点数从牌池中查找匹配的牌ID (pokerPoolId)
     *
     * @param cardListMap  牌池映射
     * @param suit         花色
     * @param rank         点数
     * @param availableIds 当前可用的牌ID列表
     * @return 匹配的pokerPoolId，未找到返回null
     */
    private Integer findCardIdBySuitRank(Map<Integer, PokerCard> cardListMap, int suit, int rank, List<Integer> availableIds) {
        for (Integer id : availableIds) {
            PokerCard card = cardListMap.get(id);
            if (card != null && card.getSuit() == suit && card.getRank() == rank) {
                return id;
            }
        }
        return null;
    }

    /**
     * 检查当前牌局是否有任何GM发牌命令（dealCards 或 dealRobotCards）
     * 用于判断是否跳过牌库抽牌
     */
    private boolean hasAnyGmCards(ToSouthBloodGameDataVo gameDataVo) {
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.isDelState()) continue;
            long playerId = info.getPlayerId();
            if (ToSouthBloodStartManager.hasGmCards(playerId) || ToSouthBloodStartManager.hasGmRobotCards(playerId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 炸弹测试模式发牌：
     * 将3~A各花色组成的四张同点炸弹按轮次分配给玩家，剩余手牌位置用2填充。
     * 以4人桌为例：每人获得3个炸弹(12张) + 1张2 = 13张。
     */
    private void sendBombTestCards(Map<Integer, PokerCard> cardListMap, ToSouthBloodGameDataVo gameDataVo) {
        // 按点数分组，2单独收集作为填充牌
        Map<Integer, List<PokerCard>> rankGroups = new HashMap<>();
        List<PokerCard> deuces = new ArrayList<>();
        for (PokerCard card : cardListMap.values()) {
            if (card.getRank() == RANK_2) {
                deuces.add(card);
            } else {
                rankGroups.computeIfAbsent(card.getRank(), k -> new ArrayList<>()).add(card);
            }
        }

        // 收集完整的四张同点炸弹组，按点数升序排列保证稳定性（3最小优先分配）
        List<List<PokerCard>> bombGroups = rankGroups.entrySet().stream()
                .filter(e -> e.getValue().size() >= 4)
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ArrayList<>(e.getValue().subList(0, 4)))
                .collect(Collectors.toList());

        List<PlayerSeatInfo> activePlayers = gameDataVo.getPlayerSeatInfoList().stream()
                .filter(p -> !p.isDelState())
                .collect(Collectors.toList());
        int playerCount = activePlayers.size();
        int handPoker = gameDataVo.getRoomCfg().getHandPoker();

        // 炸弹组按轮次分配给玩家（手牌未满才追加）
        Map<Long, List<PokerCard>> playerCardMap = new HashMap<>();
        for (PlayerSeatInfo info : activePlayers) {
            playerCardMap.put(info.getPlayerId(), new ArrayList<>());
        }
        for (int i = 0; i < bombGroups.size(); i++) {
            PlayerSeatInfo player = activePlayers.get(i % playerCount);
            List<PokerCard> hand = playerCardMap.get(player.getPlayerId());
            if (hand.size() + 4 <= handPoker) {
                hand.addAll(bombGroups.get(i));
            }
        }

        // 剩余位置用2补齐
        int deuceIdx = 0;
        for (PlayerSeatInfo info : activePlayers) {
            List<PokerCard> hand = playerCardMap.get(info.getPlayerId());
            while (hand.size() < handPoker && deuceIdx < deuces.size()) {
                hand.add(deuces.get(deuceIdx++));
            }
        }

        // 炸弹测试后牌堆为空
        gameDataVo.setCards(new ArrayList<>());

        // 与 sendCards 保持相同的发牌协议
        for (PlayerSeatInfo info : activePlayers) {
            List<Card> handCards = new ArrayList<>(playerCardMap.get(info.getPlayerId()));
            // 按规则排列手牌并计算高亮牌
            List<Integer> highlightIds = ToSouthBloodHandUtils.sortAndGetHighlightCards(handCards);
            if (!highlightIds.isEmpty()) {
                gameDataVo.getPlayerHighlightCards().put(info.getPlayerId(), highlightIds);
            }

            List<Integer> playCard = new ArrayList<>();
            List<Integer> sortedHandCards = new ArrayList<>();
            for (Card c : handCards) {
                if (c instanceof PokerCard pc) {
                    playCard.add(pc.getPokerPoolId());
                    sortedHandCards.add(pc.getClientId());
                }
            }

            info.setCards(new ArrayList<>());
            info.getCards().add(playCard);

            if (log.isDebugEnabled()) {
                log.debug("炸弹测试发牌 - 玩家: {}, 座位: {}, 手牌: {}", info.getPlayerId(), info.getSeatId(),
                        ToSouthBloodHandUtils.cardListToString(handCards));
            }
            // 记录发牌到一局日志
            gameDataVo.getGameLog().recordDeal(info.getPlayerId(), info.getSeatId(),
                    ToSouthBloodHandUtils.cardListToString(handCards));

            RespToSouthBloodSendCardsInfo sendCardsInfo = new RespToSouthBloodSendCardsInfo();
            sendCardsInfo.sortedHandCards = sortedHandCards;
            List<Integer> temp = new ArrayList<>(sortedHandCards);
            Collections.shuffle(temp);
            sendCardsInfo.originalHandCards = temp;
            sendCardsInfo.highlightCards = highlightIds;
            gameController.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(info.getPlayerId(), sendCardsInfo));
        }
    }

    private PlayerSeatInfo findSeatWithSpecifyCard(ToSouthBloodGameDataVo gameDataVo, Map<Integer, PokerCard> cardMap, int rank, int suit) {
        for (PlayerSeatInfo playerSeatInfo : gameDataVo.getPlayerSeatInfoList()) {
            for (Integer cardId : playerSeatInfo.getCurrentCards()) {
                PokerCard pokerCard = cardMap.get(cardId);
                if (pokerCard != null && pokerCard.getRank() == rank && pokerCard.getSuit() == suit) {
                    return playerSeatInfo;
                }
            }
        }
        return null;
    }

    private void checkInstantWin(ToSouthBloodGameController controller) {
        ToSouthBloodGameDataVo gameDataVo = controller.getGameDataVo();
        Map<Integer, PokerCard> cardMap = ToSouthBloodDataHelper.getCardListMap(ToSouthBloodDataHelper.getPoolId(gameDataVo));

        // key: PlayerSeatInfo, value: 该玩家自己的通杀牌型信息
        Map<PlayerSeatInfo, Pair<Integer, List<Integer>>> winnerMap = new LinkedHashMap<>();

        for (PlayerSeatInfo seatInfo : gameDataVo.getPlayerSeatInfoList()) {
            List<Integer> handCardIds = seatInfo.getCurrentCards();
            List<Card> handCards = handCardIds.stream().map(cardMap::get).collect(Collectors.toList());
            if (log.isDebugEnabled()) {
                List<Card> sorted = new ArrayList<>(handCards);
                sorted.sort(ToSouthBloodHandUtils.CARD_COMPARATOR);
                log.debug("通杀检查 - 玩家: {}, 手牌: {}", seatInfo.getPlayerId(), ToSouthBloodHandUtils.cardListToString(sorted));
            }
            Pair<Integer, List<Integer>> instantWinCards = ToSouthBloodHandUtils.getInstantWinCards(handCards);
            if (instantWinCards != null) {
                winnerMap.put(seatInfo, instantWinCards);
                log.info("玩家 {} 触发通杀！类型: {}", seatInfo.getPlayerId(), instantWinCards.getFirst());
                // 记录通杀到一局日志
                List<Card> sorted = new ArrayList<>(handCards);
                sorted.sort(ToSouthBloodHandUtils.CARD_COMPARATOR);
                gameDataVo.getGameLog().recordInstantWinSettlement(seatInfo.getPlayerId(),
                        instantWinCards.getFirst(), ToSouthBloodHandUtils.cardListToString(sorted));
            }
        }

        if (!winnerMap.isEmpty()) {
            instantWinContext = new ToSouthBloodSettlementContext();
            instantWinContext.setInstantWin(true);
            for (Map.Entry<PlayerSeatInfo, Pair<Integer, List<Integer>>> entry : winnerMap.entrySet()) {
                Pair<Integer, List<Integer>> winCards = entry.getValue();
                instantWinContext.addItem(new ToSouthBloodSettlementContext.SettlementItem(
                        entry.getKey(),
                        true,
                        winCards.getFirst(),
                        winCards.getSecond()
                ));
            }
        }
    }

    @Override
    public void nextPhase() {
        if (gameController instanceof ToSouthBloodGameController controller) {
            if (instantWinContext != null && !instantWinContext.getWinners().isEmpty()) {
                // 通杀，直接进入结算
                controller.addPokerPhaseTimer(new ToSouthBloodSettlementPhase(controller, instantWinContext));
            } else {
                // 进入打牌阶段
                controller.addPokerPhaseTimer(new ToSouthBloodPlayCardPhase(controller));
            }
        }
    }
}
