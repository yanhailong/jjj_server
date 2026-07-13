package com.jjg.game.poker.game.douxian.room;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.message.req.ReqPokerBet;
import com.jjg.game.poker.game.common.message.req.ReqPokerSampleCardOperation;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.constant.DouXianZone;
import com.jjg.game.poker.game.douxian.data.DouXianBuilder;
import com.jjg.game.poker.game.douxian.data.DouXianDataHelper;
import com.jjg.game.poker.game.douxian.data.DouXianZoneCards;
import com.jjg.game.poker.game.douxian.gamephase.DouXianDealPhase;
import com.jjg.game.poker.game.douxian.gamephase.DouXianSettlementPhase;
import com.jjg.game.poker.game.douxian.gamephase.DouXianTierAdvancePhase;
import com.jjg.game.poker.game.douxian.message.req.ReqDouXianCancelHosting;
import com.jjg.game.poker.game.douxian.message.req.ReqDouXianConcede;
import com.jjg.game.poker.game.douxian.message.req.ReqDouXianConfirmPlay;
import com.jjg.game.poker.game.douxian.message.req.ReqDouXianDiscard;
import com.jjg.game.poker.game.douxian.message.req.ReqDouXianPlaceCard;
import com.jjg.game.poker.game.douxian.message.req.ReqDouXianRecharge;
import com.jjg.game.poker.game.douxian.message.resp.NotifyDouXianConcede;
import com.jjg.game.poker.game.douxian.message.resp.NotifyDouXianConfirmResult;
import com.jjg.game.poker.game.douxian.message.resp.NotifyDouXianDiscardResult;
import com.jjg.game.poker.game.douxian.message.resp.NotifyDouXianHostingState;
import com.jjg.game.poker.game.douxian.message.resp.NotifyDouXianPlaceCardResult;
import com.jjg.game.poker.game.douxian.message.resp.RepsDouXianRoomBaseInfo;
import com.jjg.game.poker.game.douxian.room.data.DouXianGameDataVo;
import com.jjg.game.poker.game.douxian.util.DouXianHandEvaluator;
import com.jjg.game.poker.game.douxian.util.DouXianHandResult;
import com.jjg.game.poker.game.douxian.util.DouXianSettlementCalculator;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.ImmortalCardCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 斗仙牌房间控制器。
 * <p>
 * 开局/补牌->出牌->结算->飞升->弃牌 的完整状态机、得证大道/隐忍渡劫/认输/即时充值复活流程、
 * 机器人调度都已经接好（见对应 gamephase/autohandler 类）。还没做/没接的：
 * 结算的"场次最小输赢/封顶值"（见 {@link DouXianSettlementCalculator} 的 TODO）、
 * 灵气复苏（公式未知，完全没实现）、充值复活的真实支付渠道、正式的大结算界面。
 */
@GameController(gameType = EGameType.DOU_XIAN, roomType = RoomType.POKER_ROOM)
public class DouXianGameController extends BasePokerGameController<DouXianGameDataVo> {

    public DouXianGameController(AbstractRoomController<Room_ChessCfg, ? extends Room> roomController) {
        super(roomController);
    }

    @Override
    protected DouXianGameDataVo createRoomDataVo(Room_ChessCfg roomCfg) {
        return new DouXianGameDataVo(roomCfg);
    }

    @Override
    public EGameType gameControlType() {
        return EGameType.DOU_XIAN;
    }

    @Override
    public void respRoomInitInfoAction(PlayerController playerController) {
        long viewerId = playerController.playerId();
        RepsDouXianRoomBaseInfo baseInfo = new RepsDouXianRoomBaseInfo();
        baseInfo.phase = getCurrentGamePhase();
        baseInfo.round = gameDataVo.getRound();
        baseInfo.roundMultiplier = DouXianConstant.getRoundMultiplier(Math.max(gameDataVo.getRound(), 1));
        baseInfo.playerInfos = new ArrayList<>();
        for (Long playerId : gameDataVo.getActivePlayerIds()) {
            // 重连/进房推送要按接收方视角脱敏：别人本回合还没结算亮牌的摆牌不能让重连玩家看到，见DESIGN.md 8.8
            baseInfo.playerInfos.add(DouXianBuilder.buildPlayerInfo(playerId, this, playerId == viewerId));
        }
        List<Integer> selfHand = gameDataVo.getHandCards().get(viewerId);
        if (selfHand != null) {
            baseInfo.selfHandCardIds = DouXianDataHelper.getClientCardIds(gameDataVo, selfHand);
        }
        baseInfo.overTime = gameDataVo.getPhaseEndTime();
        baseInfo.betBase = gameDataVo.getRoomCfg().getBetBase();
        ImmortalCardCfg cardCfg = DouXianDataHelper.getImmortalCardCfg(gameDataVo);
        if (cardCfg != null) {
            baseInfo.minWinLimit = cardCfg.getWinLoss();
            baseInfo.maxWinLimit = cardCfg.getMaxCap();
        }
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerController.playerId(), baseInfo));
        log.debug("斗仙牌推送房间初始信息 playerId:{} roomCfgId:{} phase:{}",
                playerController.playerId(), gameDataVo.getRoomCfg().getId(), baseInfo.phase);
    }

    @Override
    public boolean tryStartGame() {
        if (gameDataVo.canStartGame() && getCurrentGamePhase() == EGamePhase.WAIT_READY) {
            genPlayerSeatInfoList(gameDataVo.getSeatInfo(), gameDataVo.getPlayerSeatInfoList());
            DouXianDataHelper.shuffleNewDeck(gameDataVo);
            // 开局前携带金币快照，DESIGN.md 6.2 "小额玩家保护"判定依据之一
            for (Long playerId : gameDataVo.getActivePlayerIds()) {
                gameDataVo.getGameStartBalance().put(playerId, getTransactionItemNum(playerId));
            }
            log.info("################ 斗仙牌开局 roomCfgId:{} roomId:{} 玩家:{} 开局金币:{} ################",
                    gameDataVo.getRoomCfg().getId(), roomController.getRoom().getId(), gameDataVo.getActivePlayerIds(), gameDataVo.getGameStartBalance());
            addPokerPhaseTimer(new DouXianDealPhase(this));
            return true;
        }
        return false;
    }

    /**
     * 出牌阶段结束后的钩子：进入两两×三区域结算(含全胜判定)，结算完再进飞升。
     */
    @Override
    public void startNextRoundOrSettlement() {
        addPokerPhaseTimer(new DouXianSettlementPhase(this));
    }

    /**
     * 弃牌阶段结束后决定：还有下一回合就补牌重开，第4回合结束就收尾。
     * TODO: 应该在这里弹真正的大结算界面(协议已就绪，见 NotifyDouXianGrandSettlement)，目前直接回到等待阶段。
     */
    public void finishRoundCycle() {
        if (gameDataVo.getRound() >= DouXianConstant.Common.TOTAL_ROUND) {
            log.info("################ 斗仙牌第{}回合(最后一回合)结束，大结算尚未实现，本局直接结束 roomCfgId:{} ################",
                    gameDataVo.getRound(), gameDataVo.getRoomCfg().getId());
            goBackWaitReadyPhase();
            gameDataVo.resetData(this);
            tryStartNextGame();
            return;
        }
        log.info("========== 斗仙牌第{}回合结束，进入第{}回合 ==========", gameDataVo.getRound(), gameDataVo.getRound() + 1);
        gameDataVo.nextRound();
        addPokerPhaseTimer(new DouXianDealPhase(this));
    }

    @Override
    public PlayerSeatInfo getNextExePlayer() {
        return null;
    }

    @Override
    public void sampleCardOperation(long playerId, ReqPokerSampleCardOperation req) {
        log.warn("斗仙牌不使用通用简单牌操作协议，忽略该请求 playerId:{} type:{}", playerId, req.type);
    }

    @Override
    public void dealBet(long playerId, ReqPokerBet reqPokerBet) {
        log.warn("斗仙牌没有下注阶段，忽略该下注请求 playerId:{}", playerId);
    }

    @Override
    public void addNextTimer(PlayerSeatInfo nextExePlayer, int sendCardNum) {
        // 斗仙牌摆牌是多人并行操作，不是单人轮流定时器，倒计时由各 gamephase 自行通过
        // addPokerPhaseTimer 管理，这里不需要做任何事。
    }

    @Override
    public void onRunGamePlayerLeaveRoom(SeatInfo remove) {
        log.warn("斗仙牌局中玩家离座，按认输处理 playerId:{}", remove.getPlayerId());
        doConcede(remove.getPlayerId());
    }

    @Override
    public void onPlayerLeaveRoomAction(RoomPlayer roomPlayer, SeatInfo remove) {
        gameDataVo.getHandCards().remove(remove.getPlayerId());
        gameDataVo.getConfirmedPlayerIds().remove(remove.getPlayerId());
        gameDataVo.getConcededPlayerIds().remove(remove.getPlayerId());
        gameDataVo.getHostingPlayerIds().remove(remove.getPlayerId());
        gameDataVo.getPendingSpecialRule().remove(remove.getPlayerId());
        gameDataVo.getRechargingPlayerIds().remove(remove.getPlayerId());
        log.info("斗仙牌玩家离开房间，已清理局内数据 playerId:{} roomCfgId:{}",
                remove.getPlayerId(), gameDataVo.getRoomCfg().getId());
    }

    @Override
    public boolean canJoinRobot() {
        return getCurrentGamePhase() == EGamePhase.WAIT_READY;
    }

    // ------------------------------------------------------------------
    // 出牌阶段
    // ------------------------------------------------------------------

    public void reqPlaceCard(long playerId, ReqDouXianPlaceCard req) {
        if (getCurrentGamePhase() != EGamePhase.PLAY_CART
                || gameDataVo.getConcededPlayerIds().contains(playerId)
                || gameDataVo.getConfirmedPlayerIds().contains(playerId)) {
            sendPlaceCardError(playerId, Code.FORBID);
            return;
        }
        DouXianZone zone = DouXianZone.fromId(req.zoneId);
        int round = gameDataVo.getRound();
        if (zone == null || !zone.isOpenAt(round)) {
            sendPlaceCardError(playerId, Code.PARAM_ERROR);
            return;
        }
        DouXianZoneCards zoneCards = gameDataVo.getPlayerZoneCards(playerId).get(zone);
        if (req.cardIds.size() > zone.getCapacity() - zoneCards.getCarriedCards().size()) {
            sendPlaceCardError(playerId, Code.PARAM_ERROR);
            return;
        }
        List<Integer> hand = gameDataVo.getHandCards().computeIfAbsent(playerId, k -> new ArrayList<>());
        Set<Integer> available = new HashSet<>(hand);
        available.addAll(zoneCards.getNewCards());
        List<Integer> requestedCfgIds = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (Integer clientId : req.cardIds) {
            Integer cfgId = DouXianDataHelper.findCfgIdByClientId(gameDataVo, clientId);
            if (cfgId == null || !available.contains(cfgId) || !seen.add(cfgId)) {
                // cfgId为null:客户端id无效；不在available里:不属于该玩家；seen.add()==false:请求里重复摆了同一张牌
                sendPlaceCardError(playerId, Code.PARAM_ERROR);
                return;
            }
            requestedCfgIds.add(cfgId);
        }

        hand.addAll(zoneCards.getNewCards());
        zoneCards.getNewCards().clear();
        for (Integer cfgId : requestedCfgIds) {
            hand.remove(cfgId);
        }
        zoneCards.getNewCards().addAll(requestedCfgIds);

        // 本回合还没结算亮牌，不能把摆入的牌面/牌型广播给其他玩家(见DESIGN.md 8.8)：
        // 本人收到完整牌面，其他人只收到隐藏张数。
        NotifyDouXianPlaceCardResult selfNotify = new NotifyDouXianPlaceCardResult();
        selfNotify.playerId = playerId;
        selfNotify.placement = DouXianBuilder.buildZonePlacements(playerId, gameDataVo, true).stream()
                .filter(p -> p.zoneId == zone.getId()).findFirst().orElse(null);
        selfNotify.remainHandCardNum = hand.size();
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, selfNotify));

        NotifyDouXianPlaceCardResult othersNotify = new NotifyDouXianPlaceCardResult();
        othersNotify.playerId = playerId;
        othersNotify.placement = DouXianBuilder.buildZonePlacements(playerId, gameDataVo, false).stream()
                .filter(p -> p.zoneId == zone.getId()).findFirst().orElse(null);
        othersNotify.remainHandCardNum = hand.size();
        broadcastToPlayers(RoomMessageBuilder.newBuilder().toAllPlayer().exceptPlayer(playerId).setData(othersNotify));

        log.info("斗仙牌摆牌 playerId:{} zone:{} 本次摆入:{} 该区域当前:{} 剩余手牌:{}",
                playerId, zone, DouXianDataHelper.cfgIdsToString(gameDataVo, requestedCfgIds),
                DouXianDataHelper.cfgIdsToString(gameDataVo, zoneCards.getAllCards()),
                DouXianDataHelper.cfgIdsToString(gameDataVo, hand));
    }

    private void sendPlaceCardError(long playerId, int code) {
        NotifyDouXianPlaceCardResult notify = new NotifyDouXianPlaceCardResult();
        notify.code = code;
        notify.playerId = playerId;
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notify));
    }

    public void reqConfirmPlay(long playerId, ReqDouXianConfirmPlay req) {
        if (getCurrentGamePhase() != EGamePhase.PLAY_CART
                || gameDataVo.getConcededPlayerIds().contains(playerId)
                || gameDataVo.getConfirmedPlayerIds().contains(playerId)) {
            return;
        }
        int round = gameDataVo.getRound();
        for (DouXianZone zone : DouXianZone.values()) {
            if (!zone.isOpenAt(round)) {
                continue;
            }
            if (!gameDataVo.getPlayerZoneCards(playerId).get(zone).isFull()) {
                NotifyDouXianConfirmResult error = new NotifyDouXianConfirmResult();
                error.code = Code.PARAM_ERROR;
                error.playerId = playerId;
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, error));
                return;
            }
        }
        gameDataVo.getConfirmedPlayerIds().add(playerId);
        broadcastConfirmResult(playerId);
        logPlayerFinalHands("玩家主动确认", playerId);
        if (isAllActiveConfirmed()) {
            removePokerPhaseTimer();
            currentGamePhase.phaseFinish();
        }
    }

    /**
     * 打印某玩家本回合已开放区域的最终摆牌+牌型+灵力值，确认出牌(主动/托管/超时)时都会调用，
     * 是核对"摆牌对不对/灵力值算得对不对"最直接的日志。
     */
    private void logPlayerFinalHands(String reason, long playerId) {
        int round = gameDataVo.getRound();
        StringBuilder sb = new StringBuilder();
        for (DouXianZone zone : DouXianZone.values()) {
            if (!zone.isOpenAt(round)) {
                continue;
            }
            List<Integer> cardIds = gameDataVo.getPlayerZoneCards(playerId).get(zone).getAllCards();
            List<Card> cards = DouXianDataHelper.toCards(gameDataVo, cardIds);
            DouXianHandResult result = DouXianHandEvaluator.evaluateZone(zone, cards, round);
            sb.append(zone).append(DouXianDataHelper.cardsToString(cards))
                    .append('=').append(result.getHandType().getDisplayName())
                    .append('(').append(result.getAetherValue()).append(") ");
        }
        log.info("斗仙牌摆牌确认[{}] round:{} playerId:{} {}", reason, round, playerId, sb);
    }

    /**
     * 托管/超时时自动摆牌：按 仙>灵>凡 优先级贪心选灵力值最高的组合(DESIGN.md 5.)
     */
    public void autoFillAndConfirm(long playerId) {
        if (gameDataVo.getConfirmedPlayerIds().contains(playerId)) {
            return;
        }
        int round = gameDataVo.getRound();
        List<Integer> hand = gameDataVo.getHandCards().computeIfAbsent(playerId, k -> new ArrayList<>());
        for (DouXianZone zone : List.of(DouXianZone.IMMORTAL, DouXianZone.SPIRIT, DouXianZone.MORTAL)) {
            if (!zone.isOpenAt(round)) {
                continue;
            }
            DouXianZoneCards zoneCards = gameDataVo.getPlayerZoneCards(playerId).get(zone);
            int need = zoneCards.remainingCapacity();
            if (need <= 0) {
                continue;
            }
            if (hand.size() < need) {
                log.error("斗仙牌托管摆牌手牌不足 playerId:{} zone:{} need:{} handSize:{}", playerId, zone, need, hand.size());
                continue;
            }
            List<Card> carried = DouXianDataHelper.toCards(gameDataVo, zoneCards.getCarriedCards());
            List<Card> candidates = DouXianDataHelper.toCards(gameDataVo, hand);
            DouXianHandResult best = DouXianHandEvaluator.findBestZone(zone, carried, candidates, round);
            List<Card> chosenNew = best.getCards().subList(carried.size(), best.getCards().size());
            for (Card card : chosenNew) {
                int cfgId = DouXianDataHelper.toCfgId(card);
                hand.remove(Integer.valueOf(cfgId));
                zoneCards.getNewCards().add(cfgId);
            }
            log.info("斗仙牌自动摆牌(托管/机器人) playerId:{} zone:{} 候选手牌:{} 选中:{} 结果:{}({})",
                    playerId, zone, DouXianDataHelper.cardsToString(candidates), DouXianDataHelper.cardsToString(chosenNew),
                    best.getHandType().getDisplayName(), best.getAetherValue());
        }
        gameDataVo.getConfirmedPlayerIds().add(playerId);
        broadcastConfirmResult(playerId);
        logPlayerFinalHands("托管/机器人自动确认", playerId);
    }

    private void broadcastConfirmResult(long playerId) {
        NotifyDouXianConfirmResult notify = new NotifyDouXianConfirmResult();
        notify.playerId = playerId;
        notify.allConfirmed = isAllActiveConfirmed();
        broadcastToPlayers(RoomMessageBuilder.newBuilder().toAllPlayer().setData(notify));
    }

    private boolean isAllActiveConfirmed() {
        for (Long playerId : gameDataVo.getActivePlayerIds()) {
            if (!gameDataVo.getConfirmedPlayerIds().contains(playerId)) {
                return false;
            }
        }
        return true;
    }

    public void forceFinishPlayCardPhase() {
        for (Long playerId : gameDataVo.getActivePlayerIds()) {
            if (!gameDataVo.getConfirmedPlayerIds().contains(playerId)) {
                boolean firstTimeout = gameDataVo.getHostingPlayerIds().add(playerId);
                if (firstTimeout) {
                    log.warn("斗仙牌出牌超时，玩家进入托管 playerId:{}", playerId);
                    NotifyDouXianHostingState hostingNotify = new NotifyDouXianHostingState();
                    hostingNotify.playerId = playerId;
                    hostingNotify.hosting = true;
                    broadcastToPlayers(RoomMessageBuilder.newBuilder().toAllPlayer().setData(hostingNotify));
                }
                autoFillAndConfirm(playerId);
            }
        }
        startNextRoundOrSettlement();
    }

    // ------------------------------------------------------------------
    // 弃牌阶段
    // ------------------------------------------------------------------

    public void reqDiscard(long playerId, ReqDouXianDiscard req) {
        if (getCurrentGamePhase() != EGamePhase.DISCARD
                || gameDataVo.getConcededPlayerIds().contains(playerId)
                || gameDataVo.getDiscardedPlayerIds().contains(playerId)) {
            return;
        }
        List<Integer> hand = gameDataVo.getHandCards().computeIfAbsent(playerId, k -> new ArrayList<>());
        List<Integer> discardedCfgIds = new ArrayList<>();
        if (!req.noDiscard && req.cardIds != null && !req.cardIds.isEmpty()) {
            Set<Integer> seen = new HashSet<>();
            for (Integer clientId : req.cardIds) {
                Integer cfgId = DouXianDataHelper.findCfgIdByClientId(gameDataVo, clientId);
                if (cfgId == null || !hand.contains(cfgId) || !seen.add(cfgId)) {
                    // cfgId为null:客户端id无效；不在hand里:不属于该玩家；seen.add()==false:请求里重复弃了同一张牌
                    NotifyDouXianDiscardResult error = new NotifyDouXianDiscardResult();
                    error.code = Code.PARAM_ERROR;
                    error.playerId = playerId;
                    broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, error));
                    return;
                }
                discardedCfgIds.add(cfgId);
            }
            hand.removeAll(discardedCfgIds);
            DouXianDataHelper.returnCardsToPool(gameDataVo, discardedCfgIds);
        }
        gameDataVo.getDiscardedPlayerIds().add(playerId);
        broadcastDiscardResult(playerId, discardedCfgIds.isEmpty(), discardedCfgIds.size());
        log.info("斗仙牌弃牌 playerId:{} 弃了:{} 剩余手牌:{}",
                playerId, discardedCfgIds.isEmpty() ? "不弃" : DouXianDataHelper.cfgIdsToString(gameDataVo, discardedCfgIds),
                DouXianDataHelper.cfgIdsToString(gameDataVo, hand));
        if (isAllActiveDiscarded()) {
            removePokerPhaseTimer();
            currentGamePhase.phaseFinish();
        }
    }

    public void autoNoDiscard(long playerId) {
        if (gameDataVo.getDiscardedPlayerIds().contains(playerId)) {
            return;
        }
        gameDataVo.getDiscardedPlayerIds().add(playerId);
        broadcastDiscardResult(playerId, true, 0);
    }

    private void broadcastDiscardResult(long playerId, boolean noDiscard, int discardCount) {
        NotifyDouXianDiscardResult notify = new NotifyDouXianDiscardResult();
        notify.playerId = playerId;
        notify.noDiscard = noDiscard;
        notify.discardCount = discardCount;
        notify.allDiscarded = isAllActiveDiscarded();
        broadcastToPlayers(RoomMessageBuilder.newBuilder().toAllPlayer().setData(notify));
    }

    private boolean isAllActiveDiscarded() {
        for (Long playerId : gameDataVo.getActivePlayerIds()) {
            if (!gameDataVo.getDiscardedPlayerIds().contains(playerId)) {
                return false;
            }
        }
        return true;
    }

    public void forceFinishDiscardPhase() {
        for (Long playerId : gameDataVo.getActivePlayerIds()) {
            if (!gameDataVo.getDiscardedPlayerIds().contains(playerId)) {
                log.info("斗仙牌弃牌超时，视为不弃 playerId:{}", playerId);
                autoNoDiscard(playerId);
            }
        }
        finishRoundCycle();
    }

    public void reqCancelHosting(long playerId, ReqDouXianCancelHosting req) {
        if (gameDataVo.getHostingPlayerIds().remove(playerId)) {
            NotifyDouXianHostingState notify = new NotifyDouXianHostingState();
            notify.playerId = playerId;
            notify.hosting = false;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().toAllPlayer().setData(notify));
            log.info("斗仙牌玩家取消托管 playerId:{}", playerId);
        }
    }

    // ------------------------------------------------------------------
    // 即时充值复活(DESIGN.md 8.9)
    // TODO(需要支付网关联调): 没有真实支付渠道可以验证"充值成功"，reqRecharge 目前只能记录请求，
    // 不能真的补充金币；倒计时到了(DouXianRechargePhase 超时)一律按认输处理，见 forceFinishRechargePhase。
    // ------------------------------------------------------------------

    public void reqRecharge(long playerId, ReqDouXianRecharge req) {
        log.warn("收到斗仙牌即时充值复活请求，未接入真实支付渠道，暂时无法处理 playerId:{} rechargeOptionId:{}",
                playerId, req.rechargeOptionId);
    }

    public void forceFinishRechargePhase() {
        for (Long playerId : new ArrayList<>(gameDataVo.getRechargingPlayerIds())) {
            if (getCurrentGamePhase() != EGamePhase.RECHARGE) {
                // 上一个玩家认输已经把只剩一人的大结算/游戏重置触发了，不能再对这份陈旧的
                // 充值等待名单继续操作，否则会污染 tryStartNextGame() 可能已经开启的新一局
                return;
            }
            log.warn("斗仙牌即时充值复活超时，按认输处理 playerId:{}", playerId);
            doConcede(playerId);
        }
        gameDataVo.getRechargingPlayerIds().clear();
        if (getCurrentGamePhase() == EGamePhase.RECHARGE) {
            addPokerPhaseTimer(new DouXianTierAdvancePhase(this));
        }
    }

    // ------------------------------------------------------------------
    // 认输(DESIGN.md 8.9)
    // ------------------------------------------------------------------

    public void reqConcede(long playerId, ReqDouXianConcede req) {
        if (gameDataVo.getConcededPlayerIds().contains(playerId)) {
            return;
        }
        log.info("斗仙牌玩家主动认输 playerId:{}", playerId);
        doConcede(playerId);
    }

    /**
     * 认输的通用处理：不管是主动认输、超时未充值复活还是局中离座，都走这里。
     * 手牌和场上摆的牌全部收回公共牌库(比文档"下回合结束时回收"更早，是阶段4的简化实现，
     * 见 DESIGN.md)，不再等到下一次飞升。
     */
    private void doConcede(long playerId) {
        if (gameDataVo.getConcededPlayerIds().contains(playerId)) {
            return;
        }
        gameDataVo.getConcededPlayerIds().add(playerId);
        gameDataVo.getRechargingPlayerIds().remove(playerId);
        gameDataVo.getPendingSpecialRule().remove(playerId);

        List<Integer> toReturn = new ArrayList<>(gameDataVo.getHandCards().getOrDefault(playerId, List.of()));
        for (DouXianZone zone : DouXianZone.values()) {
            DouXianZoneCards zoneCards = gameDataVo.getPlayerZoneCards(playerId).get(zone);
            toReturn.addAll(zoneCards.getAllCards());
            zoneCards.clear();
        }
        gameDataVo.getHandCards().remove(playerId);
        DouXianDataHelper.returnCardsToPool(gameDataVo, toReturn);

        NotifyDouXianConcede notify = new NotifyDouXianConcede();
        notify.playerId = playerId;
        notify.triggerGrandSettlement = gameDataVo.getActivePlayerIds().size() <= 1;
        broadcastToPlayers(RoomMessageBuilder.newBuilder().toAllPlayer().setData(notify));
        log.info("斗仙牌玩家认输完成 playerId:{} 剩余活跃玩家数:{}", playerId, gameDataVo.getActivePlayerIds().size());

        if (notify.triggerGrandSettlement) {
            // TODO(阶段4遗留): 应该弹真正的大结算界面，目前跟 finishRoundCycle() 的占位实现一致，
            // 直接结束本局回到等待阶段。
            log.warn("斗仙牌只剩一名未认输玩家，大结算尚未实现，本局直接结束 roomCfgId:{}", gameDataVo.getRoomCfg().getId());
            goBackWaitReadyPhase();
            gameDataVo.resetData(this);
            tryStartNextGame();
            return;
        }
        // 这个玩家的离开可能导致当前阶段"其余人全部完成操作"，需要主动推进，不能一直等一个已经不在的人
        if (getCurrentGamePhase() == EGamePhase.PLAY_CART && isAllActiveConfirmed()) {
            removePokerPhaseTimer();
            currentGamePhase.phaseFinish();
        } else if (getCurrentGamePhase() == EGamePhase.DISCARD && isAllActiveDiscarded()) {
            removePokerPhaseTimer();
            currentGamePhase.phaseFinish();
        }
    }
}
