package com.jjg.game.poker.game.tosouth.room;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.message.req.ReqPokerBet;
import com.jjg.game.poker.game.common.message.req.ReqPokerSampleCardOperation;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.tosouth.gamephase.ToSouthSettlementPhase;
import com.jjg.game.poker.game.tosouth.gamephase.ToSouthStartGamePhase;
import com.jjg.game.poker.game.tosouth.message.bean.ToSouthActionInfo;
import com.jjg.game.poker.game.tosouth.message.bean.ToSouthPlayerInfo;
import com.jjg.game.poker.game.tosouth.message.bean.ToSouthRecommendCards;
import com.jjg.game.poker.game.tosouth.message.req.ReqTurnAction;
import com.jjg.game.poker.game.tosouth.message.resp.RespToSouthRoomBaseInfo;
import com.jjg.game.poker.game.tosouth.message.notify.NotifyToSouthTurnActionInfo;
import com.jjg.game.poker.game.tosouth.room.data.ToSouthGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import com.jjg.game.poker.game.tosouth.message.notify.NotifyToSouthBombSettlement;
import com.jjg.game.poker.game.tosouth.room.data.ToSouthRoundRecord;

import com.jjg.game.poker.game.tosouth.message.bean.ToSouthBombDetail;

import java.util.*;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.tosouth.data.ToSouthDataHelper;
import com.jjg.game.poker.game.tosouth.util.ToSouthCardType;
import com.jjg.game.poker.game.tosouth.util.ToSouthHandUtils;
import java.util.stream.Collectors;

import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.poker.game.tosouth.autohandler.ToSouthAutoPlayHandler;

import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.RANK_3;
import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.SPADE_SUITS;

@GameController(gameType = EGameType.TO_SOUTH, roomType = RoomType.POKER_ROOM)
public class ToSouthGameController extends BasePokerGameController<ToSouthGameDataVo> {

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
        int index = gameDataVo.getIndex();
        for (int i = 1; i < playerSeatInfoList.size(); i++) {
            int newIndex = (index + i) % playerSeatInfoList.size();
            PlayerSeatInfo info = playerSeatInfoList.get(newIndex);
            if (!info.isOver() && !info.isDelState()) {
                gameDataVo.setIndex(newIndex);
                return info;
            }
        }
        return null;
    }

    @Override
    public void sampleCardOperation(long playerId, ReqPokerSampleCardOperation req) {

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
            checkNextTurn();
            return;
        }

        // Play Logic
        List<Integer> playCardIds = reqTurnAction.cards;
        if (CollUtil.isEmpty(playCardIds)) {
            log.warn("玩家未出牌：{}", info.getPlayerId());
            return;
        }
        
        Map<Integer, PokerCard> cardMap = ToSouthDataHelper.getCardListMap(ToSouthDataHelper.getPoolId(gameDataVo));
        
        // 校验出的牌是否在玩家手牌中
        // info.getCurrentCards() 存储的是 ID
        // playCardIds 是 clientId，需要转换
        List<Integer> handClientIds = ToSouthDataHelper.getClientId(gameDataVo, info.getCurrentCards());
        if (!new HashSet<>(handClientIds).containsAll(playCardIds)) {
            log.warn("玩家 {} 出的牌 {} 不属于其手牌 {}", info.getPlayerId(), playCardIds, handClientIds);
            return;
        }

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
        
        List<Card> playCards = playCardsIdsToCards(realPlayCardIds, cardMap);

        // 1. 第一轮黑桃3检测
        if (gameDataVo.isFirstRound() && gameDataVo.getLastPlayCards() == null) {
            boolean hasSpade3 = playCards.stream().anyMatch(c -> c.getRank() == RANK_3 && c.getSuit() == SPADE_SUITS);
            if (!hasSpade3) {
                log.warn("首局首出必须包含黑桃3");
                return;
            }
        }

        // 2. 牌型检查
        ToSouthCardType type = ToSouthHandUtils.getCardType(playCards);
        if (type == ToSouthCardType.NONE) {
            log.warn("非法牌型");
            return;
        }

        // 3. 牌型比较
        if (gameDataVo.getRoundLeaderSeatId() != info.getSeatId()) {
            List<Integer> lastCardIds = gameDataVo.getLastPlayCards();
            List<Card> lastCards = playCardsIdsToCards(lastCardIds, cardMap);
            if (!ToSouthHandUtils.compare(lastCards, playCards)) {
                log.warn("牌型太小，管不上");
                return;
            }
        }

        info.getCurrentCards().removeAll(realPlayCardIds);
        gameDataVo.setLastPlayCards(realPlayCardIds);
        gameDataVo.setLastPlaySeatId(info.getSeatId());
        gameDataVo.setPassCount(0); // 重置过牌计数
        
        // 记录出牌
        gameDataVo.getCurrentRoundPlays().add(new ToSouthRoundRecord(info.getSeatId(), realPlayCardIds, type));
        
        // Log formatted cards
        playCards.sort(ToSouthHandUtils.CARD_COMPARATOR);
        log.debug("玩家 {} 出牌成功 - 类型: {}, 牌: {}, 剩余手牌: {}", info.getPlayerId(), type, ToSouthHandUtils.cardListToString(playCards), info.getCurrentCards().size());

        if (info.getCurrentCards().isEmpty()) {
            log.info("玩家 {} 胜利 (出完手牌)，游戏结束", info.getPlayerId());
            info.setOver(true);
            
            // 如果最后一手牌是炸弹，需要先处理炸弹结算
            if (isBomb(type)) {
                processBombSettlement(info.getSeatId());
            }

            // 触发结算逻辑
            addPokerPhaseTimer(new ToSouthSettlementPhase(this, List.of(info)));
            return;
        }

        checkNextTurn();
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
        
        if (bombChain.isEmpty()) return;

        // 炸弹牌可以由首个出牌的玩家打出去，打出去没有单独得分
        if (victimIndex == -1 && bombChain.size() == 1) {
            return;
        }

        ToSouthRoundRecord winnerRecord = bombChain.getLast();
        long winnerId = Objects.requireNonNull(getPlayerBySeatId(winnerRecord.seatId)).getPlayerId();
        long baseBet = gameDataVo.getRoomBet();

        List<ToSouthBombDetail> details = new ArrayList<>();

        // 第一个被炸的人赔付双倍
        ToSouthRoundRecord firstVictim = null;
        if (victimIndex != -1) {
            firstVictim = plays.get(victimIndex);
        } else {
            // 第一个被炸的出的是炸弹
            if (bombChain.size() > 1) {
                firstVictim = bombChain.getFirst();
            }
        }
        
        if (firstVictim != null) {
            long victimId = Objects.requireNonNull(getPlayerBySeatId(firstVictim.seatId)).getPlayerId();
            if (victimId != winnerId) {
                addBombScore(details, victimId, winnerId, baseBet * 2, 1);
            }
        }

        // 倒数第二个出炸弹的
        if (bombChain.size() >= 2) {
            ToSouthRoundRecord secondLast = bombChain.get(bombChain.size() - 2);
            long secondLastId = Objects.requireNonNull(getPlayerBySeatId(secondLast.seatId)).getPlayerId();
            if (secondLastId != winnerId) {
                addBombScore(details, secondLastId, winnerId, baseBet, 2);
            }
        }
        
        if (CollUtil.isNotEmpty(details)) {
             NotifyToSouthBombSettlement notify = new NotifyToSouthBombSettlement();
             notify.details = details;
             for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
                 RoomMessageBuilder.newBuilder().sendPlayer(info.getPlayerId(), notify);
             }
        }
    }

    private void addBombScore(List<ToSouthBombDetail> details, long loserId, long winnerId, long score, int type) {
        gameDataVo.getBombSettlementMap().merge(loserId, -score, Long::sum);
        gameDataVo.getBombSettlementMap().merge(winnerId, score, Long::sum);
        
        details.add(new ToSouthBombDetail(winnerId, loserId, score, type));
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

    private void checkNextTurn() {
        // 规则：只要有 (人数 - 1) 个人连续 Pass，则一轮结束
        int totalPlayers = gameDataVo.getPlayerSeatInfoList().size();
        int passLimit = totalPlayers - 1;
        log.debug("检查下家 - 连续过牌: {}/{}, 当前索引: {}", gameDataVo.getPassCount(), passLimit, gameDataVo.getIndex());

        if (gameDataVo.getPassCount() >= passLimit) {
            // 新的一轮
            // 下一个出牌的人应该是最后一次出牌的人
            int winnerSeatId = gameDataVo.getLastPlaySeatId();
            PlayerSeatInfo nextLeader = getPlayerBySeatId(winnerSeatId);

            if (nextLeader != null) {
                log.debug("一轮结束，玩家 {} 获得球权，新一轮开始", nextLeader.getPlayerId());
                gameDataVo.setRoundLeaderSeatId(nextLeader.getSeatId());
                gameDataVo.setLastPlayCards(null);
                gameDataVo.setFirstRound(false);
                gameDataVo.setPassCount(0);
                gameDataVo.getCurRoundPassedPlayerSeats().clear(); // 新回合清空过牌列表
                
                // 处理炸弹结算 (如果有的话)
                processBombSettlement(winnerSeatId);
                // 清空本轮出牌记录
                gameDataVo.getCurrentRoundPlays().clear();

                gameDataVo.setIndex(nextLeader.getSeatId());
                broadcastNextTurn(nextLeader.getPlayerId(), gameDataVo.getCurRoundPassedPlayerSeats(), false);
                addNextTimer(nextLeader, 0);
            }
        } else {
            // 继续当前轮，找下家
            PlayerSeatInfo nextPlayer = getNextExePlayer();
            if (nextPlayer != null) {
                log.debug("当前轮继续，下家 {} 出牌", nextPlayer.getPlayerId());
                gameDataVo.setIndex(nextPlayer.getSeatId());
                broadcastNextTurn(nextPlayer.getPlayerId(), gameDataVo.getCurRoundPassedPlayerSeats());
                addNextTimer(nextPlayer, 0);
            }
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

    public void broadcastNextTurn(long waitPlayerId, Set<Integer> curRoundPassedPlayerSeats) {
        broadcastNextTurn(waitPlayerId, curRoundPassedPlayerSeats, true);
    }


    private void broadcastNextTurn(long waitPlayerId, Set<Integer> curRoundPassedPlayerSeats, boolean canPass) {
        ToSouthActionInfo actionInfo = new ToSouthActionInfo();
        actionInfo.waitPlayerId = waitPlayerId;
        actionInfo.curRoundPassedPlayerSeats = curRoundPassedPlayerSeats;
        actionInfo.canPass = canPass;

        actionInfo.lastPlayCards = gameDataVo.getLastPlayCards();
        actionInfo.lastPlaySeatId = gameDataVo.getLastPlaySeatId();
        actionInfo.roundLeaderSeatId = gameDataVo.getRoundLeaderSeatId();
        actionInfo.isFirstRound = gameDataVo.isFirstRound();

        // 计算等待时间
        long currentTime = System.currentTimeMillis();
        long duration = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.PLAY_CARDS);
        actionInfo.waitEndTime = currentTime + duration;

        NotifyToSouthTurnActionInfo notify = new NotifyToSouthTurnActionInfo();

        // 1. 发给其他人,不携带推荐牌组
        actionInfo.recommendCardsList = null;
        actionInfo.canPlay = true;
        notify.actionInfo = actionInfo;
        
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.getPlayerId() == waitPlayerId) continue;
            
            // 为每个接收者设置其自己的手牌
            // 在 Netty/Protobuf 场景下，通常在 write 时会序列化，如果是同步序列化，那么可以复用对象。
            // 但为了绝对安全，这里使用 clone
            ToSouthActionInfo playerActionInfo = cloneActionInfo(actionInfo);
            playerActionInfo.selfHandCards = PokerDataHelper.getClientId(gameDataVo, info.getCurrentCards());
            
            notify.actionInfo = playerActionInfo;
            RoomMessageBuilder.newBuilder().sendPlayer(info.getPlayerId(), notify);
        }

        PlayerSeatInfo waitPlayer = gameDataVo.getPlayerSeatInfoMap().get(waitPlayerId);
        // 计算推荐出牌 (仅针对等待玩家) 发给当前操作玩家 (带 recommend)
        fillRecommendCards(actionInfo, waitPlayer);
        
        ToSouthActionInfo waitPlayerActionInfo = cloneActionInfo(actionInfo);
        waitPlayerActionInfo.selfHandCards = PokerDataHelper.getClientId(gameDataVo, waitPlayer.getCurrentCards());
        
        notify.actionInfo = waitPlayerActionInfo;
        RoomMessageBuilder.newBuilder().sendPlayer(waitPlayerId, notify);
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
                        .filter(c -> c.getRank() == RANK_3 && c.getSuit() == SPADE_SUITS)
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
        target.curRoundPassedPlayerSeats = source.curRoundPassedPlayerSeats;
        target.recommendCardsList = source.recommendCardsList;
        target.lastPlayCards = source.lastPlayCards;
        target.lastPlaySeatId = source.lastPlaySeatId;
        target.roundLeaderSeatId = source.roundLeaderSeatId;
        target.isFirstRound = source.isFirstRound;
        target.selfHandCards = source.selfHandCards;
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
        log.debug("响应房间信息 - 玩家: {}", playerController.playerId());
        RespToSouthRoomBaseInfo baseInfo = new RespToSouthRoomBaseInfo(Code.SUCCESS);
        baseInfo.phase = getCurrentGamePhase();
        baseInfo.playerInfos = new ArrayList<>();
        Map<Long, PlayerSeatInfo> playerSeatInfoMap = gameDataVo.getPlayerSeatInfoMap();
        for (Map.Entry<Integer, SeatInfo> entry : gameDataVo.getSeatInfo().entrySet()) {
            SeatInfo seatInfo = entry.getValue();
            if (playerNotInit(seatInfo.getPlayerId()) || !seatInfo.isSeatDown()) {
                continue;
            }
            ToSouthPlayerInfo playerInfo = new ToSouthPlayerInfo();
            playerInfo.pokerPlayerInfo = PokerBuilder.getPokerPlayerInfo(seatInfo, this);
            PlayerSeatInfo seatPlayerInfo = playerSeatInfoMap.get(seatInfo.getPlayerId());
            if (Objects.nonNull(seatPlayerInfo) && !seatPlayerInfo.isDelState()) {
                // 仅发送手牌数量，不再发送具体手牌
                playerInfo.handCardCount = seatPlayerInfo.getCurrentCards().size();
                playerInfo.pokerPlayerInfo.operationType = seatPlayerInfo.getOperationType();
            }
            baseInfo.playerInfos.add(playerInfo);
        }
        
        if (baseInfo.phase == EGamePhase.PLAY_CART) {
            ToSouthActionInfo actionInfo = new ToSouthActionInfo();
            
            // 填充当前玩家手牌
            PlayerSeatInfo selfPlayerInfo = gameDataVo.getPlayerSeatInfoMap().get(playerController.playerId());
            if (selfPlayerInfo != null && !selfPlayerInfo.isDelState()) {
                actionInfo.selfHandCards = PokerDataHelper.getClientId(gameDataVo, selfPlayerInfo.getCurrentCards());
            }

            PlayerSeatInfo currentPlayer = gameDataVo.getCurrentPlayerSeatInfo();
            if (Objects.nonNull(currentPlayer)) {
                actionInfo.waitPlayerId = currentPlayer.getPlayerId();
                if (currentPlayer.getPlayerId() == playerController.playerId()) {
                     fillRecommendCards(actionInfo, currentPlayer);
                }
            }
            
            if (Objects.nonNull(gameDataVo.getPlayerTimerEvent())) {
                actionInfo.waitEndTime = gameDataVo.getPlayerTimerEvent().getNextTime();
            }
            
            actionInfo.curRoundPassedPlayerSeats = gameDataVo.getCurRoundPassedPlayerSeats();
            actionInfo.lastPlayCards = gameDataVo.getLastPlayCards();
            actionInfo.lastPlaySeatId = gameDataVo.getLastPlaySeatId();
            actionInfo.roundLeaderSeatId = gameDataVo.getRoundLeaderSeatId();
            actionInfo.isFirstRound = gameDataVo.isFirstRound();
            if (Objects.nonNull(currentPlayer)) {
                 if (isFirstPlayer(currentPlayer.getSeatId())) {
                     actionInfo.canPass = false;
                 }
            }

            baseInfo.actionInfo = actionInfo;
        }
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerController.playerId(), baseInfo));
    }

    @Override
    public boolean tryStartGame() {
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        int total = gameDataVo.getSeatDownNum();
        if (getCurrentGamePhase() == EGamePhase.WAIT_READY && total == 4) {
            addPokerPhaseTimer(new ToSouthStartGamePhase(this, gameDataVo.getId()));
            log.info("尝试开启下一局 当前id{} roomId:{}", gameDataVo.getId(), roomController.getRoom().getId());
            return true;
        }
        return false;
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
