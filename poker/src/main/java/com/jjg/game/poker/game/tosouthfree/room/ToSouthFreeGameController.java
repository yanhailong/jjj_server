package com.jjg.game.poker.game.tosouthfree.room;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.*;
import com.jjg.game.core.pb.NotifyExitRoom;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerPhaseChange;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerPlayerChange;
import com.jjg.game.poker.game.common.message.req.ReqPokerBet;
import com.jjg.game.poker.game.common.message.req.ReqPokerSampleCardOperation;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.tosouthfree.autohandler.ToSouthFreeAutoPlayHandler;
import com.jjg.game.poker.game.tosouthfree.autohandler.ToSouthFreeReadyTimeoutHandler;
import com.jjg.game.poker.game.tosouthfree.autohandler.ToSouthFreeRobotHandler;
import com.jjg.game.poker.game.tosouthfree.cardlib.ToSouthFreeCardLibManager;
import com.jjg.game.poker.game.tosouthfree.constant.ToSouthFreeConstant;
import com.jjg.game.poker.game.tosouthfree.data.ToSouthFreeDataHelper;
import com.jjg.game.poker.game.tosouthfree.data.ToSouthFreeSettlementContext;
import com.jjg.game.poker.game.tosouthfree.gamephase.ToSouthFreeSettlementPhase;
import com.jjg.game.poker.game.tosouthfree.gamephase.ToSouthFreeStartGamePhase;
import com.jjg.game.poker.game.tosouthfree.message.bean.*;
import com.jjg.game.poker.game.tosouthfree.message.notify.NotifyToSouthFreeBombSettlement;
import com.jjg.game.poker.game.tosouthfree.message.notify.NotifyToSouthFreePlayerReady;
import com.jjg.game.poker.game.tosouthfree.message.notify.NotifyToSouthFreeTurnActionInfo;
import com.jjg.game.poker.game.tosouthfree.message.req.ReqToSouthFreeGoReady;
import com.jjg.game.poker.game.tosouthfree.message.req.ReqToSouthFreeTurnAction;
import com.jjg.game.poker.game.tosouthfree.message.resp.RespToSouthFreeRoomBaseInfo;
import com.jjg.game.poker.game.tosouthfree.message.resp.RespToSouthFreeSendCardsInfo;
import com.jjg.game.poker.game.tosouthfree.room.data.ToSouthFreeGameDataVo;
import com.jjg.game.poker.game.tosouthfree.room.data.ToSouthFreeRoundRecord;
import com.jjg.game.poker.game.tosouthfree.util.ToSouthFreeCardType;
import com.jjg.game.poker.game.tosouthfree.util.ToSouthFreeHandUtils;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.robot.RobotScheduleUtil;
import com.jjg.game.room.timer.RoomEventType;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ChessRobotCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import com.jjg.game.sampledata.bean.SouthernMoneyCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.jjg.game.poker.game.tosouthfree.constant.ToSouthFreeConstant.*;

@GameController(gameType = EGameType.TO_SOUTH_FREE, roomType = RoomType.POKER_ROOM)
public class ToSouthFreeGameController extends BasePokerGameController<ToSouthFreeGameDataVo> {
    /**
     * 准备倒计时（毫秒）
     */
    private static final int READY_TIMEOUT = 11000;

    public ToSouthFreeGameController(AbstractRoomController<Room_ChessCfg, ? extends Room> roomController) {
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
    public boolean reqChangeTable(PlayerController playerController, ToSouthFreeGameController controller) {
        AbstractRoomController<Room_ChessCfg, ? extends Room> abstractRoomController = controller.getRoomController();
        Room room = abstractRoomController.getRoom();
        boolean changed =
                roomController.getRoomManager().changeRoom(
                        playerController, room, room.getGameType(), controller.getRoom().getRoomCfgId(), controller.getRoom().getMaxLimit());
        return changed;
    }

    @Override
    public boolean canJoinRobot() {
        return getCurrentGamePhase() == EGamePhase.WAIT_READY;
    }

    public void turnAction(long playerId, ReqToSouthFreeTurnAction reqTurnAction) {
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

        /*if (log.isDebugEnabled()) {
            String cardStr = "无";
            if (actionType == 0 && CollUtil.isNotEmpty(reqTurnAction.cards)) {
                Map<Integer, PokerCard> cardMap = ToSouthFreeDataHelper.getCardListMap(ToSouthFreeDataHelper.getPoolId(gameDataVo));
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
                c.sort(ToSouthFreeHandUtils.CARD_COMPARATOR);
                cardStr = ToSouthFreeHandUtils.cardListToString(c);
            }
            //log.debug("玩家操作 - ID: {}, 动作: {}, 牌: {}", playerId, actionType == 1 ? "过" : "出", cardStr);
        }*/

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
            //log.debug("玩家 {} 过牌，当前连续过牌数: {}", info.getPlayerId(), gameDataVo.getPassCount());
            gameDataVo.getGameLog().recordPass(info.getPlayerId(), info.getSeatId());
            checkNextTurn(info.getPlayerId());
            return;
        }
        // Play Logic
        List<Integer> playCardIds = reqTurnAction.cards;
        if (CollUtil.isEmpty(playCardIds)) {
            log.warn("玩家未出牌：{}", info.getPlayerId());
            return;
        }

        Map<Integer, PokerCard> cardMap = ToSouthFreeDataHelper.getCardListMap(ToSouthFreeDataHelper.getPoolId(gameDataVo));

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
        ToSouthFreeCardType type = ToSouthFreeHandUtils.getCardType(playCards);
        if (type == ToSouthFreeCardType.NONE) {
            log.warn("[南方前进-免费][出牌] 非法牌型 - 玩家: {}, 座位: {}, 牌: {}",
                    playerId, info.getSeatId(), ToSouthFreeHandUtils.cardListToString(playCards));
            return;
        }
        //log.info("[南方前进-免费][出牌] 玩家: {}, 座位: {}, 牌型: {}, 牌: {}",
        //        playerId, info.getSeatId(), type, ToSouthFreeHandUtils.cardListToString(playCards));

        // 确定出牌子类型（在 lastPlayCards 更新前判断）：首出 / 跟牌 / 新一轮出牌
        boolean isFirstPlayerNow = isFirstPlayer(info.getSeatId());
        boolean isGameFirstPlay = isFirstPlayerNow && gameDataVo.isFirstRound();

        // 3. 牌型比较
        if (gameDataVo.getRoundLeaderSeatId() != info.getSeatId()) {
            List<Integer> lastCardIds = gameDataVo.getLastPlayCards();
            List<Card> lastCards = playCardsIdsToCards(lastCardIds, cardMap);
            ToSouthFreeCardType lastType = ToSouthFreeHandUtils.getCardType(lastCards);
            //log.info("[南方前进-免费][比牌] 上家牌型: {}, 上家牌: {} | 当前牌型: {}, 当前牌: {}",
            //        lastType, ToSouthFreeHandUtils.cardListToString(lastCards),
            //        type, ToSouthFreeHandUtils.cardListToString(playCards));
            if (!ToSouthFreeHandUtils.compare(lastCards, playCards)) {
                log.warn("[南方前进-免费][比牌] 管不上 - 玩家: {}, {} [{}] 无法压过 {} [{}]",
                        playerId,
                        ToSouthFreeHandUtils.cardListToString(playCards), type,
                        ToSouthFreeHandUtils.cardListToString(lastCards), lastType);
                return;
            }
            //log.info("[南方前进-免费][比牌] 管牌成功 - 玩家: {}, {} [{}] 压过 {} [{}]",
            //        playerId,
            //        ToSouthFreeHandUtils.cardListToString(playCards), type,
            //        ToSouthFreeHandUtils.cardListToString(lastCards), lastType);
        } else {
            //log.info("[南方前进-免费][首出] 玩家: {}, 座位: {}, 牌型: {}, 牌: {}",
            //        playerId, info.getSeatId(), type, ToSouthFreeHandUtils.cardListToString(playCards));
        }

        info.getCurrentCards().removeAll(realPlayCardIds);

        // 出牌后重新排序剩余手牌并更新高亮牌
        if (!info.getCurrentCards().isEmpty()) {
            List<Card> remainingCards = info.getCurrentCards().stream()
                    .map(cardMap::get)
                    .collect(Collectors.toList());
            List<Integer> highlightIds = ToSouthFreeHandUtils.sortAndGetHighlightCards(remainingCards);

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
        gameDataVo.getCurrentRoundPlays().add(new ToSouthFreeRoundRecord(info.getSeatId(), realPlayCardIds, playCardIds, type));
        // 记录出牌到一局日志（区分 首出 / 跟牌 / 新一轮出牌）
        playCards.sort(ToSouthFreeHandUtils.CARD_COMPARATOR);
        String cardsStr = ToSouthFreeHandUtils.cardListToString(playCards);
        int remain = info.getCurrentCards().size();
        if (isGameFirstPlay) {
            gameDataVo.getGameLog().recordFirstPlay(info.getPlayerId(), info.getSeatId(), type.name(), cardsStr, remain);
        } else if (isFirstPlayerNow) {
            gameDataVo.getGameLog().recordNewRoundPlay(info.getPlayerId(), info.getSeatId(), type.name(), cardsStr, remain);
        } else {
            gameDataVo.getGameLog().recordFollowPlay(info.getPlayerId(), info.getSeatId(), type.name(), cardsStr, remain);
        }
        /*if (log.isDebugEnabled()) {
            //log.debug("玩家 {} 出牌成功 - 类型: {}, 牌: {}, 剩余手牌: {}", info.getPlayerId(), type, ToSouthFreeHandUtils.cardListToString(playCards), info.getCurrentCards().size());
        }*/
        if (info.getCurrentCards().isEmpty()) {
//            log.info("玩家 {} 胜利 (出完手牌)，游戏结束", info.getPlayerId());
            info.setOver(true);

            // 先广播最后一手出牌信息给所有玩家，再进行结算
            broadcastLastAction(info.getPlayerId());

            // 如果最后一手牌是炸弹，需要先处理炸弹结算
            if (isBomb(type)) {
                processBombSettlement(info.getSeatId());
            }

            // 触发结算逻辑
            ToSouthFreeSettlementContext context = new ToSouthFreeSettlementContext();
            context.setInstantWin(false);
            context.addItem(new ToSouthFreeSettlementContext.SettlementItem(
                    info,
                    true,
                    0,
                    null
            ));
            addPokerPhaseTimer(new ToSouthFreeSettlementPhase(this, context));
            return;
        }

        checkNextTurn(0);
    }

    /**
     * 结算当前轮中的炸弹赔付
     *
     * @param winnerSeatId
     */
    private void processBombSettlement(int winnerSeatId) {
        List<ToSouthFreeRoundRecord> plays = gameDataVo.getCurrentRoundPlays();
        if (CollUtil.isEmpty(plays)) return;

        ToSouthFreeRoundRecord lastPlay = plays.getLast();
        if (lastPlay.seatId != winnerSeatId) {
            return;
        }
        if (!isBomb(lastPlay.cardType)) {
            return;
        }

        // 打印本轮完整出牌记录
        /*if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("[南方前进-免费][炸弹结算] 开始处理 - 赢家座位: ")
                    .append(winnerSeatId).append(", 最后牌型: ").append(lastPlay.cardType)
                    .append("\n  本轮plays(共").append(plays.size()).append("条):");
            for (int i = 0; i < plays.size(); i++) {
                ToSouthFreeRoundRecord r = plays.get(i);
                sb.append("\n    [").append(i).append("] seat=").append(r.seatId)
                        .append(" type=").append(r.cardType)
                        .append(" cards=").append(r.cards);
            }
            log.debug(sb.toString());
        }*/

        // 炸弹链
        List<ToSouthFreeRoundRecord> bombChain = new ArrayList<>();
        int victimIndex = -1;

        // 从后往前遍历，收集连续的炸弹
        // 炸弹链的中断条件：
        // 1. 遇到非炸弹牌 (这就是被炸的牌)
        // 2. 遍历完列表
        for (int i = plays.size() - 1; i >= 0; i--) {
            ToSouthFreeRoundRecord record = plays.get(i);
            if (isBomb(record.cardType)) {
                bombChain.addFirst(record);
            } else {
                victimIndex = i;
                break; // 找到被炸的牌，停止
            }
        }

        // 打印扫描结果
        /*if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("[南方前进-免费][炸弹结算] 扫描结果 - victimIndex=")
                    .append(victimIndex).append(", bombChain(共").append(bombChain.size()).append("条):");
            for (int i = 0; i < bombChain.size(); i++) {
                ToSouthFreeRoundRecord r = bombChain.get(i);
                sb.append("\n    [").append(i).append("] seat=").append(r.seatId)
                        .append(" type=").append(r.cardType);
            }
            if (victimIndex >= 0) {
                ToSouthFreeRoundRecord v = plays.get(victimIndex);
                sb.append("\n  被炸牌: seat=").append(v.seatId).append(" type=").append(v.cardType);
            }
            log.debug(sb.toString());
        }*/

        if (bombChain.isEmpty()) return;

        // 炸弹牌可以由首个出牌的玩家打出去，打出去没有单独得分
        if (victimIndex == -1 && bombChain.size() == 1) {
            log.debug("首出炸弹且无人压制，不触发额外结算");
            // 这里不能return，因为即便是首出炸弹，也可能触发新的一轮
            // 但是当前函数只负责结算，所以return没问题，逻辑推进由 checkNextTurn 负责
            return;
        }

        ToSouthFreeRoundRecord winnerRecord = bombChain.getLast();
        long winnerId = Objects.requireNonNull(getPlayerBySeatId(winnerRecord.seatId)).getPlayerId();
        long baseBet = gameDataVo.getRoomBet();
        Map<Integer, PokerCard> cardMap = ToSouthFreeDataHelper.getCardListMap(ToSouthFreeDataHelper.getPoolId(gameDataVo));
        SouthernMoneyCfg moneyCfg = ToSouthFreeDataHelper.getSouthernMoneyCfg(gameDataVo);
        if (moneyCfg == null) {
            log.error("缺少SouthernMoneyCfg配置，跳过炸弹结算");
            return;
        }

        List<ToSouthFreeBombDetail> details = new ArrayList<>();

        // 守卫：如果被炸的牌不是可炸牌型（非2单张/对子、非炸弹、非连对），不触发结算
        // 普通单张（非2）、对子（非2）、三张、顺子等均不能被炸
        if (victimIndex != -1) {
            ToSouthFreeRoundRecord victimRecord = plays.get(victimIndex);
            List<Card> victimCards = playCardsIdsToCards(victimRecord.cards, cardMap);
            victimCards.sort(ToSouthFreeHandUtils.CARD_COMPARATOR);
            boolean allRank2 = !victimCards.isEmpty() && victimCards.stream().allMatch(c -> c.getRank() == RANK_2);
            boolean bombable = isBomb(victimRecord.cardType) || allRank2;
            if (!bombable) {
                log.warn("被炸的牌型不可被炸，跳过结算 seatId={}, cardType={}, cards={}",
                        victimRecord.seatId, victimRecord.cardType, victimRecord.cards);
                return;
            }
        }

        List<ToSouthFreeRoundRecord> settledRecords = new ArrayList<>();
        if (victimIndex != -1) {
            // 从被直接炸的牌（victimIndex）往前，连续回溯所有rank-2牌，全部纳入赔付
            // 规则：输家须为自己打出的2以及链路上所有2型牌负责
            // 例：A单3 → B黑2 → C红2 → A四条5 → B四条6 → C四连对
            //   settledRecords = [B黑2(2), C红2(4), A四条5(8), B四条6(8)]，B赔22倍
            for (int i = victimIndex; i >= 0; i--) {
                ToSouthFreeRoundRecord record = plays.get(i);
                List<Card> cards = playCardsIdsToCards(record.cards, cardMap);
                boolean allRank2 = !cards.isEmpty() && cards.stream().allMatch(c -> c.getRank() == RANK_2);
                if (allRank2) {
                    settledRecords.add(0, record); // 头插，保持时间顺序（早→晚）
                } else {
                    break; // 遇到非rank-2牌（如普通单张），停止回溯
                }
            }
        }
        if (bombChain.size() > 1) {
            // 连炸链：累计所有被炸的炸弹牌型（不含赢家自身出的炸弹）
            settledRecords.addAll(bombChain.subList(0, bombChain.size() - 1));
        }

        log.debug("[南方前进-免费][炸弹结算] settledRecords共{}条: {}", settledRecords.size(),
                settledRecords.stream().map(r -> "seat" + r.seatId + ":" + r.cardType).toList());

        if (CollUtil.isNotEmpty(settledRecords)) {
            long totalMultiplier = 0;
            for (ToSouthFreeRoundRecord settledRecord : settledRecords) {
                long multiplier = getBombSettlementMultiplier(settledRecord, cardMap, moneyCfg);
                if (multiplier <= 0) {
                    log.warn("炸弹结算未命中赔付规则 seatId={}, cardType={}, cards={}",
                            settledRecord.seatId, settledRecord.cardType, settledRecord.cards);
                    continue;
                }
                log.debug("[南方前进-免费][炸弹结算] seat={} type={} 倍数={}",
                        settledRecord.seatId, settledRecord.cardType, multiplier);
                totalMultiplier += multiplier;
            }

            long victimId;
            int detailType;
            if (bombChain.size() >= 2) {
                ToSouthFreeRoundRecord secondLast = bombChain.get(bombChain.size() - 2);
                victimId = Objects.requireNonNull(getPlayerBySeatId(secondLast.seatId)).getPlayerId();
                detailType = 2;
            } else {
                // 单炸：被直接炸的玩家（victimIndex处）是输家，而非settledRecords最前面的玩家
                ToSouthFreeRoundRecord directVictim = victimIndex >= 0 ? plays.get(victimIndex) : settledRecords.getFirst();
                victimId = Objects.requireNonNull(getPlayerBySeatId(directVictim.seatId)).getPlayerId();
                detailType = 1;
            }

            long score = baseBet * totalMultiplier;
            if (score > 0 && victimId != winnerId) {
                log.debug("炸弹结算 - 赢家: {}, 输家: {}, 被炸数量: {}, 总倍数: {}, 金额: {}, 连炸: {}",
                        winnerId, victimId, settledRecords.size(), totalMultiplier, score, bombChain.size() >= 2);
                addBombScore(details, victimId, winnerId, score, detailType);
                // 记录炸弹结算到一局日志（税后赢分从 details 中取）
                long winScore = 0;
                for (ToSouthFreeBombDetail d : details) {
                    if (d.playerId == winnerId && d.type == ToSouthFreeConstant.BOMB_WIN_TYPE) {
                        winScore = d.score;
                    }
                }
                gameDataVo.getGameLog().recordBombSettlement(winnerId, victimId, score, winScore, bombChain.size());
            }
        }

        if (CollUtil.isNotEmpty(details)) {
            NotifyToSouthFreeBombSettlement notify = new NotifyToSouthFreeBombSettlement();
            notify.details = details;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
        }
    }

    private void addBombScore(List<ToSouthFreeBombDetail> details, long loserId, long winnerId, long score, int type) {
        // 直接扣除输家积分
        // 炸弹扣钱
        deductItem(loserId, score, AddType.GAME_SETTLEMENT, "ToSouthFree bomb loses money", false);
        details.add(new ToSouthFreeBombDetail(loserId, score, ToSouthFreeConstant.BOMB_LOSE_TYPE));

        // 计算赢家税后积分并添加
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        long tax = BigDecimal.valueOf(score)
                .multiply(BigDecimal.valueOf(roomCfg.getWinRatio()))
                .divide(GameConstant.TEN_THOUSAND_BD, RoundingMode.DOWN).longValue();
        gameDataTracker.addGameLogData("tax", tax);
        long finalWinScore = score - tax;

        addItem(winnerId, finalWinScore, AddType.GAME_SETTLEMENT);
        details.add(new ToSouthFreeBombDetail(winnerId, finalWinScore, ToSouthFreeConstant.BOMB_WIN_TYPE));
    }

    /**
     * 计算被炸牌型的赔付倍数（中局赔付，读 被管炸弹 配置而非结算剩余配置）
     * 2的牌型按每张单独计算：红2 = remainred2 倍，黑2 = remainblack2 倍，累加
     * 炸弹牌型读配置：四条=fourkindboom，三连对=remainBoom，四连对=fourpairsboom
     */
    private long getBombSettlementMultiplier(ToSouthFreeRoundRecord victimRecord,
                                             Map<Integer, PokerCard> cardMap,
                                             SouthernMoneyCfg moneyCfg) {
        List<Card> victimCards = playCardsIdsToCards(victimRecord.cards, cardMap);
        if (CollUtil.isEmpty(victimCards)) return 0;

        victimCards.sort(ToSouthFreeHandUtils.CARD_COMPARATOR);

        // 2的牌型：每张单独计算（红2 * remainred2 + 黑2 * remainblack2）
        // 适用 SINGLE / PAIR / TRIPLE 中全为2的情况
        boolean allRank2 = victimCards.stream().allMatch(c -> c.getRank() == RANK_2);
        if (allRank2 && (victimRecord.cardType == ToSouthFreeCardType.SINGLE
                || victimRecord.cardType == ToSouthFreeCardType.PAIR
                || victimRecord.cardType == ToSouthFreeCardType.TRIPLE)) {
            long multi = 0;
            for (Card c : victimCards) {
                boolean isRed = c.getSuit() == HEART_SUIT || c.getSuit() == DIAMOND_SUIT;
                multi += isRed ? moneyCfg.getRemainred2() : moneyCfg.getRemainblack2();
            }
            return multi;
        }

        // 被炸的炸弹：读被管炸弹配置（非结算剩余配置）
        if (victimRecord.cardType == ToSouthFreeCardType.BOMB_QUAD) {
            return moneyCfg.getFourkindboom();
        }
        if (victimRecord.cardType == ToSouthFreeCardType.CONSECUTIVE_PAIRS) {
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

    private boolean isBomb(ToSouthFreeCardType type) {
        return type == ToSouthFreeCardType.BOMB_QUAD || type == ToSouthFreeCardType.CONSECUTIVE_PAIRS;
    }

    /**
     * 是否为第一个打牌的
     *
     * @return
     */
    private boolean isFirstPlayer(int seatId) {
        return gameDataVo.getLastPlayCards() == null && gameDataVo.getRoundLeaderSeatId() == seatId;
    }

    private List<Card> playCardsIdsToCards(List<Integer> ids, Map<Integer, PokerCard> map) {
        return ids.stream().map(map::get).collect(Collectors.toList());
    }

    private void checkNextTurn(long passerPlayerId) {
        //log.debug("检查下家 - 当前索引: {}", gameDataVo.getIndex());

        PlayerSeatInfo nextPlayer = getNextExePlayer();

        // 如果没有下家，或者下家就是上一个出牌的人（说明其他人都过了/出局），一轮结束
        if (nextPlayer == null || nextPlayer.getSeatId() == gameDataVo.getLastPlaySeatId()) {
            int winnerSeatId = gameDataVo.getLastPlaySeatId();
            PlayerSeatInfo nextLeader = getPlayerBySeatId(winnerSeatId);

            if (nextLeader != null) {
                //log.debug("一轮结束，玩家 {} 获得球权，新一轮开始", nextLeader.getPlayerId());
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
            //log.debug("当前轮继续，下家 {} 出牌", nextPlayer.getPlayerId());
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

    /**
     * 广播第一手牌的出牌信息（游戏结束前，通知所有玩家最终出牌动作）
     * 与 broadcastNextTurn 不同
     * lastPlaySeatId = -1
     *
     * @param
     */
    public void broadcastFirstTurn(long waitPlayerId, boolean canPass) {
//        broadcastNextTurn(waitPlayerId, canPass, 0);
        NotifyToSouthFreeTurnActionInfo notify = new NotifyToSouthFreeTurnActionInfo();
        ToSouthFreeActionInfo actionInfo = new ToSouthFreeActionInfo();
        actionInfo.lastpassUserId = 0;
        actionInfo.waitPlayerId = waitPlayerId;
        actionInfo.canPass = canPass;
        fillCommonActionInfo(actionInfo);
        // 计算等待时间
        long currentTime = System.currentTimeMillis();
        long duration = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.PLAY_CARDS);
        actionInfo.waitEndTime = currentTime + duration;
        // 1. 发给其他人,不携带推荐牌组（非等待玩家不能出牌）
        actionInfo.recommendCardsList = null;
        actionInfo.lastPlaySeatId = -1;
//        actionInfo.canPlay = false;
        notify.actionInfo = actionInfo;

        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.getPlayerId() == waitPlayerId) continue;
            actionInfo.canPlay = false;
            // 为每个接收者设置其自己的手牌
            // 在 Netty/Protobuf 场景下，通常在 write 时会序列化，如果是同步序列化，那么可以复用对象。
            // 但为了绝对安全，这里使用 clone
            ToSouthFreeActionInfo playerActionInfo = cloneActionInfo(actionInfo);
            playerActionInfo.selfHandCards = PokerDataHelper.getClientId(gameDataVo, info.getCurrentCards());
            playerActionInfo.selfHighlightCards = gameDataVo.getPlayerHighlightCards().get(info.getPlayerId());
            playerActionInfo.lastPlaySeatId = -1;
            notify.actionInfo = playerActionInfo;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(info.getPlayerId(), notify));
        }
        Map<Long, PlayerSeatInfo> playerSeatInfoMap = gameDataVo.getPlayerSeatInfoMap();
        PlayerSeatInfo waitPlayer = playerSeatInfoMap.get(waitPlayerId);
        // 计算推荐出牌 (仅针对等待玩家) 发给当前操作玩家 (带 recommend)
        fillRecommendCards(actionInfo, waitPlayer);

        ToSouthFreeActionInfo waitPlayerActionInfo = cloneActionInfo(actionInfo);
        waitPlayerActionInfo.selfHandCards = PokerDataHelper.getClientId(gameDataVo, waitPlayer.getCurrentCards());
        waitPlayerActionInfo.selfHighlightCards = gameDataVo.getPlayerHighlightCards().get(waitPlayerId);

        notify.actionInfo = waitPlayerActionInfo;

        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(waitPlayerId, notify));
    }

    public void broadcastNextTurn(long waitPlayerId, boolean canPass, long passerPlayerId) {
        NotifyToSouthFreeTurnActionInfo notify = new NotifyToSouthFreeTurnActionInfo();
        ToSouthFreeActionInfo actionInfo = new ToSouthFreeActionInfo();
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
            ToSouthFreeActionInfo playerActionInfo = cloneActionInfo(actionInfo);
            playerActionInfo.selfHandCards = PokerDataHelper.getClientId(gameDataVo, info.getCurrentCards());
            playerActionInfo.selfHighlightCards = gameDataVo.getPlayerHighlightCards().get(info.getPlayerId());

            notify.actionInfo = playerActionInfo;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(info.getPlayerId(), notify));
        }
        Map<Long, PlayerSeatInfo> playerSeatInfoMap = gameDataVo.getPlayerSeatInfoMap();
        PlayerSeatInfo waitPlayer = playerSeatInfoMap.get(waitPlayerId);
        // 计算推荐出牌 (仅针对等待玩家) 发给当前操作玩家 (带 recommend)
        fillRecommendCards(actionInfo, waitPlayer);

        ToSouthFreeActionInfo waitPlayerActionInfo = cloneActionInfo(actionInfo);
        waitPlayerActionInfo.selfHandCards = PokerDataHelper.getClientId(gameDataVo, waitPlayer.getCurrentCards());
        waitPlayerActionInfo.selfHighlightCards = gameDataVo.getPlayerHighlightCards().get(waitPlayerId);

        notify.actionInfo = waitPlayerActionInfo;

        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(waitPlayerId, notify));
    }

    /**
     * 广播最后一手牌的出牌信息（游戏结束前，通知所有玩家最终出牌动作）
     * 与 broadcastNextTurn 不同：没有下一个等待玩家，不需要推荐牌组
     *
     * @param winnerPlayerId 赢家玩家ID，用作 waitPlayerId 让前端正常展示出牌信息
     */
    private void broadcastLastAction(long winnerPlayerId) {
        NotifyToSouthFreeTurnActionInfo notify = new NotifyToSouthFreeTurnActionInfo();
        ToSouthFreeActionInfo actionInfo = new ToSouthFreeActionInfo();
        actionInfo.lastpassUserId = 0;
        actionInfo.waitPlayerId = winnerPlayerId;
        actionInfo.canPass = false;
        actionInfo.canPlay = false;
        actionInfo.waitEndTime = -1;
        fillCommonActionInfo(actionInfo);
        actionInfo.recommendCardsList = null;

        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            ToSouthFreeActionInfo playerActionInfo = cloneActionInfo(actionInfo);
            playerActionInfo.selfHandCards = PokerDataHelper.getClientId(gameDataVo, info.getCurrentCards());
            playerActionInfo.selfHighlightCards = gameDataVo.getPlayerHighlightCards().get(info.getPlayerId());
            notify.actionInfo = playerActionInfo;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(info.getPlayerId(), notify));
        }
    }

    // 当前轮玩家公开信息
    private void fillCurRoundPlayerInfos(ToSouthFreeActionInfo actionInfo) {
        for (PlayerSeatInfo playerSeatInfo : gameDataVo.getPlayerSeatInfoList()) {
            ToSouthFreeCurRoundPlayerInfo curRoundPlayerInfo = new ToSouthFreeCurRoundPlayerInfo();
            curRoundPlayerInfo.playerId = playerSeatInfo.getPlayerId();
            curRoundPlayerInfo.seatId = playerSeatInfo.getSeatId();
            curRoundPlayerInfo.passed = gameDataVo.getCurRoundPassedPlayerSeats().contains(curRoundPlayerInfo.seatId);
            curRoundPlayerInfo.cardCount = playerSeatInfo.getCurrentCards().size();
            actionInfo.curRoundPlayerInfos.add(curRoundPlayerInfo);
        }
    }

    // 本轮出牌历史
    private void fillCurRoundPlayedCardsHistory(ToSouthFreeActionInfo actionInfo) {
        if (gameDataVo.getCurrentRoundPlays().isEmpty()) {
            return;
        }
        // 本轮出牌历史
        actionInfo.curRoundPlayedCardHistory = gameDataVo.getCurrentRoundPlays().stream().map(play -> {
            ToSouthFreePlayCardRecord playCardRecord = new ToSouthFreePlayCardRecord();
            playCardRecord.seatId = play.seatId;
            playCardRecord.playedCards = play.getCardClientIds();
            return playCardRecord;
        }).toList();
    }

    /**
     * 填充 ActionInfo 中与轮次状态相关的公共字段，供 broadcastNextTurn 与 respRoomInitInfoAction 共用。
     * 包含：lastPlayCards / lastPlayCardsType / lastPlaySeatId / roundLeaderSeatId / isFirstRound
     * curRoundPlayerInfos / curRoundPlayedCardHistory
     * <p>
     * lastPlaySeatId / lastPlayCards 的语义是「上一个动作的玩家」：
     * - 若上一动作是 pass（lastpassUserId != 0）：lastPlaySeatId = pass 玩家座位，lastPlayCards 为空
     * - 若上一动作是出牌（lastpassUserId == 0）：同步 gameDataVo 中真正出牌玩家的牌型与座位
     */
    private void fillCommonActionInfo(ToSouthFreeActionInfo actionInfo) {
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
     *
     * @param actionInfo
     * @param waitPlayer
     */
    private void fillRecommendCards(ToSouthFreeActionInfo actionInfo, PlayerSeatInfo waitPlayer) {
        if (waitPlayer == null) return;

        List<ToSouthFreeRecommendCards> recommendCardsList = new ArrayList<>();
        Map<Integer, PokerCard> cardMap = ToSouthFreeDataHelper.getCardListMap(ToSouthFreeDataHelper.getPoolId(gameDataVo));
        List<Card> handCards = waitPlayer.getCurrentCards().stream()
                .map(cardMap::get)
                .collect(Collectors.toList());

        if (isFirstPlayer(waitPlayer.getSeatId())) {
            // 首出推荐
            Map<Integer, List<Card>> rankMap = ToSouthFreeHandUtils.convertCardListToRankMap(handCards);
            if (gameDataVo.isFirstRound()) {
                // 首局首出 (找含黑桃3的)
                List<Card> best = handCards.stream()
                        .filter(c -> c.getRank() == RANK_3 && c.getSuit() == SPADE_SUIT)
                        .findFirst()
                        .map(spade3 -> ToSouthFreeHandUtils.findBestPlayWithFirstCard(rankMap, spade3))
                        .orElse(null);
                if (CollUtil.isNotEmpty(best)) {
                    List<Integer> ids = ToSouthFreeDataHelper.getClientId(gameDataVo, best.stream().map(c -> ((PokerCard) c).getPokerPoolId()).collect(Collectors.toList()));
                    recommendCardsList.add(new ToSouthFreeRecommendCards(ids));
                }
            } else {
                // 普通首出 (获取所有可能)
                List<List<Card>> allBestPlays = ToSouthFreeHandUtils.findAllBestPlays(handCards);
                for (List<Card> play : allBestPlays) {
                    List<Integer> ids = ToSouthFreeDataHelper.getClientId(gameDataVo, play.stream().map(c -> ((PokerCard) c).getPokerPoolId()).collect(Collectors.toList()));
                    recommendCardsList.add(new ToSouthFreeRecommendCards(ids));
                }
            }
        } else {
            // 跟牌推荐 (获取所有可能)
            List<Integer> lastIds = gameDataVo.getLastPlayCards();
            if (CollUtil.isNotEmpty(lastIds)) {
                List<Card> lastCards = lastIds.stream().map(cardMap::get).collect(Collectors.toList());
                List<List<Card>> allPlays = ToSouthFreeHandUtils.findAllFollowPlays(handCards, lastCards);

                for (List<Card> play : allPlays) {
                    List<Integer> ids = ToSouthFreeDataHelper.getClientId(gameDataVo, play.stream().map(c -> ((PokerCard) c).getPokerPoolId()).collect(Collectors.toList()));
                    recommendCardsList.add(new ToSouthFreeRecommendCards(ids));
                }
            }
        }

        if (CollUtil.isNotEmpty(recommendCardsList)) {
            actionInfo.recommendCardsList = recommendCardsList;
        }
        actionInfo.canPlay = CollUtil.isNotEmpty(recommendCardsList);
    }

    private ToSouthFreeActionInfo cloneActionInfo(ToSouthFreeActionInfo source) {
        ToSouthFreeActionInfo target = new ToSouthFreeActionInfo();
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

    /**
     * 重写基类 respRoomInitInfo：修正玩家加入时广播的 playerStatus
     * 基类用 seatInfo.isJoinGame() 作为 playerStatus，新玩家 isJoinGame()=false 导致推送 playerStatus=false
     * 这里改为显式设置 playerStatus=true（表示玩家在房间）
     */
    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerController.playerId());
        gamePlayer.getPokerPlayerGameData().setInit(true);
        respRoomInitInfoAction(playerController);
        // 通知其他玩家：玩家加入，playerStatus=true
        NotifyPokerPlayerChange playerChange = new NotifyPokerPlayerChange();
        PokerPlayerInfo info = PokerBuilder.buildPlayerInfo(gamePlayer, null, this);
        info.playerStatus = true;
        playerChange.pokerPlayerInfo = info;
        playerChange.totalNum = gameDataVo.getGamePlayerMap().size();
        roomController.broadcastToPlayers(RoomMessageBuilder.newBuilder()
                .sendAllPlayer(playerChange).exceptPlayer(playerController.playerId()));
        // 尝试开启游戏
        tryStartNextGame();
    }

    @Override
    public void respRoomInitInfoAction(PlayerController playerController) {
        //log.debug("响应南方前进-免费房间信息 - 玩家: {}", playerController.playerId());
        RespToSouthFreeRoomBaseInfo baseInfo = new RespToSouthFreeRoomBaseInfo(Code.SUCCESS);
        baseInfo.phase = getCurrentGamePhase();
        if (playerController.getPlayer().getRoomId() > 0 && playerController.getScene() instanceof AbstractRoomController<?, ?> roomController) {
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomController.getRoom().getRoomCfgId());
            baseInfo.roomBet = warehouseCfg.getBetShow();
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
            ToSouthFreeActionInfo actionInfo = new ToSouthFreeActionInfo();
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
            RespToSouthFreeSendCardsInfo sendCardsInfo = new RespToSouthFreeSendCardsInfo();
            sendCardsInfo.sortedHandCards = sortedHandCards;
            sendCardsInfo.originalHandCards = new ArrayList<>(sortedHandCards);
            Collections.shuffle(sendCardsInfo.originalHandCards);
            sendCardsInfo.highlightCards = highlightCards;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerController.playerId(), sendCardsInfo));
        }
    }

    /**
     * 南方前进-免费请求准备/取消准备（在 WAIT_READY 阶段，四人全部准备后才开始发牌）
     *
     * @param playerId 玩家id
     * @param req      请求（status: 1=准备, 2=取消）
     */
    public void reqToSouthFreeGoReady(long playerId, ReqToSouthFreeGoReady req) {
        NotifyToSouthFreePlayerReady notify = new NotifyToSouthFreePlayerReady();
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

        // 容错：玩家被踢出通知后（exitPlayerIds），若仍在座位上又点准备，撤销踢出标记让其继续
        if (status == 1 && gameDataVo.getExitPlayerIds().contains(playerId)) {
            //log.info("玩家 {} 已被踢出通知但仍在座位上，重新点准备，撤销踢出标记", playerId);
            gameDataVo.getExitPlayerIds().remove(playerId);
            gameDataVo.getReadyPlayerIds().remove(playerId);
        }

        if (status == 2) {
            // 取消准备
            if (!gameDataVo.getReadyPlayerIds().contains(playerId)) {
                notify.code = Code.REPEAT_OP;
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notify));
                return;
            }
            gameDataVo.getReadyPlayerIds().remove(playerId);
            //log.info("玩家 {} 取消准备，当前准备人数: {}", playerId, gameDataVo.getReadyPlayerIds().size());
            notify.playerId = playerId;
            notify.status = 2;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
            //取消准备加入准备倒计时
            scheduleReadyTimeout(playerId, READY_TIMEOUT);
        } else {
            if (isOpen()) {
                // 准备（status == 1 或默认）
                if (gameDataVo.getReadyPlayerIds().contains(playerId)) {
                    // 容错：玩家已准备且仍在座位上（未真正退出房间），返回准备成功
                    if (!gameDataVo.getExitPlayerIds().contains(playerId) && playerSeatInfo != null && playerSeatInfo.isSeatDown()) {
                        //log.info("玩家 {} 重复准备请求，但仍在房间且已准备，容错返回准备成功", playerId);
                        notify.playerId = playerId;
                        notify.status = 1;
                        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notify));
                        return;
                    }
                    notify.code = Code.REPEAT_OP;
                    broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notify));
                    return;
                }
                WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(getRoom().getRoomCfgId());
                long minBalance = warehouseCfg.getEnterLimit();
                List<Long> insufficientPlayerIds = new ArrayList<>();
                for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
                    if (!info.isSeatDown()) continue;
                    long pid = info.getPlayerId();
                    long playerBalance = getTransactionItemNum(pid);
                    if (playerBalance < minBalance) {
                        RoomPlayer roomPlayer = getRoomController().getRoomPlayer(pid);
                        if (roomPlayer == null || roomPlayer.isOnline()) {
                            NotifyExitRoom exitNotify = new NotifyExitRoom();
                            exitNotify.langId = Code.USER_NOT_GOLD;
                            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(pid, exitNotify));
                            //log.info("玩家 {} 余额不足，通知客户端退出房间", pid);
                        } else {
                            getRoomController().getRoomManager().exitRoom(pid);
                            //log.info("玩家 {} 余额不足且离线，服务端直接退出房间", pid);
                        }
                        gameDataVo.getReadyTimerVersion().remove(pid);
                        return;
                    }
                }

                gameDataVo.getReadyPlayerIds().add(playerId);
                //log.info("玩家 {} 准备完成，当前准备人数: {}", playerId, gameDataVo.getReadyPlayerIds().size());
                notify.playerId = playerId;
                notify.status = 1;
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
                // 检查是否满足开局条件（人数足够 + 全部准备）
                tryStartGame();
            } else {
                //没开放
                notify.code = Code.GAME_IS_MAINTAIN;
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notify));
                return;
            }
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
            //log.info("广播阶段变化：WAIT_READY，等待玩家准备");

            // 2. 机器人按 delayTime 延迟调度准备（不再自动准备）
            for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
                if (!info.isSeatDown()) continue;
                long pid = info.getPlayerId();
                if (gameDataVo.getReadyPlayerIds().contains(pid)) continue;
                if (gameDataVo.getReadyTimerScheduled().contains(pid)) continue;
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(pid);
                if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
                    scheduleRobotReady(robotPlayer);
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
                    scheduleReadyTimeout(gamePlayer.getId(), READY_TIMEOUT);
                }
            }
        } else {
            // 非首次调用（玩家点击准备后触发）：补充机器人调度和倒计时
            for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
                if (!info.isSeatDown()) continue;
                long pid = info.getPlayerId();
                if (gameDataVo.getReadyPlayerIds().contains(pid)) continue;
                if (gameDataVo.getReadyTimerScheduled().contains(pid)) continue;
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(pid);
                if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
                    scheduleRobotReady(robotPlayer);
                } else {
                    scheduleReadyTimeout(pid, READY_TIMEOUT);
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
        long minBalance = warehouseCfg.getEnterLimit();
        List<Long> insufficientPlayerIds = new ArrayList<>();
        for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
            if (!info.isSeatDown()) continue;
            long pid = info.getPlayerId();
            long playerBalance = getTransactionItemNum(pid);
            if (playerBalance < minBalance) {
                //log.info("玩家 {} 资金不足，当前: {}, 需要: {}, 踢出房间", pid, playerBalance, minBalance);
                insufficientPlayerIds.add(pid);
            }
        }
        if (!insufficientPlayerIds.isEmpty()) {
            // 在线玩家仅发通知，实际断开前仍计入 getSeatDownNum，需手动记录偏差
            int onlineKickedCount = 0;
            for (Long playerId : insufficientPlayerIds) {
                RoomPlayer roomPlayer = getRoomController().getRoomPlayer(playerId);
                if (roomPlayer == null || roomPlayer.isOnline()) {
                    NotifyExitRoom exitNotify = new NotifyExitRoom();
                    exitNotify.langId = Code.USER_NOT_GOLD;
                    broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, exitNotify));
                    //log.info("玩家 {} 余额不足，通知客户端退出房间", playerId);
                    if (roomPlayer != null) {
                        onlineKickedCount++; // 在线玩家未立即断开，仍占 seatDown 位置
                    }
                } else {
                    getRoomController().getRoomManager().exitRoom(playerId);
                    //log.info("玩家 {} 余额不足且离线，服务端直接退出房间", playerId);
                }
                gameDataVo.getReadyTimerVersion().remove(playerId);
            }
            // 踢人后重新检查人数（在线踢人未立即断开，需从统计中扣除）
            int remaining = gameDataVo.getSeatDownNum() - onlineKickedCount;
            if (remaining < roomCfg.getMinPlayer()) {
                //log.info("资金检查后人数不足 ({}/{}), 无法开局", remaining, roomCfg.getMinPlayer());
                return false;
            }
        }

        addPokerPhaseTimer(new ToSouthFreeStartGamePhase(this, gameDataVo.getId()));
        //log.info("全部玩家已准备，开始游戏 当前id{} roomId:{}", gameDataVo.getId(), roomController.getRoom().getId());
        return true;
    }

    /**
     * 为真实玩家启动10秒准备倒计时，超时未准备则踢出房间
     */
    public void scheduleReadyTimeout(long playerId, long timeOut) {
        gameDataVo.getReadyTimerVersion().remove(playerId);
        gameDataVo.getReadyTimerScheduled().add(playerId);
        long exeTime = System.currentTimeMillis() + timeOut;
        gameDataVo.getReadyTimerVersion().put(playerId, exeTime);
        ToSouthFreeReadyTimeoutHandler handler = new ToSouthFreeReadyTimeoutHandler(playerId, gameDataVo.getId(), exeTime, roomController);
        TimerEvent<IProcessorHandler> timerEvent = new TimerEvent<>(this, exeTime, handler);
        addGameTimeEvent(timerEvent, RoomEventType.ROOM_PHASE_RUN_EVENT);
        //log.info("玩家 {} 准备倒计时开始 ({}秒), exeTime={}", playerId, timeOut / 1000, exeTime);
    }

    /**
     * 机器人按 delayTime 延迟调度准备/退出
     * <p>
     * 对局结束瞬间根据 lastWin + continueAfterVictory/continueAfterFail 权重预判留守或退出，
     * 但执行（准备或退出）均在等待 delayTime 延迟后才真正发生。
     *
     * @param robotPlayer 机器人玩家
     */
    private void scheduleRobotReady(GameRobotPlayer robotPlayer) {
        long pid = robotPlayer.getId();
        int actionId = robotPlayer.getActionId();
        ChessRobotCfg cfg = (actionId > 0) ? GameDataManager.getChessRobotCfg(actionId) : null;
        // 延迟时间：优先从配置读取，无配置则兜底 2-5 秒
        int delay;
        if (cfg != null) {
            delay = RobotScheduleUtil.getChessExecutionDelay(actionId);
        } else {
            delay = RandomUtils.nextInt(2000, 5000);
            log.warn("机器人 {} actionId={} 无 ChessRobotCfg 配置，使用兜底延迟 {}ms", pid, actionId, delay);
        }
        // 准备概率：首次进入100%；续局根据胜负读取权重
        int pro;
        if (robotPlayer.getLastWin() == 0 || cfg == null) {
            // 首次进入房间 或 无配置 → 100%准备
            pro = GameConstant.TEN_THOUSAND;
        } else {
            List<Integer> continueList = robotPlayer.getLastWin() == 1
                    ? cfg.getContinueAfterVictory()
                    : cfg.getContinueAfterFail();
            pro = (continueList != null && !continueList.isEmpty()) ? continueList.getFirst() : GameConstant.TEN_THOUSAND;
        }
        ToSouthFreeRobotHandler handler = new ToSouthFreeRobotHandler(robotPlayer, ToSouthFreeRobotHandler.GO_READY, this, pro);
        RobotScheduleUtil.schedule(getRoomController(), handler, delay);
        gameDataVo.getReadyTimerScheduled().add(pid);
        //log.info("机器人 {} 调度准备/退出 delay={}ms, pro={}, lastWin={}, actionId={}", pid, delay, pro, robotPlayer.getLastWin(), actionId);
    }

    /**
     * 机器人准备（由 ToSouthFreeRobotHandler.GO_READY 回调）
     *
     * @param playerId 机器人玩家ID
     */
    public void robotGoReady(long playerId) {
        if (getCurrentGamePhase() != EGamePhase.WAIT_READY) {
            return;
        }
        if (gameDataVo.getReadyPlayerIds().contains(playerId)) {
            return;
        }
        gameDataVo.getReadyPlayerIds().add(playerId);
        NotifyToSouthFreePlayerReady notify = new NotifyToSouthFreePlayerReady();
        notify.playerId = playerId;
        notify.status = 1;
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
        int readyCount = gameDataVo.getReadyPlayerIds().size();
        int seatDownCount = gameDataVo.getSeatDownNum();
        //log.info("机器人 {} 准备完成，广播 NotifyToSouthFreePlayerReady(playerId={}, status=1)，当前准备人数: {}/{}", playerId, playerId, readyCount, seatDownCount);
        tryStartGame();
    }

    /**
     * 机器人退出房间（continueAfter概率未通过时调用）
     *
     * @param playerId 机器人玩家ID
     */
    public void robotExitRoom(long playerId) {
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        if (gamePlayer instanceof GameRobotPlayer) {
            PlayerController pc = getRoomController().getPlayerController(playerId);
            if (pc != null) {
                getRoomController().getRoomManager().robotPlayerExitRoom(List.of(pc));
                //log.info("机器人 {} 退出房间（continueAfter概率未通过）", playerId);
            }
        }
    }

    /**
     * 踢出未准备的玩家
     * 1. 通知被踢玩家退出到大厅（必须在 exitRoom 之前，exitRoom 会清理 GamePlayer 导致无法广播）
     * 2. exitRoom(PlayerController) 真正退出 → 内部触发 onPlayerLeaveRoomAction
     * → broadcastPlayerLeaveChange(playerStatus=false) 通知其他玩家
     */
    public void kickUnreadyPlayer(long playerId) {
        RoomPlayer roomPlayer = getRoomController().getRoomPlayer(playerId);
        if (gameDataVo.getExitPlayerIds().contains(playerId)) {
            getRoomController().getRoomManager().exitRoom(playerId);
            //log.info("玩家 {} 离线且未准备，服务端强制退出房间", playerId);
            return;
        }
        if (roomPlayer == null || roomPlayer.isOnline()) {
            NotifyExitRoom exitNotify = new NotifyExitRoom();
            exitNotify.langId = gameDataVo.getRoomCfg().getEscTipText();
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, exitNotify));
            //log.info("玩家 {} 因未准备，通知客户端退出房间", playerId);
            gameDataVo.getExitPlayerIds().add(playerId);
//            getRoomController().getRoomManager().exitRoom(playerId);
        } else {
            getRoomController().getRoomManager().exitRoom(playerId);
            //log.info("玩家 {} 离线且未准备，服务端直接退出房间", playerId);
        }
        gameDataVo.getReadyTimerVersion().remove(playerId);
        // 不在此处广播 NotifyPokerPlayerChange：
        // - 在线玩家：客户端收到 NotifyExitRoom 断连后，基类 onPlayerLeaveRoom 会自动广播
        // - 离线玩家：上面 exitRoom 已触发基类 onPlayerLeaveRoom 广播
        // 如果在这里再调用 broadcastPlayerLeaveChange，在线场景会导致重复发送两条消息
    }

    /**
     * 玩家（含机器人）落座完成后回调。
     * onJoinRoomSuccessAfter 在 seatInfo 填充完毕后才调用此方法，
     * 此时可以安全地为刚入座的玩家启动准备倒计时或机器人调度。
     */
    @Override
    public void onRobotPlayerJoinRoom(PlayerController playerController, GamePlayer gamePlayer) {
        super.onRobotPlayerJoinRoom(playerController, gamePlayer);
        // 仅在等待准备阶段触发；游戏进行中（出牌/结算等阶段）不处理
        if (getCurrentGamePhase() != EGamePhase.WAIT_READY) {
            return;
        }
        long playerId = gamePlayer.getId();
        // 已准备或已调度过的玩家不重复处理
        if (gameDataVo.getReadyPlayerIds().contains(playerId)
                || gameDataVo.getReadyTimerScheduled().contains(playerId)) {
            return;
        }
        if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
            scheduleRobotReady(robotPlayer);
        } else {
            scheduleReadyTimeout(playerId, READY_TIMEOUT);
            //log.info("玩家 {} 入座后启动准备倒计时", playerId);
        }
    }

    @Override
    public void onPlayerJoinRoomAction(GamePlayer gamePlayer) {
        long playerId = gamePlayer.getId();
        // 玩家每次进入房间时，清除旧的准备倒计时状态，确保 tryStartGame 会重新调度新倒计时
        gameDataVo.getReadyPlayerIds().remove(playerId);
        gameDataVo.getReadyTimerScheduled().remove(playerId);
        gameDataVo.getReadyTimerVersion().remove(playerId);

        // 从Redis加载玩家的连赢/连输和总盈亏数据（跨房间持久化）
        if (!(gamePlayer instanceof GameRobotPlayer)) {
            try {
                ToSouthFreeCardLibManager cardLibManager = CommonUtil.getContext().getBean(ToSouthFreeCardLibManager.class);
                int streak = cardLibManager.getPlayerWinStreak(playerId);
                long totalProfit = cardLibManager.getPlayerTotalProfit(playerId);
                gameDataVo.getPlayerWinStreakMap().put(playerId, streak);
                gameDataVo.getPlayerTotalProfitMap().put(playerId, totalProfit);
                //log.info("玩家 {} 进入房间，加载统计数据 streak={}, totalProfit={}", playerId, streak, totalProfit);
            } catch (Exception e) {
                log.error("加载玩家统计数据异常 playerId={}", playerId, e);
            }
        }
    }

    @Override
    public void onPlayerLeaveRoomAction(RoomPlayer roomPlayer, SeatInfo remove) {
        long playerId = roomPlayer.getPlayerId();
        // 清除该玩家的准备状态
        gameDataVo.getReadyPlayerIds().remove(playerId);
        gameDataVo.getReadyTimerScheduled().remove(playerId);
        gameDataVo.getReadyTimerVersion().remove(playerId);
        // 清除续局状态，有人退出后下一局视为首局（黑桃3先出）
        gameDataVo.setLastGameWinnerPlayerId(0);
        gameDataVo.getLastGamePlayerIds().clear();
        gameDataVo.getExitPlayerIds().remove(playerId);
        // 设置 joinGame=false，基类 onPlayerLeaveRoom 会广播 NotifyPokerPlayerChange，
        // 其中 playerStatus = seatInfo.isJoinGame()，这样基类广播的 playerStatus 就是 false
        remove.setJoinGame(false);

        //log.info("玩家 {} 离开房间，已清除准备状态和续局状态", playerId);
    }

    /**
     * 广播玩家加入变化通知：将加入玩家的状态（playerStatus=true）同步给所有还在房间的其他玩家
     */
    private void broadcastPlayerJoinChange(long playerId) {
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        if (gamePlayer == null) {
            return;
        }
        SeatInfo seatInfo = null;
        for (SeatInfo si : gameDataVo.getSeatInfo().values()) {
            if (si.getPlayerId() == playerId) {
                seatInfo = si;
                break;
            }
        }
        if (seatInfo == null) {
            return;
        }
        NotifyPokerPlayerChange playerChange = new NotifyPokerPlayerChange();
        PokerPlayerInfo info = PokerBuilder.buildPlayerInfo(gamePlayer, seatInfo, this);
        info.playerStatus = true;
        info.status = true;
        playerChange.pokerPlayerInfo = info;
        playerChange.totalNum = gameDataVo.getGamePlayerMap().size();
        broadcastToPlayers(RoomMessageBuilder.newBuilder()
                .sendAllPlayer(playerChange).exceptPlayer(playerId));
        //log.info("已广播玩家 {} 加入状态变化给其他玩家", playerId);
    }

    /**
     * 广播玩家离开变化通知（通过 playerId 查找 SeatInfo）
     * 如果 GamePlayer 或 SeatInfo 已被 exitRoom 清理，则自动跳过避免重复广播
     */
    private void broadcastPlayerLeaveChange(long playerId) {
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        if (gamePlayer == null) {
            return;
        }
        SeatInfo seatInfo = null;
        for (SeatInfo si : gameDataVo.getSeatInfo().values()) {
            if (si.getPlayerId() == playerId) {
                seatInfo = si;
                break;
            }
        }
        if (seatInfo == null) {
            return;
        }
        broadcastPlayerLeaveChange(playerId, seatInfo);
    }

    /**
     * 广播玩家离开变化通知：将离开玩家的状态（playerStatus=false）同步给所有还在房间的真人玩家
     */
    private void broadcastPlayerLeaveChange(long playerId, SeatInfo seatInfo) {
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        if (gamePlayer == null) {
            return;
        }
        NotifyPokerPlayerChange playerChange = new NotifyPokerPlayerChange();
        PokerPlayerInfo info = PokerBuilder.buildPlayerInfo(gamePlayer, seatInfo, this);
        info.playerStatus = false;
        info.status = false;
        playerChange.pokerPlayerInfo = info;
        playerChange.totalNum = gameDataVo.getGamePlayerMap().size();
        broadcastToPlayers(RoomMessageBuilder.newBuilder()
                .sendAllPlayer(playerChange)
                .exceptPlayer(playerId));
        //log.info("已广播玩家 {} 离开状态变化给其他玩家", playerId);
    }

    @Override
    public void onRunGamePlayerLeaveRoom(SeatInfo remove) {
        // 自动走 ToSouthFreeAutoPlayHandler 模块
    }

    @Override
    public void addNextTimer(PlayerSeatInfo nextExePlayer, int sendCardNum) {
        // 给下个玩家增加定时器
        long playerId = nextExePlayer.getPlayerId();
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);

        int operationTime = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.PLAY_CARDS);

        // 如果是机器人，添加机器人处理器
        if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
            int delay = RobotScheduleUtil.getChessExecutionDelay(robotPlayer.getActionId());
            ToSouthFreeAutoPlayHandler handler = new ToSouthFreeAutoPlayHandler(playerId, gameDataVo.getId(), this);
            addPlayerTimer(handler, delay);
        } else {
            // 真实玩家，添加超时自动操作
            addPlayerTimer(new ToSouthFreeAutoPlayHandler(playerId, gameDataVo.getId(), this), operationTime);
        }
    }

    @Override
    public void dealBet(long playerId, ReqPokerBet reqPokerBet) {

    }

    @Override
    protected ToSouthFreeGameDataVo createRoomDataVo(Room_ChessCfg roomCfg) {
        return new ToSouthFreeGameDataVo(roomCfg);
    }

    @Override
    public EGameType gameControlType() {
        return EGameType.TO_SOUTH_FREE;
    }
}
