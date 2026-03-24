package com.jjg.game.poker.game.tosouth.room;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.EGameType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerPhaseChange;
import com.jjg.game.poker.game.common.message.req.ReqPokerBet;
import com.jjg.game.poker.game.common.message.req.ReqPokerSampleCardOperation;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.tosouth.constant.ToSouthConstant;
import com.jjg.game.poker.game.tosouth.data.ToSouthSettlementContext;
import com.jjg.game.poker.game.tosouth.gamephase.ToSouthSettlementPhase;
import com.jjg.game.poker.game.tosouth.gamephase.ToSouthStartGamePhase;
import com.jjg.game.poker.game.tosouth.message.bean.*;
import com.jjg.game.poker.game.tosouth.message.req.ReqToSouthGoReady;
import com.jjg.game.poker.game.tosouth.message.req.ReqTurnAction;
import com.jjg.game.poker.game.tosouth.message.resp.RespToSouthChangTable;
import com.jjg.game.poker.game.tosouth.message.resp.RespToSouthRoomBaseInfo;
import com.jjg.game.poker.game.tosouth.message.resp.RespToSouthSendCardsInfo;
import com.jjg.game.poker.game.tosouth.message.notify.NotifyToSouthTurnActionInfo;
import com.jjg.game.poker.game.tosouth.room.data.ToSouthGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;

import com.jjg.game.poker.game.tosouth.message.notify.NotifyToSouthBombSettlement;
import com.jjg.game.poker.game.tosouth.message.notify.NotifyToSouthPlayerReady;
import com.jjg.game.poker.game.tosouth.room.data.ToSouthRoundRecord;

import java.util.*;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.tosouth.data.ToSouthDataHelper;
import com.jjg.game.poker.game.tosouth.util.ToSouthCardType;
import com.jjg.game.sampledata.bean.SouthernMoneyCfg;
import com.jjg.game.poker.game.tosouth.util.ToSouthHandUtils;
import java.util.stream.Collectors;

import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.timer.RoomEventType;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.core.pb.NotifyExitRoom;
import com.jjg.game.poker.game.tosouth.autohandler.ToSouthAutoPlayHandler;
import com.jjg.game.poker.game.tosouth.autohandler.ToSouthReadyTimeoutHandler;

import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.DIAMOND_SUIT;
import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.HEART_SUIT;
import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.RANK_2;
import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.RANK_3;
import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.SPADE_SUIT;

@GameController(gameType = EGameType.TO_SOUTH, roomType = RoomType.POKER_ROOM)
public class ToSouthGameController extends BasePokerGameController<ToSouthGameDataVo> {

    /** 准备倒计时（毫秒） */
    private static final int READY_TIMEOUT = 11000;

    public ToSouthGameController(AbstractRoomController<Room_ChessCfg, ? extends Room> roomController) {
        super(roomController);
    }

    @Override
    public void startNextRoundOrSettlement() {

    }

    @Override
    public PlayerSeatInfo getNextExePlayer() {
        List<PlayerSeatInfo> playerSeatInfoList = gameDataVo.getPlayerSeatInfoList();
        if (playerSeatInfoList.isEmpty()) {
            return null;
        }
        int seatId = gameDataVo.getIndex();
        // 先找到当前玩家在列表中的真实下标（不能直接把 seatId 当 list index）
        int currentListIdx = -1;
        for (int i = 0; i < playerSeatInfoList.size(); i++) {
            if (playerSeatInfoList.get(i).getSeatId() == seatId) {
                currentListIdx = i;
                break;
            }
        }
        if (currentListIdx == -1) return null;
        // 从当前下标后一位开始轮询（环形）
        for (int i = 1; i < playerSeatInfoList.size(); i++) {
            int newIndex = (currentListIdx + i) % playerSeatInfoList.size();
            PlayerSeatInfo info = playerSeatInfoList.get(newIndex);
            if (!info.isOver() && !info.isDelState()
                    && !gameDataVo.getCurRoundPassedPlayerSeats().contains(info.getSeatId())) {
                return info;
            }
        }
        return null;
    }

    @Override
    public void sampleCardOperation(long playerId, ReqPokerSampleCardOperation req) {

    }

    /**
     * 换桌
     */
    public void reqChangeTable(PlayerController playerController, ToSouthGameController controller) {
        AbstractRoomController<Room_ChessCfg, ? extends Room> abstractRoomController = controller.getRoomController();
        Room room = abstractRoomController.getRoom();
        boolean changed =
                roomController.getRoomManager().changeRoom(
                        playerController, room, room.getGameType(), controller.getRoom().getRoomCfgId(), controller.getRoom().getMaxLimit());
        RespToSouthChangTable res = new RespToSouthChangTable(changed ? Code.SUCCESS : Code.FAIL);
        playerController.send(res);
    }

    @Override
    public boolean canJoinRobot() {
        return getCurrentGamePhase() == EGamePhase.WAIT_READY;
    }

    public void turnAction(long playerId, ReqTurnAction reqTurnAction) {
        PlayerSeatInfo info = gameDataVo.getCurrentPlayerSeatInfo();
        if (getCurrentGamePhase() != EGamePhase.PLAY_CART) {
            log.warn("当前不在出牌阶段");
            return;
        }
        if (info == null || info.getPlayerId() != playerId) {
            log.warn("出牌异常，当前应该出牌玩家: {}, 非法出牌玩家: {}", info == null ? "无" : info.getPlayerId(), playerId);
            return;
        }

        int actionType = reqTurnAction.actionType; // 0: Play, 1: Pass
        
        if (log.isDebugEnabled()) {
            String cardStr = "无";
            if (actionType == 0 && CollUtil.isNotEmpty(reqTurnAction.cards)) {
                Map<Integer, PokerCard> cardMap = ToSouthDataHelper.getCardListMap(ToSouthDataHelper.getPoolId(gameDataVo));
                List<Card> c = new ArrayList<>();
                // 将 clientId 转为 Card 对象以便打印
                for (Integer clientId : reqTurnAction.cards) {
                    for (PokerCard pc : cardMap.values()) {
                        if (pc.getClientId() == clientId) {
                            c.add(pc);
                            break;
                        }
                    }
                }
                c.sort(ToSouthHandUtils.CARD_COMPARATOR);
                cardStr = ToSouthHandUtils.cardListToString(c);
            }
            log.debug("玩家操作 - ID: {}, 动作: {}, 牌: {}", playerId, actionType == 1 ? "过" : "出", cardStr);
        }
        
        // 检查玩家是否在本轮已过牌
        if (actionType == 0 && gameDataVo.getCurRoundPassedPlayerSeats().contains(info.getSeatId())) {
            log.warn("玩家 {} 在本轮已过牌，不能再出牌", info.getPlayerId());
            return;
        }

        // Pass Logic
        if (actionType == 1) {
            // 如果是本轮的领打玩家（即没人出牌或者上一轮的赢家），不能 Pass
            // 只有当场上有牌可跟时，才能 Pass
            if (isFirstPlayer(info.getSeatId())) {
                log.warn("当前是首出阶段，不能过牌");
                return;
            }
            // 过牌成功
            gameDataVo.getCurRoundPassedPlayerSeats().add(info.getSeatId());
            gameDataVo.setPassCount(gameDataVo.getPassCount() + 1);
            log.debug("玩家 {} 过牌，当前连续过牌数: {}", info.getPlayerId(), gameDataVo.getPassCount());
            checkNextTurn(info.getPlayerId());
            return;
        }
        // Play Logic
        List<Integer> playCardIds = reqTurnAction.cards;
        if (CollUtil.isEmpty(playCardIds)) {
            log.warn("玩家未出牌：{}", info.getPlayerId());
            return;
        }
        
        Map<Integer, PokerCard> cardMap = ToSouthDataHelper.getCardListMap(ToSouthDataHelper.getPoolId(gameDataVo));

        // 将 clientId 转换为 pokerPoolId
        List<Integer> realPlayCardIds = new ArrayList<>();
        for (Integer clientId : playCardIds) {
            for (PokerCard card : cardMap.values()) {
                if (card.getClientId() == clientId) {
                    realPlayCardIds.add(card.getPokerPoolId());
                    break;
                }
            }
        }
        if (realPlayCardIds.size() != playCardIds.size()) {
            log.warn("玩家 {} 出的牌包含无效的 clientId: {}", info.getPlayerId(), playCardIds);
            return;
        }
        if (!info.getCurrentCards().containsAll(realPlayCardIds)) {
            log.warn("玩家 {} 出的牌 {} 不属于其手牌 {}", info.getPlayerId(), playCardIds, info.getCurrentCards());
            return;
        }

        List<Card> playCards = playCardsIdsToCards(realPlayCardIds, cardMap);

        // 1. 第一轮黑桃3检测
        if (gameDataVo.isFirstRound() && gameDataVo.getLastPlayCards() == null) {
            boolean hasSpade3 = playCards.stream().anyMatch(c -> c.getRank() == RANK_3 && c.getSuit() == SPADE_SUIT);
            if (!hasSpade3) {
                log.warn("首局首出必须包含黑桃3");
                return;
            }
        }

        // 2. 牌型检查
        ToSouthCardType type = ToSouthHandUtils.getCardType(playCards);
        if (type == ToSouthCardType.NONE) {
            log.warn("[南方前进][出牌] 非法牌型 - 玩家: {}, 座位: {}, 牌: {}",
                    playerId, info.getSeatId(), ToSouthHandUtils.cardListToString(playCards));
            return;
        }
        log.info("[南方前进][出牌] 玩家: {}, 座位: {}, 牌型: {}, 牌: {}",
                playerId, info.getSeatId(), type, ToSouthHandUtils.cardListToString(playCards));

        // 3. 牌型比较
        if (gameDataVo.getRoundLeaderSeatId() != info.getSeatId()) {
            List<Integer> lastCardIds = gameDataVo.getLastPlayCards();
            List<Card> lastCards = playCardsIdsToCards(lastCardIds, cardMap);
            ToSouthCardType lastType = ToSouthHandUtils.getCardType(lastCards);
            log.info("[南方前进][比牌] 上家牌型: {}, 上家牌: {} | 当前牌型: {}, 当前牌: {}",
                    lastType, ToSouthHandUtils.cardListToString(lastCards),
                    type, ToSouthHandUtils.cardListToString(playCards));
            if (!ToSouthHandUtils.compare(lastCards, playCards)) {
                log.warn("[南方前进][比牌] 管不上 - 玩家: {}, {} [{}] 无法压过 {} [{}]",
                        playerId,
                        ToSouthHandUtils.cardListToString(playCards), type,
                        ToSouthHandUtils.cardListToString(lastCards), lastType);
                return;
            }
            log.info("[南方前进][比牌] 管牌成功 - 玩家: {}, {} [{}] 压过 {} [{}]",
                    playerId,
                    ToSouthHandUtils.cardListToString(playCards), type,
                    ToSouthHandUtils.cardListToString(lastCards), lastType);
        } else {
            log.info("[南方前进][首出] 玩家: {}, 座位: {}, 牌型: {}, 牌: {}",
                    playerId, info.getSeatId(), type, ToSouthHandUtils.cardListToString(playCards));
        }

        info.getCurrentCards().removeAll(realPlayCardIds);

        // 出牌后重新排序剩余手牌并更新高亮牌
        if (!info.getCurrentCards().isEmpty()) {
            List<Card> remainingCards = info.getCurrentCards().stream()
                    .map(cardMap::get)
                    .collect(Collectors.toList());
            List<Integer> highlightIds = ToSouthHandUtils.sortAndGetHighlightCards(remainingCards);

            // 更新手牌顺序为排序后的pokerPoolId
            List<Integer> sortedPoolIds = new ArrayList<>();
            for (Card c : remainingCards) {
                if (c instanceof PokerCard pc) {
                    sortedPoolIds.add(pc.getPokerPoolId());
                }
            }
            info.getCurrentCards().clear();
            info.getCurrentCards().addAll(sortedPoolIds);

            // 更新高亮牌
            gameDataVo.getPlayerHighlightCards().put(playerId, highlightIds);
        }

        gameDataVo.setLastPlayCards(realPlayCardIds);
        gameDataVo.setLastPlayCardsType(type.getType());
        gameDataVo.setLastPlaySeatId(info.getSeatId());
        gameDataVo.setRoundLeaderSeatId(info.getSeatId()); // 每次出牌成功，更新轮次领打人为当前出牌者
        gameDataVo.setPassCount(0); // 重置过牌计数

        // 记录出牌
        gameDataVo.getCurrentRoundPlays().add(new ToSouthRoundRecord(info.getSeatId(), realPlayCardIds, playCardIds, type));
        if (log.isDebugEnabled()) {
            playCards.sort(ToSouthHandUtils.CARD_COMPARATOR);
            log.debug("玩家 {} 出牌成功 - 类型: {}, 牌: {}, 剩余手牌: {}", info.getPlayerId(), type, ToSouthHandUtils.cardListToString(playCards), info.getCurrentCards().size());
        }
        if (info.getCurrentCards().isEmpty()) {
            log.info("玩家 {} 胜利 (出完手牌)，游戏结束", info.getPlayerId());
            info.setOver(true);

            // 先广播最后一手出牌信息给所有玩家，再进行结算
            broadcastLastAction(info.getPlayerId());

            // 如果最后一手牌是炸弹，需要先处理炸弹结算
            if (isBomb(type)) {
                processBombSettlement(info.getSeatId());
            }

            // 触发结算逻辑
            ToSouthSettlementContext context = new ToSouthSettlementContext();
            context.setInstantWin(false);
            context.addItem(new ToSouthSettlementContext.SettlementItem(
                    info,
                    true,
                    0,
                    null
            ));
            addPokerPhaseTimer(new ToSouthSettlementPhase(this, context));
            return;
        }

        checkNextTurn(0);
    }

    /**
     * 结算当前轮中的炸弹赔付
     * @param winnerSeatId
     */
    private void processBombSettlement(int winnerSeatId) {
        List<ToSouthRoundRecord> plays = gameDataVo.getCurrentRoundPlays();
        if (CollUtil.isEmpty(plays)) return;

        ToSouthRoundRecord lastPlay = plays.getLast();
        if (lastPlay.seatId != winnerSeatId) {
            return;
        }
        if (!isBomb(lastPlay.cardType)) {
            return;
        }

        log.debug("开始处理炸弹结算 - 赢家座位: {}, 最后出牌类型: {}", winnerSeatId, lastPlay.cardType);

        // 炸弹链
        List<ToSouthRoundRecord> bombChain = new ArrayList<>();
        int victimIndex = -1;
        
        // 从后往前遍历，收集连续的炸弹
        // 炸弹链的中断条件：
        // 1. 遇到非炸弹牌 (这就是被炸的牌)
        // 2. 遍历完列表
        for (int i = plays.size() - 1; i >= 0; i--) {
            ToSouthRoundRecord record = plays.get(i);
            if (isBomb(record.cardType)) {
                bombChain.addFirst(record);
            } else {
                victimIndex = i;
                break; // 找到被炸的牌，停止
            }
        }
        
        log.debug("炸弹链分析 - 链长度: {}, 受害者索引: {}", bombChain.size(), victimIndex);

        if (bombChain.isEmpty()) return;

        // 炸弹牌可以由首个出牌的玩家打出去，打出去没有单独得分
        if (victimIndex == -1 && bombChain.size() == 1) {
            log.debug("首出炸弹且无人压制，不触发额外结算");
            // 这里不能return，因为即便是首出炸弹，也可能触发新的一轮
            // 但是当前函数只负责结算，所以return没问题，逻辑推进由 checkNextTurn 负责
            return;
        }

        ToSouthRoundRecord winnerRecord = bombChain.getLast();
        long winnerId = Objects.requireNonNull(getPlayerBySeatId(winnerRecord.seatId)).getPlayerId();
        long baseBet = gameDataVo.getRoomBet();
        Map<Integer, PokerCard> cardMap = ToSouthDataHelper.getCardListMap(ToSouthDataHelper.getPoolId(gameDataVo));
        SouthernMoneyCfg moneyCfg = ToSouthDataHelper.getSouthernMoneyCfg(gameDataVo);
        if (moneyCfg == null) {
            log.error("缺少SouthernMoneyCfg配置，跳过炸弹结算");
            return;
        }

        List<ToSouthBombDetail> details = new ArrayList<>();

        // 守卫：如果被炸的牌不是可炸牌型（非2单张/对子、非炸弹、非连对），不触发结算
        // 普通单张（非2）、对子（非2）、三张、顺子等均不能被炸
        if (victimIndex != -1) {
            ToSouthRoundRecord victimRecord = plays.get(victimIndex);
            List<Card> victimCards = playCardsIdsToCards(victimRecord.cards, cardMap);
            victimCards.sort(ToSouthHandUtils.CARD_COMPARATOR);
            boolean allRank2 = !victimCards.isEmpty() && victimCards.stream().allMatch(c -> c.getRank() == RANK_2);
            boolean bombable = isBomb(victimRecord.cardType) || allRank2;
            if (!bombable) {
                log.warn("被炸的牌型不可被炸，跳过结算 seatId={}, cardType={}, cards={}",
                        victimRecord.seatId, victimRecord.cardType, victimRecord.cards);
                return;
            }
        }

        List<ToSouthRoundRecord> settledRecords = new ArrayList<>();
        if (victimIndex != -1) {
            settledRecords.add(plays.get(victimIndex));  // 被炸的非炸弹牌（如对2）
        }
        if (bombChain.size() > 1) {
            // 连炸链：累计所有被炸的炸弹牌型（不含赢家自身出的炸弹）
            // 输家（倒数第二个出炸弹的玩家）赔付：被炸非炸弹牌 + 中间所有被炸的炸弹
            // 例：A对红2 → B四条 → C四条 → D四连对 全pass，C赔 (对红2+四条+四条) 给D，D的四连对不计入
            settledRecords.addAll(bombChain.subList(0, bombChain.size() - 1));
        }

        if (CollUtil.isNotEmpty(settledRecords)) {
            long totalMultiplier = 0;
            for (ToSouthRoundRecord settledRecord : settledRecords) {
                long multiplier = getBombSettlementMultiplier(settledRecord, cardMap, moneyCfg);
                if (multiplier <= 0) {
                    log.warn("炸弹结算未命中赔付规则 seatId={}, cardType={}, cards={}",
                            settledRecord.seatId, settledRecord.cardType, settledRecord.cards);
                    continue;
                }
                totalMultiplier += multiplier;
            }

            long victimId;
            int detailType;
            if (bombChain.size() >= 2) {
                ToSouthRoundRecord secondLast = bombChain.get(bombChain.size() - 2);
                victimId = Objects.requireNonNull(getPlayerBySeatId(secondLast.seatId)).getPlayerId();
                detailType = 2;
            } else {
                ToSouthRoundRecord firstVictim = settledRecords.getFirst();
                victimId = Objects.requireNonNull(getPlayerBySeatId(firstVictim.seatId)).getPlayerId();
                detailType = 1;
            }

            long score = baseBet * totalMultiplier;
            if (score > 0 && victimId != winnerId) {
                log.debug("炸弹结算 - 赢家: {}, 输家: {}, 被炸数量: {}, 总倍数: {}, 金额: {}, 连炸: {}",
                        winnerId, victimId, settledRecords.size(), totalMultiplier, score, bombChain.size() >= 2);
                addBombScore(details, victimId, winnerId, score, detailType);
            }
        }
        
        if (CollUtil.isNotEmpty(details)) {
             NotifyToSouthBombSettlement notify = new NotifyToSouthBombSettlement();
             notify.details = details;
             broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
        }
    }

    private void addBombScore(List<ToSouthBombDetail> details, long loserId, long winnerId, long score, int type) {
        // 直接扣除输家积分
        deductItem(loserId, score, AddType.GAME_SETTLEMENT, "南方前进炸弹扣分", false);
        details.add(new ToSouthBombDetail(loserId, score , ToSouthConstant.BOMB_LOSE_TYPE));

        // 计算赢家税后积分并添加
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        long tax = BigDecimal.valueOf(score)
                .multiply(BigDecimal.valueOf(10000 - roomCfg.getEffectiveRatio()))
                .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN).longValue();
        gameDataTracker.addGameLogData("tax", tax);
        long finalWinScore = score - tax;

        addItem(winnerId, finalWinScore, AddType.GAME_SETTLEMENT);
        details.add(new ToSouthBombDetail(winnerId, finalWinScore ,ToSouthConstant.BOMB_WIN_TYPE));
    }

    /**
     * 计算被炸牌型的赔付倍数（中局赔付，读 被管炸弹 配置而非结算剩余配置）
     * 2的牌型按每张单独计算：红2 = remainred2 倍，黑2 = remainblack2 倍，累加
     * 炸弹牌型读配置：四条=fourkindboom，三连对=remainBoom，四连对=fourpairsboom
     */
    private long getBombSettlementMultiplier(ToSouthRoundRecord victimRecord,
                                              Map<Integer, PokerCard> cardMap,
                                              SouthernMoneyCfg moneyCfg) {
        List<Card> victimCards = playCardsIdsToCards(victimRecord.cards, cardMap);
        if (CollUtil.isEmpty(victimCards)) return 0;

        victimCards.sort(ToSouthHandUtils.CARD_COMPARATOR);

        // 2的牌型：每张单独计算（红2 * remainred2 + 黑2 * remainblack2）
        // 适用 SINGLE / PAIR / TRIPLE 中全为2的情况
        boolean allRank2 = victimCards.stream().allMatch(c -> c.getRank() == RANK_2);
        if (allRank2 && (victimRecord.cardType == ToSouthCardType.SINGLE
                || victimRecord.cardType == ToSouthCardType.PAIR
                || victimRecord.cardType == ToSouthCardType.TRIPLE)) {
            long multi = 0;
            for (Card c : victimCards) {
                boolean isRed = c.getSuit() == HEART_SUIT || c.getSuit() == DIAMOND_SUIT;
                multi += isRed ? moneyCfg.getRemainred2() : moneyCfg.getRemainblack2();
            }
            return multi;
        }

        // 被炸的炸弹：读被管炸弹配置（非结算剩余配置）
        if (victimRecord.cardType == ToSouthCardType.BOMB_QUAD) {
            return moneyCfg.getFourkindboom();
        }
        if (victimRecord.cardType == ToSouthCardType.CONSECUTIVE_PAIRS) {
            return victimCards.size() >= 8
                    ? moneyCfg.getFourpairsboom()   // 四连对
                    : moneyCfg.getRemainBoom();     // 三连对
        }
        return 0;
    }

    private boolean containsRedTwo(List<Card> cards) {
        for (Card card : cards) {
            if (card.getRank() == RANK_2 && (card.getSuit() == HEART_SUIT || card.getSuit() == DIAMOND_SUIT)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isBomb(ToSouthCardType type) {
        return type == ToSouthCardType.BOMB_QUAD || type == ToSouthCardType.CONSECUTIVE_PAIRS;
    }

    /**
     * 是否为第一个打牌的
     * @return
     */
    private boolean isFirstPlayer(int seatId) {
        return gameDataVo.getLastPlayCards() == null && gameDataVo.getRoundLeaderSeatId() == seatId;
    }

    private List<Card> playCardsIdsToCards(List<Integer> ids, Map<Integer, PokerCard> map) {
        return ids.stream().map(map::get).collect(Collectors.toList());
    }

    private void checkNextTurn(long passerPlayerId) {
        log.debug("检查下家 - 当前索引: {}", gameDataVo.getIndex());

        PlayerSeatInfo nextPlayer = getNextExePlayer();

        // 如果没有下家，或者下家就是上一个出牌的人（说明其他人都过了/出局），一轮结束
        if (nextPlayer == null || nextPlayer.getSeatId() == gameDataVo.getLastPlaySeatId()) {
            int winnerSeatId = gameDataVo.getLastPlaySeatId();
            PlayerSeatInfo nextLeader = getPlayerBySeatId(winnerSeatId);

            if (nextLeader != null) {
                log.debug("一轮结束，玩家 {} 获得球权，新一轮开始", nextLeader.getPlayerId());
                gameDataVo.setRoundLeaderSeatId(nextLeader.getSeatId());
                gameDataVo.setLastPlayCards(null);
                gameDataVo.setFirstRound(false);
                gameDataVo.setPassCount(0);
                gameDataVo.getCurRoundPassedPlayerSeats().clear();

                // 处理炸弹结算 (如果有的话)
                processBombSettlement(winnerSeatId);
                // 清空本轮出牌记录
                gameDataVo.getCurrentRoundPlays().clear();

                broadcastNextTurn(nextLeader.getPlayerId(), false, passerPlayerId);
                gameDataVo.setIndex(nextLeader.getSeatId());
                addNextTimer(nextLeader, 0);
            }
        } else {
            // 继续当前轮，找下家
            log.debug("当前轮继续，下家 {} 出牌", nextPlayer.getPlayerId());
            broadcastNextTurn(nextPlayer.getPlayerId(), true, passerPlayerId);
            gameDataVo.setIndex(nextPlayer.getSeatId());
            addNextTimer(nextPlayer, 0);
        }
    }
    
    /**
     * 获取指定座位的玩家信息
     */
    private PlayerSeatInfo getPlayerBySeatId(int seatId) {
         for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
             if (info.getSeatId() == seatId) return info;
         }
         return null;
    }

    public void broadcastNextTurn(long waitPlayerId) {
        broadcastNextTurn(waitPlayerId, true, 0);
    }

    public void broadcastNextTurn(long waitPlayerId, boolean canPass) {
        broadcastNextTurn(waitPlayerId, canPass, 0);
    }

    public void broadcastNextTurn(long waitPlayerId, boolean canPass, long passerPlayerId) {
        NotifyToSouthTurnActionInfo notify = new NotifyToSouthTurnActionInfo();
        ToSouthActionInfo actionInfo = new ToSouthActionInfo();
        actionInfo.lastpassUserId = passerPlayerId;
        actionInfo.waitPlayerId = waitPlayerId;
        actionInfo.canPass = canPass;
        fillCommonActionInfo(actionInfo);
        // 计算等待时间
        long currentTime = System.currentTimeMillis();
        long duration = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.PLAY_CARDS);
        actionInfo.waitEndTime = currentTime + duration;
        // 1. 发给其他人,不携带推荐牌组（非等待玩家不能出牌）
        actionInfo.recommendCardsList = null;
//        actionInfo.canPlay = false;
        notify.actionInfo = actionInfo;

        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.getPlayerId() == waitPlayerId) continue;
            actionInfo.canPlay = false;
            // 为每个接收者设置其自己的手牌
            // 在 Netty/Protobuf 场景下，通常在 write 时会序列化，如果是同步序列化，那么可以复用对象。
            // 但为了绝对安全，这里使用 clone
            ToSouthActionInfo playerActionInfo = cloneActionInfo(actionInfo);
            playerActionInfo.selfHandCards = PokerDataHelper.getClientId(gameDataVo, info.getCurrentCards());
            playerActionInfo.selfHighlightCards = gameDataVo.getPlayerHighlightCards().get(info.getPlayerId());

            notify.actionInfo = playerActionInfo;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(info.getPlayerId(), notify));
        }
        Map<Long, PlayerSeatInfo> playerSeatInfoMap = gameDataVo.getPlayerSeatInfoMap();
        PlayerSeatInfo waitPlayer = playerSeatInfoMap.get(waitPlayerId);
        // 计算推荐出牌 (仅针对等待玩家) 发给当前操作玩家 (带 recommend)
        fillRecommendCards(actionInfo, waitPlayer);
        
        ToSouthActionInfo waitPlayerActionInfo = cloneActionInfo(actionInfo);
        waitPlayerActionInfo.selfHandCards = PokerDataHelper.getClientId(gameDataVo, waitPlayer.getCurrentCards());
        waitPlayerActionInfo.selfHighlightCards = gameDataVo.getPlayerHighlightCards().get(waitPlayerId);

        notify.actionInfo = waitPlayerActionInfo;

        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(waitPlayerId, notify));
    }

    /**
     * 广播最后一手牌的出牌信息（游戏结束前，通知所有玩家最终出牌动作）
     * 与 broadcastNextTurn 不同：没有下一个等待玩家，不需要推荐牌组
     * @param winnerPlayerId 赢家玩家ID，用作 waitPlayerId 让前端正常展示出牌信息
     */
    private void broadcastLastAction(long winnerPlayerId) {
        NotifyToSouthTurnActionInfo notify = new NotifyToSouthTurnActionInfo();
        ToSouthActionInfo actionInfo = new ToSouthActionInfo();
        actionInfo.lastpassUserId = 0;
        actionInfo.waitPlayerId = winnerPlayerId;
        actionInfo.canPass = false;
        actionInfo.canPlay = false;
        actionInfo.waitEndTime = -1;
        fillCommonActionInfo(actionInfo);
        actionInfo.recommendCardsList = null;

        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            ToSouthActionInfo playerActionInfo = cloneActionInfo(actionInfo);
            playerActionInfo.selfHandCards = PokerDataHelper.getClientId(gameDataVo, info.getCurrentCards());
            playerActionInfo.selfHighlightCards = gameDataVo.getPlayerHighlightCards().get(info.getPlayerId());
            notify.actionInfo = playerActionInfo;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(info.getPlayerId(), notify));
        }
    }

    // 当前轮玩家公开信息
    private void fillCurRoundPlayerInfos(ToSouthActionInfo actionInfo) {
        for (PlayerSeatInfo playerSeatInfo : gameDataVo.getPlayerSeatInfoList()) {
            ToSouthCurRoundPlayerInfo curRoundPlayerInfo = new ToSouthCurRoundPlayerInfo();
            curRoundPlayerInfo.playerId = playerSeatInfo.getPlayerId();
            curRoundPlayerInfo.seatId = playerSeatInfo.getSeatId();
            curRoundPlayerInfo.passed = gameDataVo.getCurRoundPassedPlayerSeats().contains(curRoundPlayerInfo.seatId);
            curRoundPlayerInfo.cardCount = playerSeatInfo.getCurrentCards().size();
            actionInfo.curRoundPlayerInfos.add(curRoundPlayerInfo);
        }
    }

    // 本轮出牌历史
    private void fillCurRoundPlayedCardsHistory(ToSouthActionInfo actionInfo) {
        if (gameDataVo.getCurrentRoundPlays().isEmpty()) {
            return;
        }
        // 本轮出牌历史
        actionInfo.curRoundPlayedCardHistory = gameDataVo.getCurrentRoundPlays().stream().map(play -> {
            ToSouthPlayCardRecord playCardRecord = new ToSouthPlayCardRecord();
            playCardRecord.seatId = play.seatId;
            playCardRecord.playedCards = play.getCardClientIds();
            return playCardRecord;
        }).toList();
    }

    /**
     * 填充 ActionInfo 中与轮次状态相关的公共字段，供 broadcastNextTurn 与 respRoomInitInfoAction 共用。
     * 包含：lastPlayCards / lastPlayCardsType / lastPlaySeatId / roundLeaderSeatId / isFirstRound
     *       curRoundPlayerInfos / curRoundPlayedCardHistory
     *
     * lastPlaySeatId / lastPlayCards 的语义是「上一个动作的玩家」：
     *   - 若上一动作是 pass（lastpassUserId != 0）：lastPlaySeatId = pass 玩家座位，lastPlayCards 为空
     *   - 若上一动作是出牌（lastpassUserId == 0）：同步 gameDataVo 中真正出牌玩家的牌型与座位
     */
    private void fillCommonActionInfo(ToSouthActionInfo actionInfo) {
        if (actionInfo.lastpassUserId != 0) {
            // 上一个动作是 pass：只记录 pass 玩家的座位，不填牌（前端桌面牌保持上一次出牌状态不变）
            PlayerSeatInfo passerInfo = gameDataVo.getPlayerSeatInfoMap().get(actionInfo.lastpassUserId);
            if (passerInfo != null) {
                actionInfo.lastPlaySeatId = passerInfo.getSeatId();
            }
        } else if (CollUtil.isNotEmpty(gameDataVo.getLastPlayCards())) {
            // 上一个动作是出牌：同步上家打出的牌、牌型、座位
            actionInfo.lastPlayCards = PokerDataHelper.getClientId(gameDataVo, gameDataVo.getLastPlayCards());
            actionInfo.lastPlayCardsType = gameDataVo.getLastPlayCardsType();
            actionInfo.lastPlaySeatId = gameDataVo.getLastPlaySeatId();
        }
        // 其余情况（新一轮开始，gameDataVo.getLastPlayCards() == null）：三个字段均保持默认空值
        actionInfo.roundLeaderSeatId = gameDataVo.getRoundLeaderSeatId();
        actionInfo.isFirstRound = gameDataVo.isFirstRound();
        fillCurRoundPlayerInfos(actionInfo);
        fillCurRoundPlayedCardsHistory(actionInfo);
    }

    /**
     * 计算并填充将出牌玩家的全部推荐牌组
     * @param actionInfo
     * @param waitPlayer
     */
    private void fillRecommendCards(ToSouthActionInfo actionInfo, PlayerSeatInfo waitPlayer) {
        if (waitPlayer == null) return;
        
        List<ToSouthRecommendCards> recommendCardsList = new ArrayList<>();
        Map<Integer, PokerCard> cardMap = ToSouthDataHelper.getCardListMap(ToSouthDataHelper.getPoolId(gameDataVo));
        List<Card> handCards = waitPlayer.getCurrentCards().stream()
                .map(cardMap::get)
                .collect(Collectors.toList());
                
        if (isFirstPlayer(waitPlayer.getSeatId())) {
            // 首出推荐
            Map<Integer, List<Card>> rankMap = ToSouthHandUtils.convertCardListToRankMap(handCards);
            if (gameDataVo.isFirstRound()) {
                // 首局首出 (找含黑桃3的)
                List<Card> best = handCards.stream()
                        .filter(c -> c.getRank() == RANK_3 && c.getSuit() == SPADE_SUIT)
                        .findFirst()
                        .map(spade3 -> ToSouthHandUtils.findBestPlayWithFirstCard(rankMap, spade3))
                        .orElse(null);
                if (CollUtil.isNotEmpty(best)) {
                    List<Integer> ids = ToSouthDataHelper.getClientId(gameDataVo, best.stream().map(c -> ((PokerCard)c).getPokerPoolId()).collect(Collectors.toList()));
                    recommendCardsList.add(new ToSouthRecommendCards(ids));
                }
            } else {
                // 普通首出 (获取所有可能)
                List<List<Card>> allBestPlays = ToSouthHandUtils.findAllBestPlays(handCards);
                for (List<Card> play : allBestPlays) {
                    List<Integer> ids = ToSouthDataHelper.getClientId(gameDataVo, play.stream().map(c -> ((PokerCard)c).getPokerPoolId()).collect(Collectors.toList()));
                    recommendCardsList.add(new ToSouthRecommendCards(ids));
                }
            }
        } else {
            // 跟牌推荐 (获取所有可能)
            List<Integer> lastIds = gameDataVo.getLastPlayCards();
            if (CollUtil.isNotEmpty(lastIds)) {
                List<Card> lastCards = lastIds.stream().map(cardMap::get).collect(Collectors.toList());
                List<List<Card>> allPlays = ToSouthHandUtils.findAllFollowPlays(handCards, lastCards);
                
                for (List<Card> play : allPlays) {
                     List<Integer> ids = ToSouthDataHelper.getClientId(gameDataVo, play.stream().map(c -> ((PokerCard)c).getPokerPoolId()).collect(Collectors.toList()));
                     recommendCardsList.add(new ToSouthRecommendCards(ids));
                }
            }
        }
        
        if (CollUtil.isNotEmpty(recommendCardsList)) {
            actionInfo.recommendCardsList = recommendCardsList;
        }
        actionInfo.canPlay = CollUtil.isNotEmpty(recommendCardsList);
    }

    private ToSouthActionInfo cloneActionInfo(ToSouthActionInfo source) {
        ToSouthActionInfo target = new ToSouthActionInfo();
        target.waitPlayerId = source.waitPlayerId;
        target.waitEndTime = source.waitEndTime;
        target.canPass = source.canPass;
        target.canPlay = source.canPlay;
        target.curRoundPlayerInfos = source.curRoundPlayerInfos;
        target.curRoundPlayedCardHistory = source.curRoundPlayedCardHistory;
        target.recommendCardsList = source.recommendCardsList;
        target.lastPlayCards = source.lastPlayCards;
        target.lastPlayCardsType = source.lastPlayCardsType;
        target.lastPlaySeatId = source.lastPlaySeatId;
        target.roundLeaderSeatId = source.roundLeaderSeatId;
        target.isFirstRound = source.isFirstRound;
        target.selfHandCards = source.selfHandCards;
        target.selfHighlightCards = source.selfHighlightCards;
        target.lastpassUserId = source.lastpassUserId;
        return target;
    }

    private int getActivePlayerCount() {
        int count = 0;
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (!info.isOver() && !info.isDelState()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void respRoomInitInfoAction(PlayerController playerController) {
        log.debug("响应南方前进房间信息 - 玩家: {}", playerController.playerId());
        RespToSouthRoomBaseInfo baseInfo = new RespToSouthRoomBaseInfo(Code.SUCCESS);
        baseInfo.phase = getCurrentGamePhase();
         if (playerController.getPlayer().getRoomId() > 0 && playerController.getScene() instanceof AbstractRoomController<?, ?> roomController) {
             WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomController.getRoom().getRoomCfgId());
             baseInfo.roomBet =warehouseCfg.getEnterLimit();
        }
        baseInfo.playerInfos = new ArrayList<>();
        Map<Long, PlayerSeatInfo> playerSeatInfoMap = gameDataVo.getPlayerSeatInfoMap();
        for (Map.Entry<Integer, SeatInfo> entry : gameDataVo.getSeatInfo().entrySet()) {
            SeatInfo seatInfo = entry.getValue();
            if (playerNotInit(seatInfo.getPlayerId()) || !seatInfo.isSeatDown()) {
                continue;
            }
            PokerPlayerInfo pokerPlayerInfo = PokerBuilder.getPokerPlayerInfo(seatInfo, this);
            PlayerSeatInfo seatPlayerInfo = playerSeatInfoMap.get(seatInfo.getPlayerId());
            if (Objects.nonNull(seatPlayerInfo) && !seatPlayerInfo.isDelState()) {
                pokerPlayerInfo.operationType = seatPlayerInfo.getOperationType();
            }
            baseInfo.playerInfos.add(pokerPlayerInfo);
        }

        PlayerSeatInfo selfPlayerInfo = gameDataVo.getPlayerSeatInfoMap().get(playerController.playerId());

        if (baseInfo.phase == EGamePhase.PLAY_CART) {
            ToSouthActionInfo actionInfo = new ToSouthActionInfo();
            fillCommonActionInfo(actionInfo);

            // 重连玩家自己的手牌及高亮牌
            if (selfPlayerInfo != null && !selfPlayerInfo.isDelState()) {
                actionInfo.selfHandCards = PokerDataHelper.getClientId(gameDataVo, selfPlayerInfo.getCurrentCards());
                actionInfo.selfHighlightCards = gameDataVo.getPlayerHighlightCards().get(playerController.playerId());
            }

            // 默认不可出牌；仅当重连玩家正是当前等待出牌方时才可出牌
            actionInfo.canPlay = false;

            PlayerSeatInfo currentPlayer = gameDataVo.getCurrentPlayerSeatInfo();
            if (Objects.nonNull(currentPlayer)) {
                actionInfo.waitPlayerId = currentPlayer.getPlayerId();
                actionInfo.canPass = !isFirstPlayer(currentPlayer.getSeatId());
                if (currentPlayer.getPlayerId() == playerController.playerId()) {
                    actionInfo.canPlay = true;
                    fillRecommendCards(actionInfo, currentPlayer);
                }
            }

            if (Objects.nonNull(gameDataVo.getPlayerTimerEvent())) {
                actionInfo.waitEndTime = gameDataVo.getPlayerTimerEvent().getNextTime();
            }

            baseInfo.actionInfo = actionInfo;
        }
        // WAIT_READY 阶段：重连时需补发当前已准备的玩家列表
        if (baseInfo.phase == EGamePhase.WAIT_READY) {
            baseInfo.readyPlayerIds = gameDataVo.getReadyPlayerIds();
        }
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerController.playerId(), baseInfo));
        // START_GAME 阶段：已发牌但出牌阶段尚未开始，重连时需补发手牌数据
        if (baseInfo.phase == EGamePhase.START_GAME && selfPlayerInfo != null && !selfPlayerInfo.isDelState()) {
            List<Integer> sortedHandCards = PokerDataHelper.getClientId(gameDataVo, selfPlayerInfo.getCurrentCards());
            List<Integer> highlightCards = gameDataVo.getPlayerHighlightCards().get(playerController.playerId());
            RespToSouthSendCardsInfo sendCardsInfo = new RespToSouthSendCardsInfo();
            sendCardsInfo.sortedHandCards = sortedHandCards;
            sendCardsInfo.originalHandCards = new ArrayList<>(sortedHandCards);
            Collections.shuffle(sendCardsInfo.originalHandCards);
            sendCardsInfo.highlightCards = highlightCards;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerController.playerId(), sendCardsInfo));
        }
    }

    /**
     * 南方前进请求准备/取消准备（在 WAIT_READY 阶段，四人全部准备后才开始发牌）
     *
     * @param playerId 玩家id
     * @param req      请求（status: 1=准备, 2=取消）
     */
    public void reqToSouthGoReady(long playerId, ReqToSouthGoReady req) {
        NotifyToSouthPlayerReady notify = new NotifyToSouthPlayerReady();
        // 校验玩家是否在座位上，且当前处于等待准备阶段
        TreeMap<Integer, SeatInfo> seatInfo = gameDataVo.getSeatInfo();
        SeatInfo playerSeatInfo = null;
        for (SeatInfo info : seatInfo.values()) {
            if (info.getPlayerId() == playerId) {
                playerSeatInfo = info;
                break;
            }
        }
        if (playerSeatInfo == null || !playerSeatInfo.isSeatDown() || getCurrentGamePhase() != EGamePhase.WAIT_READY) {
            notify.code = Code.ERROR_REQ;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notify));
            return;
        }

        int status = req.status; // 1=准备, 2=取消

        if (status == 2) {
            // 取消准备
            if (!gameDataVo.getReadyPlayerIds().contains(playerId)) {
                notify.code = Code.REPEAT_OP;
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notify));
                return;
            }
            gameDataVo.getReadyPlayerIds().remove(playerId);
            log.info("玩家 {} 取消准备，当前准备人数: {}", playerId, gameDataVo.getReadyPlayerIds().size());
            notify.playerId = playerId;
            notify.status = 2;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
        } else {
            // 准备（status == 1 或默认）
            if (gameDataVo.getReadyPlayerIds().contains(playerId)) {
                notify.code = Code.REPEAT_OP;
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notify));
                return;
            }
            gameDataVo.getReadyPlayerIds().add(playerId);
            log.info("玩家 {} 准备完成，当前准备人数: {}", playerId, gameDataVo.getReadyPlayerIds().size());
            notify.playerId = playerId;
            notify.status = 1;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
            // 检查是否满足开局条件（人数足够 + 全部准备）
            tryStartGame();
        }
    }

    @Override
    public boolean tryStartGame() {
        if (getCurrentGamePhase() != EGamePhase.WAIT_READY) {
            return false;
        }
        // 结算后/首次进入 WAIT_READY 时：先同步阶段给客户端，再启动准备倒计时
        if (gameDataVo.getReadyPlayerIds().isEmpty()) {
            // 1. 先广播阶段变化，客户端收到后展示准备界面
            NotifyPokerPhaseChange phaseChange = PokerBuilder.buildNotifyPhaseChange(EGamePhase.WAIT_READY, -1);
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(phaseChange));
            log.info("广播阶段变化：WAIT_READY，等待玩家准备");

            // 2. 机器人自动准备
            for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
                if (!info.isSeatDown()) continue;
                long pid = info.getPlayerId();
                if (gameDataVo.getReadyPlayerIds().contains(pid)) continue;
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(pid);
                if (gamePlayer instanceof GameRobotPlayer) {
                    gameDataVo.getReadyPlayerIds().add(pid);
                    NotifyToSouthPlayerReady notify = new NotifyToSouthPlayerReady();
                    notify.playerId = pid;
                    notify.status = 1;
                    broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
                    log.info("机器人 {} 自动准备", pid);
                }
            }

            // 3. 同步消息发完后，为未准备的真实玩家启动准备倒计时
            for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
                if (!info.isSeatDown()) continue;
                long pid = info.getPlayerId();
                if (gameDataVo.getReadyPlayerIds().contains(pid)) continue;
                if (gameDataVo.getReadyTimerScheduled().contains(pid)) continue;
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(pid);
                if (!(gamePlayer instanceof GameRobotPlayer)) {
                    scheduleReadyTimeout(pid);
                }
            }
        } else {
            // 非首次调用（玩家点击准备后触发）：只补充机器人和倒计时
            for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
                if (!info.isSeatDown()) continue;
                long pid = info.getPlayerId();
                if (gameDataVo.getReadyPlayerIds().contains(pid)) continue;
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(pid);
                if (gamePlayer instanceof GameRobotPlayer) {
                    gameDataVo.getReadyPlayerIds().add(pid);
                    NotifyToSouthPlayerReady notify = new NotifyToSouthPlayerReady();
                    notify.playerId = pid;
                    notify.status = 1;
                    broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
                    log.info("机器人 {} 自动准备", pid);
                }
            }
            for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
                if (!info.isSeatDown()) continue;
                long pid = info.getPlayerId();
                if (gameDataVo.getReadyPlayerIds().contains(pid)) continue;
                if (gameDataVo.getReadyTimerScheduled().contains(pid)) continue;
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(pid);
                if (!(gamePlayer instanceof GameRobotPlayer)) {
                    scheduleReadyTimeout(pid);
                }
            }
        }
        // 人数不够，等待
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        int total = gameDataVo.getSeatDownNum();
        if (total < roomCfg.getMinPlayer()) {
            return false;
        }
        // 检查所有在座玩家是否已准备
        for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
            if (!info.isSeatDown()) continue;
            if (!gameDataVo.getReadyPlayerIds().contains(info.getPlayerId())) {
                // 还有玩家未准备，等待
                return false;
            }
        }
        // 全部准备完成，开始游戏前先检查资金
        // 资金检查：每位玩家需持有倍场50倍资金，不足则踢出房间（在 WAIT_READY 阶段踢人，exitRoom 可正常生效）
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(getRoom().getRoomCfgId());
        long minBalance = warehouseCfg.getEnterLimit() * 50L;
        List<Long> insufficientPlayerIds = new ArrayList<>();
        for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
            if (!info.isSeatDown()) continue;
            long pid = info.getPlayerId();
            long playerBalance = getTransactionItemNum(pid);
            if (playerBalance < minBalance) {
                log.info("玩家 {} 资金不足，当前: {}, 需要: {}, 踢出房间", pid, playerBalance, minBalance);
                insufficientPlayerIds.add(pid);
            }
        }
        if (!insufficientPlayerIds.isEmpty()) {
            for (Long pid : insufficientPlayerIds) {
                gameDataVo.getReadyPlayerIds().remove(pid);
                RoomPlayer roomPlayer = getRoomController().getRoomPlayer(pid);
                if (roomPlayer == null || roomPlayer.isOnline()) {
                    NotifyExitRoom exitNotify = new NotifyExitRoom();
                    exitNotify.langId = Code.USER_NOT_GOLD;
                    broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(pid, exitNotify));
                    log.info("玩家 {} 资金不足，通知客户端退出房间", pid);
                } else {
                    getRoomController().getRoomManager().exitRoom(pid);
                    log.info("玩家 {} 离线且资金不足，服务端直接退出房间", pid);
                }
            }
            // 踢人后重新检查人数是否足够
            int remaining = gameDataVo.getSeatDownNum();
            if (remaining < roomCfg.getMinPlayer()) {
                log.info("资金检查后人数不足 ({}/{}), 无法开局", remaining, roomCfg.getMinPlayer());
                return false;
            }
        }

        addPokerPhaseTimer(new ToSouthStartGamePhase(this, gameDataVo.getId()));
        log.info("全部玩家已准备，开始游戏 当前id{} roomId:{}", gameDataVo.getId(), roomController.getRoom().getId());
        return true;
    }

    /**
     * 为真实玩家启动10秒准备倒计时，超时未准备则踢出房间
     */
    private void scheduleReadyTimeout(long playerId) {
        gameDataVo.getReadyTimerScheduled().add(playerId);
        ToSouthReadyTimeoutHandler handler = new ToSouthReadyTimeoutHandler(playerId, gameDataVo.getId(), this);
        long exeTime = System.currentTimeMillis() + READY_TIMEOUT;
        TimerEvent<IProcessorHandler> timerEvent = new TimerEvent<>(this, exeTime, handler);
        addGameTimeEvent(timerEvent, RoomEventType.ROOM_PHASE_RUN_EVENT);
        log.info("玩家 {} 准备倒计时开始 ({}秒)", playerId, READY_TIMEOUT / 1000);
    }

    /**
     * 踢出未准备的玩家（对齐德州扑克退出房间逻辑）
     * 在线玩家：发送 NotifyExitRoom 通知，由客户端处理退出
     * 离线玩家：服务端直接调用 exitRoom 清理房间信息
     */
    public void kickUnreadyPlayer(long playerId) {
        RoomPlayer roomPlayer = getRoomController().getRoomPlayer(playerId);
        if (roomPlayer == null || roomPlayer.isOnline()) {
            NotifyExitRoom exitNotify = new NotifyExitRoom();
            exitNotify.langId = gameDataVo.getRoomCfg().getEscTipText();
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, exitNotify));
            log.info("玩家 {} 因未准备，通知客户端退出房间", playerId);
        } else {
            getRoomController().getRoomManager().exitRoom(playerId);
            log.info("玩家 {} 离线且未准备，服务端直接退出房间", playerId);
        }
    }

    @Override
    public void onPlayerLeaveRoomAction(RoomPlayer roomPlayer, SeatInfo remove) {
        long playerId = roomPlayer.getPlayerId();
        // 清除该玩家的准备状态
        gameDataVo.getReadyPlayerIds().remove(playerId);
        gameDataVo.getReadyTimerScheduled().remove(playerId);
        // 清除续局状态，有人退出后下一局视为首局（黑桃3先出）
        gameDataVo.setLastGameWinnerPlayerId(0);
        gameDataVo.getLastGamePlayerIds().clear();
        log.info("玩家 {} 离开房间，已清除准备状态和续局状态", playerId);
    }

    @Override
    public void onRunGamePlayerLeaveRoom(SeatInfo remove) {
        // 自动走 ToSouthAutoPlayHandler 模块
    }

    @Override
    public void addNextTimer(PlayerSeatInfo nextExePlayer, int sendCardNum) {
        // 给下个玩家增加定时器
        long playerId = nextExePlayer.getPlayerId();
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        
        int operationTime = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.PLAY_CARDS);

        // 如果是机器人，添加机器人处理器
        if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
             int delay = RandomUtils.nextInt(2000, 5000);
             ToSouthAutoPlayHandler handler = new ToSouthAutoPlayHandler(playerId, gameDataVo.getId(), this);
             addPlayerTimer(handler, delay);
        } else {
            // 真实玩家，添加超时自动操作
            addPlayerTimer(new ToSouthAutoPlayHandler(playerId, gameDataVo.getId(), this), operationTime);
        }
    }

    @Override
    public void dealBet(long playerId, ReqPokerBet reqPokerBet) {

    }

    @Override
    protected ToSouthGameDataVo createRoomDataVo(Room_ChessCfg roomCfg) {
        return new ToSouthGameDataVo(roomCfg);
    }

    @Override
    public EGameType gameControlType() {
        return EGameType.TO_SOUTH;
    }
}
