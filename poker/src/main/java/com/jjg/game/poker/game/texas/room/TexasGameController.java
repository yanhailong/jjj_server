package com.jjg.game.poker.game.texas.room;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.gamephase.BaseWaitReadyPhase;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerSampleCardOperation;
import com.jjg.game.poker.game.common.message.req.ReqPokerBet;
import com.jjg.game.poker.game.common.message.req.ReqPokerSampleCardOperation;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.data.Pot;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.data.TexasSaveHistory;
import com.jjg.game.poker.game.texas.gamephase.TexasPlayCardPhase;
import com.jjg.game.poker.game.texas.gamephase.TexasProcessorHandler;
import com.jjg.game.poker.game.texas.gamephase.TexasSettlementPhase;
import com.jjg.game.poker.game.texas.message.TexasBuilder;
import com.jjg.game.poker.game.texas.message.bean.TexasHistory;
import com.jjg.game.poker.game.texas.message.bean.TexasHistoryPlayerInfo;
import com.jjg.game.poker.game.texas.message.bean.TexasHistoryRoundInfo;
import com.jjg.game.poker.game.texas.message.bean.TexasPlayerInfo;
import com.jjg.game.poker.game.texas.message.reps.*;
import com.jjg.game.poker.game.texas.message.req.ReqTexasHistory;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.poker.game.texas.util.HandResult;
import com.jjg.game.room.base.ERoomItemReason;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.RoomDataHelper;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import com.jjg.game.sampledata.bean.TexasCfg;

import java.util.*;
import java.util.stream.Collectors;

import static com.jjg.game.poker.game.texas.constant.TexasConstant.Common.*;

/**
 * @author lm
 */
@GameController(gameType = EGameType.TEXAS, roomType = RoomType.POKER_ROOM)
public class TexasGameController extends BasePokerGameController<TexasGameDataVo> {
    public TexasGameController(AbstractRoomController<Room_ChessCfg, ? extends Room> roomController) {
        super(roomController);
    }

    @Override
    protected TexasGameDataVo createRoomDataVo(Room_ChessCfg roomCfg) {
        return new TexasGameDataVo(roomCfg);
    }

    @Override
    public void sampleCardOperation(long playerId, ReqPokerSampleCardOperation req) {
        switch (req.type) {
            case PokerConstant.PlayerOperation.DISCARD -> discardCard(playerId);
            case PokerConstant.PlayerOperation.PASS -> passCards(playerId);
        }
    }

    @Override
    public void respRoomInitInfoAction(PlayerController playerController) {
        RepsTexasRoomBaseInfo repsTexasRoomBaseInfo = new RepsTexasRoomBaseInfo(Code.SUCCESS);
        if (getCurrentGamePhase() != EGamePhase.WAIT_READY) {
            if (Objects.nonNull(gameDataVo.getPublicCards())) {
                repsTexasRoomBaseInfo.publicCards = TexasDataHelper.getClientId(gameDataVo,
                    gameDataVo.getPublicCards());
            }
            repsTexasRoomBaseInfo.waitPlayerId = gameDataVo.getCurrentPlayerSeatInfo().getPlayerId();
            repsTexasRoomBaseInfo.waitEndTime = gameDataVo.getPlayerTimerEvent().getNextTime();
            List<Pot> pool = gameDataVo.getPool();
            repsTexasRoomBaseInfo.bottomPool = pool.getFirst().getAmount();
            repsTexasRoomBaseInfo.seatId = gameDataVo.getDealerSeatId();
            if (pool.size() > 1) {
                repsTexasRoomBaseInfo.edgePool = pool.subList(1, pool.size())
                    .stream()
                    .map(Pot::getAmount)
                    .toList();
            }
        }
        repsTexasRoomBaseInfo.notifyTexasSettlementInfo = gameDataVo.getNotifyTexasSettlementInfo();
        List<TexasPlayerInfo> playerInfos = new ArrayList<>();
        Map<Long, PlayerSeatInfo> playerSeatInfoMap = gameDataVo.getPlayerSeatInfoMap();
        //构建玩家信息
        for (Map.Entry<Integer, SeatInfo> entry : gameDataVo.getSeatInfo().entrySet()) {
            SeatInfo seatInfo = entry.getValue();
            if (seatInfo.getPlayerId() == playerController.playerId()) {
                if (!gameDataVo.getTempGold().containsKey(playerController.playerId())) {
                    GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerController.playerId());
                    addTempGoldOrOutTable(seatInfo, gamePlayer);
                }
            }
            playerInfos.add(TexasBuilder.getTexasPlayerInfo(playerSeatInfoMap.get(seatInfo.getPlayerId()), seatInfo,
                gameDataVo));
        }
        repsTexasRoomBaseInfo.playerInfos = playerInfos;
        repsTexasRoomBaseInfo.findDealerTime = TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.FIND_DEALER);
        repsTexasRoomBaseInfo.sendCardTime = TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS);
        repsTexasRoomBaseInfo.operationTime = TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.PLAY_CARDS);
        TexasCfg texasCfg = TexasDataHelper.getTexasCfg(gameDataVo);
        repsTexasRoomBaseInfo.SB = texasCfg.getSbNum();
        repsTexasRoomBaseInfo.BB = texasCfg.getBbNum();
        repsTexasRoomBaseInfo.operationTime = TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.PLAY_CARDS);
        //客户端初始化完成
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerController.playerId(),
            repsTexasRoomBaseInfo));
    }

    @Override
    public void tryStartGame() {
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        int total = gameDataVo.getSeatDownNum();
        if (getCurrentGamePhase() == EGamePhase.WAIT_READY && roomCfg.getMinPlayer() <= total && total <= roomCfg.getMaxPlayer()) {
            addPokerPhase(new TexasPlayCardPhase(this));
            log.info("开启下一局 当前id{}", gameDataVo.getId());
        }
    }


    @Override
    public EGameType gameControlType() {
        return EGameType.TEXAS;
    }


    /**
     * 下注 和 all_in
     */
    @Override
    public void dealBet(long playerId, ReqPokerBet reqPokerBet) {
        PlayerSeatInfo info = gameDataVo.getCurrentPlayerSeatInfo();
        if (Objects.isNull(info) || info.getPlayerId() != playerId) {
            return;
        }
        //不是当前玩家执行
        if (notDoOperation(playerId, info)) {
            return;
        }
        //all_In
        long betValue = 0;
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        long tempCurrency = gameDataVo.getTempGold().getOrDefault(playerId, 0L);
        boolean allIn = reqPokerBet.betType == PokerConstant.PlayerOperation.ALL_IN;
        Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
        if (allIn) {
            betValue = tempCurrency;
        } else if (reqPokerBet.betType == PokerConstant.PlayerOperation.BET && tempCurrency >= reqPokerBet.betValue) {
            if (!isAllAllIn(playerId)) {
                return;
            }
            //正常下注
            betValue = reqPokerBet.betValue;
        } else if (reqPokerBet.betType == PokerConstant.PlayerOperation.FOLLOW_CARD) {
            //跟注
            betValue = gameDataVo.getMaxBetValue() - baseBetInfo.getOrDefault(playerId, 0L);
        }
        Long remain = gameDataVo.getTempGold().get(playerId);
        long tempTotal = baseBetInfo.getOrDefault(playerId, 0L) + betValue;
        if (betValue <= 0 || betValue > remain || !allIn && tempTotal < gameDataVo.getMaxBetValue()) {
            return;
        }
        info.setOperationType(betValue == remain ? PokerConstant.PlayerOperation.ALL_IN : reqPokerBet.betType);
        info.setOver(true);
        //修改玩家数据
        changePlayerGold(gamePlayer, -betValue);
        baseBetInfo.merge(playerId, betValue, Long::sum);
        baseBetInfo.entrySet().stream().max(Comparator.comparingLong(Map.Entry::getValue))
            .ifPresent((entry) -> gameDataVo.setMaxBetValue(entry.getValue()));
        //添加记录
        TexasHistoryRoundInfo historyRoundInfo = gameDataVo.getHistoryRoundInfo();
        historyRoundInfo.roundInfo.add(TexasBuilder.getTexasHistoryPlayerInfo(info, gameDataVo, betValue));
        TexasHistoryPlayerInfo totalPlayerBetInfo =
            gameDataVo.getTexasHistory().getTotalPlayerBetInfoMap().get(playerId);
        if (Objects.nonNull(totalPlayerBetInfo)) {
            totalPlayerBetInfo.betValue += betValue;
        }
        gameDataVo.getPool().getFirst().addChips(betValue);
        gameDataVo.getPool().getFirst().addEligiblePlayer(playerId);
        //通知
        NotifyTexasBet notifyTexasBet = new NotifyTexasBet();
        notifyTexasBet.betType = info.getOperationType();
        notifyTexasBet.betValue = betValue;
        notifyTexasBet.playerId = playerId;
        PlayerSeatInfo nextExePlayer = getNextExePlayer();
        if (Objects.nonNull(nextExePlayer)) {
            addNextTimer(nextExePlayer, 0);
            notifyTexasBet.nextPlayerId = nextExePlayer.getPlayerId();
            notifyTexasBet.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
            RoomDataHelper.checkPlayerVipLevel(gamePlayer, this, betValue);
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyTexasBet));
        } else {
            //结算的时候加注，all不算有效押注
            if (!isNextRoundOrSettlement() || isNextRoundOrSettlement() && reqPokerBet.betType == PokerConstant.PlayerOperation.FOLLOW_CARD) {
                RoomDataHelper.checkPlayerVipLevel(gamePlayer, this, betValue);
            }
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyTexasBet));
            startNextRoundOrSettlement();
        }
    }

    public boolean isAllAllIn(long playerId) {
        for (PlayerSeatInfo seatInfo : gameDataVo.getPlayerSeatInfoList()) {
            if (seatInfo.isDelState()) {
                continue;
            }
            if (seatInfo.getPlayerId() == playerId) {
                continue;
            }
            if (seatInfo.getOperationType() != PokerConstant.PlayerOperation.ALL_IN) {
                return true;
            }
        }
        return false;
    }


    /**
     * 获取初始执行的index
     */
    public int getInitIndex() {
        List<PlayerSeatInfo> playerSeatInfoList = gameDataVo.getPlayerSeatInfoList();
        int size = playerSeatInfoList.size();
        int dealerIndex = gameDataVo.getDealerIndex();
        if (size <= 3) {
            return Math.min(dealerIndex, playerSeatInfoList.size() - 1);
        }
        //庄家左侧第三个执行
        return (gameDataVo.getDealerIndex() + 3) % size;
    }

    /**
     * 过牌
     *
     * @return true 能过牌 false不能过牌
     */
    public boolean passCards(long playerId) {
        PlayerSeatInfo info = gameDataVo.getCurrentPlayerSeatInfo();
        //不是当前玩家执行
        if (notDoOperation(playerId, info)) {
            return true;
        }
        //下注小于最大下注 不能过牌
        Long oldBet = gameDataVo.getBaseBetInfo().getOrDefault(playerId, 0L);
        if (oldBet < gameDataVo.getMaxBetValue()) {
            return false;
        }
        sampleOperation(info, PokerConstant.PlayerOperation.PASS);
        return true;
    }

    /**
     * 弃牌
     */
    public void discardCard(long playerId) {
        PlayerSeatInfo info = gameDataVo.getCurrentPlayerSeatInfo();
        //不是当前玩家执行
        if (notDoOperation(playerId, info)) {
            return;
        }
        sampleOperation(info, PokerConstant.PlayerOperation.DISCARD);
    }

    /**
     * 是否不能进行该操作
     *
     * @return true 不能进行该操作
     */
    private boolean notDoOperation(long playerId, PlayerSeatInfo info) {
        return info.getPlayerId() != playerId || info.isOver() || info.getOperationType() != PokerConstant.PlayerOperation.NONE;
    }

    public boolean hasAllIn() {
        return gameDataVo.getPlayerSeatInfoList()
            .stream()
            .filter(playerSeatInfo -> !playerSeatInfo.isDelState())
            .anyMatch(info -> info.getOperationType() == PokerConstant.PlayerOperation.ALL_IN);
    }

    /**
     * 开启下一轮还是进行结算
     */
    public void startNextRoundOrSettlement() {
        if (gameDataVo.getPlayerGameNnm() == 0) {
            setCurrentGamePhase(new BaseWaitReadyPhase<>(this));
            gameDataVo.resetData(this);
            return;
        }
        boolean nextRoundOrSettlement = isNextRoundOrSettlement();
        //结算
        if (nextRoundOrSettlement) {
            addPokerPhaseTimer(new TexasSettlementPhase(this));
        } else {
            //清除上轮数据
            for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
                if (isOverGame(info)) {
                    continue;
                }
                info.setOver(false);
                info.setOperationType(PokerConstant.PlayerOperation.NONE);
            }
            PlayerSeatInfo nextExePlayer = getNextExePlayer();
            //如果找不到下一轮说明全all 如果只剩一个没allin的也直接结算
            if (Objects.isNull(nextExePlayer) || !isAllAllIn(nextExePlayer.getPlayerId())) {
                gameDataVo.setSettlement(ALL_SETTLEMENT);
                //设置阶段结束事件
                addPokerPhaseTimer(new TexasSettlementPhase(this));
                return;
            }
            if (hasAllIn()) {
                gameDataVo.setPool(buildPots(gameDataVo));
            }
            //下一轮
            gameDataVo.nextRound();
            //添加记录
            TexasSaveHistory texasHistory = gameDataVo.getTexasHistory();
            TexasHistoryRoundInfo texasHistoryRoundInfo = new TexasHistoryRoundInfo(gameDataVo.getRound());
            texasHistoryRoundInfo.roundInfo = new ArrayList<>();
            texasHistory.getTexasHistoryRoundInfos().add(texasHistoryRoundInfo);
            boolean isFlipRound = gameDataVo.getRound() == FLIP_CARDS_ROUND;
            int fixChips = gameDataVo.getMaxBetValue() > 0 || isFlipRound ? FIX_CHIPS : 0;
            gameDataVo.setMaxBetValue(0);
            gameDataVo.getRoundBet().clear();
            //设置本轮当前底池押注
            if (gameDataVo.getRound() > INIT_ROUND) {
                TexasHistoryRoundInfo historyRoundInfo = gameDataVo.getHistoryRoundInfo();
                historyRoundInfo.potAllBet =
                    gameDataVo.getPool().stream().map(Pot::getAmount).collect(Collectors.toList());
            }
            //发牌
            int sendCardNum = isFlipRound ? SEND_CARD_NUM : ADD_CARDS;
            List<Integer> addCards = new ArrayList<>(sendCardNum);
            for (int i = 0; i < sendCardNum; i++) {
                Integer card = gameDataVo.getCards().removeFirst();
                addCards.add(card);
            }
            List<Integer> publicCards = gameDataVo.getPublicCards();
            if (publicCards == null) {
                texasHistory.setPreFlop(TexasDataHelper.getClientId(gameDataVo, addCards));
                gameDataVo.setPublicCards(addCards);
            } else {
                publicCards.addAll(addCards);
                Map<Integer, PokerCard> cardMap = TexasDataHelper.getCardListMap(TexasDataHelper.getPoolId(gameDataVo));
                Integer cfgCardId = publicCards.getLast();
                //添加记录
                if (texasHistory.getThirdCardId() == 0) {
                    texasHistory.setThirdCardId(cardMap.get(cfgCardId).getClientId());
                } else if (texasHistory.getFourthCardId() == 0) {
                    texasHistory.setFourthCardId(cardMap.get(cfgCardId).getClientId());
                }
            }
            addNextTimer(nextExePlayer, sendCardNum, fixChips + FLIP_CARDS);
            //下发本轮数据
            Map<Long, PlayerSeatInfo> collect = gameDataVo.getPlayerSeatInfoList().stream()
                .filter(info -> !info.isDelState())
                .collect(Collectors.toMap(PlayerSeatInfo::getPlayerId, info -> info));
            for (SeatInfo seatInfo : gameDataVo.getSeatInfo().values()) {
                PlayerSeatInfo info = null;
                if (seatInfo.isJoinGame()) {
                    info = collect.get(seatInfo.getPlayerId());
                }
                NotifyTexasPublicCardChange notifyTexasPublicCardChange = TexasBuilder.getNotifyPublicCardChange(info
                    , nextExePlayer, addCards, gameDataVo);
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(seatInfo.getPlayerId(),
                    notifyTexasPublicCardChange));
            }
        }
    }

    @Override
    public void onPlayerLeaveRoomAction(RoomPlayer roomPlayer, SeatInfo remove) {
        //如果在游戏中删除数据
        gameDataVo.getTempGold().remove(remove.getPlayerId());
    }

    @Override
    public void onRunGamePlayerLeaveRoom(SeatInfo remove) {
        PlayerSeatInfo currentPlayerSeatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (currentPlayerSeatInfo.getPlayerId() == remove.getPlayerId()) {
            //如果他是执行人 直接下一轮或结算   他不是执行人 剩一个直接结算
            PlayerSeatInfo nextExePlayer = getNextExePlayer();
            if (Objects.isNull(nextExePlayer)) {
                //判断是否结算开启下一轮
                startNextRoundOrSettlement();
            } else {
                addNextPlayerAndBroadcast(nextExePlayer, new NotifyPokerSampleCardOperation());
            }
        } else {
            if (isNextRoundOrSettlement()) {
                startNextRoundOrSettlement();
            }
        }
    }

    /**
     * 添加下一个玩家的执行timer
     */
    public void addNextTimer(PlayerSeatInfo nextExePlayer, int sendCardNum) {
        addNextTimer(nextExePlayer, sendCardNum, 0);
    }

    /**
     * 添加下一个玩家的执行timer
     */
    public void addNextTimer(PlayerSeatInfo nextExePlayer, int sendCardNum, int fixTime) {
        int time = TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.PLAY_CARDS);
        int sendTime = TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS);
        gameDataVo.addTimerId();
        addPlayerTimer(new TexasProcessorHandler(nextExePlayer.getPlayerId(), gameDataVo.getId(), this,
                gameDataVo.getTimerId()),
            time + sendTime * sendCardNum + fixTime);
    }

    /**
     * 玩家是否已经结束本轮游戏
     */
    public boolean isOverGame(PlayerSeatInfo info) {
        return info.getOperationType() == PokerConstant.PlayerOperation.ALL_IN
            || info.getOperationType() == PokerConstant.PlayerOperation.DISCARD || info.isDelState();
    }

    /**
     * 判断是结算还是下一轮
     *
     * @return true 结算 false 下一轮
     */
    public boolean isNextRoundOrSettlement() {
        List<PlayerSeatInfo> infos = gameDataVo.getPlayerSeatInfoList();
        Pair<Integer, Integer> result = getGetRemainingPlayerNum(infos);
        //判断弃牌到只剩一名玩家
        if (result.getFirst() == infos.size() - 1) {
            gameDataVo.setSettlement(DISCARD_SETTLEMENT);
            return true;
        }
        return gameDataVo.getRound() >= TexasConstant.Common.MAX_ROUND && result.getSecond() == 0;
    }

    /**
     * 获取剩余可操作玩家数
     *
     * @return 弃牌数 没操作数
     */
    private Pair<Integer, Integer> getGetRemainingPlayerNum(List<PlayerSeatInfo> infos) {
        int discardNum = 0;
        int notOverNum = 0;
        for (PlayerSeatInfo info : infos) {
            if (!info.isOver()) {
                notOverNum++;
            } else {
                if (info.getOperationType() == PokerConstant.PlayerOperation.DISCARD || info.isDelState()) {
                    discardNum++;
                }
            }
        }
        return Pair.newPair(discardNum, notOverNum);
    }

    /**
     * 弃牌和过牌
     */
    public void sampleOperation(PlayerSeatInfo info, int Operation) {
        long playerId = info.getPlayerId();
        info.setOperationType(Operation);
        info.setOver(true);
        //添加记录
        TexasHistoryRoundInfo historyRoundInfo = gameDataVo.getHistoryRoundInfo();
        historyRoundInfo.roundInfo.add(TexasBuilder.getTexasHistoryPlayerInfo(info, gameDataVo, 0));
        //通知其他人
        NotifyPokerSampleCardOperation notifyPokerSampleCardOperation = new NotifyPokerSampleCardOperation();
        notifyPokerSampleCardOperation.operationType = info.getOperationType();
        notifyPokerSampleCardOperation.playerId = playerId;
        PlayerSeatInfo nextExePlayer = getNextExePlayer();
        if (Objects.nonNull(nextExePlayer)) {
            addNextPlayerAndBroadcast(nextExePlayer, notifyPokerSampleCardOperation);
        } else {
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyPokerSampleCardOperation));
            //判断结算 还是 下一轮
            startNextRoundOrSettlement();
        }
    }


    /**
     * 获取下一个执行的玩家Id
     */
    public PlayerSeatInfo getNextExePlayer() {
        List<PlayerSeatInfo> playerSeatInfoList = gameDataVo.getPlayerSeatInfoList();
        int index = gameDataVo.getIndex();
        //未弃牌人数为1不用接下来的操作了
        Pair<Integer, Integer> playerNum = getGetRemainingPlayerNum(playerSeatInfoList);
        if (playerNum.getFirst() == playerSeatInfoList.size() - 1) {
            return null;
        }
        //找还未操作过的玩家
        for (int i = 1; i < playerSeatInfoList.size(); i++) {
            int newIndex = (index + i) % playerSeatInfoList.size();
            PlayerSeatInfo info = playerSeatInfoList.get(newIndex);
            if (!info.isOver() && !isOverGame(info)) {
                gameDataVo.setIndex(newIndex);
                return info;
            }
        }
        //寻找有加注的玩家
        if (gameDataVo.getMaxBetValue() > 0) {
            for (int i = 1; i < playerSeatInfoList.size(); i++) {
                int newIndex = (index + i) % playerSeatInfoList.size();
                PlayerSeatInfo info = playerSeatInfoList.get(newIndex);
                Long betValue = gameDataVo.getBaseBetInfo().getOrDefault(info.getPlayerId(), 0L);
                if (betValue < gameDataVo.getMaxBetValue() && !isOverGame(info)) {
                    info.setOperationType(PokerConstant.PlayerOperation.NONE);
                    info.setOver(false);
                    gameDataVo.setIndex(newIndex);
                    return info;
                }
            }
        }
        return null;
    }

    /**
     * 修改玩家金币
     */
    public void changePlayerGold(GamePlayer gamePlayer, long change) {
        if (change > 0) {
            addItem(gamePlayer.getId(), change, ERoomItemReason.GAME_SETTLEMENT);
            gameDataVo.getTempGold().merge(gamePlayer.getId(), change, Long::sum);
        } else {
            deductItem(gamePlayer.getId(), Math.abs(change), ERoomItemReason.GAME_BET);
            gameDataVo.getTempGold().merge(gamePlayer.getId(), change, Long::sum);
        }
    }

    /**
     * 补充临时货币或者下桌
     */
    public boolean addTempGoldOrOutTable(SeatInfo seatInfo, GamePlayer gamePlayer) {
        Long hasTempCurrency = gameDataVo.getTempGold().getOrDefault(seatInfo.getPlayerId(), 0L);
        TexasCfg texasCfg = TexasDataHelper.getTexasCfg(gameDataVo);
        if (hasTempCurrency < texasCfg.getTablecoin()) {
            long defaultCoinsNum = Math.min(TexasDataHelper.getDefaultCoinsNum(gameDataVo), texasCfg.getTablecoin());
            if (gamePlayer.getGold() >= defaultCoinsNum) {
                //增加零时货币
                gameDataVo.getTempGold().put(seatInfo.getPlayerId(), defaultCoinsNum);
                NotifyTexasTempGoldReflush reflush = new NotifyTexasTempGoldReflush();
                reflush.addValue = defaultCoinsNum;
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(gamePlayer.getId(), reflush));
                return true;
            } else {
                seatInfo.setSeatDown(false);
                return false;
            }
        }
        return true;
    }

    /**
     * 构建主池 和 边池
     *
     * @return 第0个索引是主池
     */
    public static List<Pot> buildPots(TexasGameDataVo gameDataVo) {
        List<Pot> pots = new ArrayList<>();
        Map<Long, Long> remaining = new HashMap<>(gameDataVo.getBaseBetInfo()); // 复制，避免修改原数据
        while (!remaining.isEmpty()) {
            long minBet = Collections.min(remaining.values());
            Pot pot = new Pot();

            long potAmount = 0;
            List<Long> toRemove = new ArrayList<>();

            for (Map.Entry<Long, Long> entry : remaining.entrySet()) {
                long playerId = entry.getKey();
                long betAmount = entry.getValue();

                long contribution = Math.min(betAmount, minBet);
                potAmount += contribution;
                pot.addEligiblePlayer(playerId);

                long newAmount = betAmount - contribution;
                if (newAmount == 0) {
                    toRemove.add(playerId);
                } else {
                    entry.setValue(newAmount);
                }
            }

            // 设置底池金额
            pot.addChips(potAmount);

            // 移除已全押的玩家
            for (Long id : toRemove) {
                remaining.remove(id);
            }

            pots.add(pot);
        }

        return pots;
    }

    /**
     * 请求亮牌
     */
    public void reqShowCard(long playerId, TexasGameController controller) {
        //暂定弃牌和最后剩的玩家才可以亮牌
        PlayerSeatInfo info = gameDataVo.getPlayerSeatInfoMap().get(playerId);
        if (Objects.isNull(info)) {
            return;
        }
        NotifyTexasShowCard notifyTexasShowCard = new NotifyTexasShowCard();
        if ((gameDataVo.getSettlement() != 1 && info.getOperationType() != PokerConstant.PlayerOperation.DISCARD) ||
            controller.getCurrentGamePhase() != EGamePhase.GAME_ROUND_OVER_SETTLEMENT) {
            notifyTexasShowCard.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyTexasShowCard));
            return;
        }
        notifyTexasShowCard.playerId = playerId;
        notifyTexasShowCard.cards = TexasDataHelper.getClientId(gameDataVo, info.getCurrentCards());
        HandResult tempHandType = TexasBuilder.getTempHandType(info, gameDataVo);
        if (Objects.nonNull(tempHandType)) {
            notifyTexasShowCard.handType = tempHandType.getHandRank().rank;
        }
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyTexasShowCard));
    }

    /**
     * 换桌
     */
    public void reqChangeTable(PlayerController playerController, TexasGameController controller) {
        AbstractRoomController<Room_ChessCfg, ? extends Room> abstractRoomController = controller.getRoomController();
        Room room = abstractRoomController.getRoom();
        boolean changed =
            roomController.getRoomManager().changeRoom(
                playerController, room.getId(), room.getGameType(), room.getRoomCfgId());
        RepsTexasRoomBaseInfo repsTexasRoomBaseInfo = new RepsTexasRoomBaseInfo(changed ? Code.SUCCESS : Code.FAIL);
        playerController.send(repsTexasRoomBaseInfo);
    }

    /**
     * 请求历史记录
     *
     * @param playerId 玩家id
     * @param req      请求
     */
    public void reqTexasHistory(long playerId, ReqTexasHistory req) {
        RepsTexasHistory repsTexasHistory = new RepsTexasHistory(Code.SUCCESS);
        int size = gameDataVo.getTexasHistoryList().size();
        if (size == 0) {
            repsTexasHistory.maxRecodeNum = 0;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, repsTexasHistory));
            return;
        }
        if (req.index > size) {
            repsTexasHistory.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, repsTexasHistory));
            return;
        }
        repsTexasHistory.maxRecodeNum = size;
        TexasSaveHistory texasSaveHistory = gameDataVo.getTexasHistoryList().get(req.index == -1 ?
            repsTexasHistory.maxRecodeNum - 1 : req.index - 1);
        repsTexasHistory.history = buildTexasHistory(playerId, texasSaveHistory);
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, repsTexasHistory));
    }

    public TexasHistory buildTexasHistory(long playerId, TexasSaveHistory texasSaveHistory) {
        TexasHistory texasHistory = new TexasHistory();
        texasHistory.BBValue = texasSaveHistory.getBBValue();
        texasHistory.SBValue = texasSaveHistory.getSBValue();
        texasHistory.texasHistoryRoundInfos = new ArrayList<>(texasSaveHistory.getTexasHistoryRoundInfos());
        texasHistory.fourthCardId = texasSaveHistory.getFourthCardId();
        texasHistory.thirdCardId = texasSaveHistory.getThirdCardId();
        texasHistory.id = texasSaveHistory.getId();
        texasHistory.preFlop = texasSaveHistory.getPreFlop();
        Map<Long, List<Integer>> allCards = texasSaveHistory.getAllCards();
        Map<Long, TexasHistoryPlayerInfo> sendTotalPlayerBetInfo = new HashMap<>();
        Map<Long, TexasHistoryPlayerInfo> totalPlayerBetInfo = texasSaveHistory.getTotalPlayerBetInfoMap();
        //构建自己的基本信息
        for (TexasHistoryPlayerInfo playerInfo : totalPlayerBetInfo.values()) {
            if (playerId == 0 || playerInfo.playerId == playerId) {
                sendTotalPlayerBetInfo.put(playerInfo.playerId, TexasBuilder.getTexasHistoryPlayerInfo(playerInfo,
                    allCards));
                continue;
            }
            sendTotalPlayerBetInfo.put(playerInfo.playerId, TexasBuilder.getTexasHistoryPlayerInfo(playerInfo, null));
        }
        if (Objects.nonNull(texasSaveHistory.getSettlementAllCards())) {
            TexasHistoryRoundInfo texasHistoryRoundInfo = new TexasHistoryRoundInfo(-1);
            texasHistoryRoundInfo.potAllBet = texasSaveHistory.getPotList();
            texasHistoryRoundInfo.roundInfo = new ArrayList<>();
            for (Map.Entry<Long, List<Integer>> entry : texasSaveHistory.getSettlementAllCards().entrySet()) {
                Long settlementPlayerId = entry.getKey();
                TexasHistoryPlayerInfo texasHistoryPlayerInfo = totalPlayerBetInfo.get(settlementPlayerId);
                if (Objects.isNull(texasHistoryPlayerInfo)) {
                    continue;
                }
                //构建总的
                TexasHistoryPlayerInfo playerInfo = sendTotalPlayerBetInfo.get(settlementPlayerId);
                playerInfo.cardIds = allCards.get(settlementPlayerId);
                TexasHistoryPlayerInfo roundPlayerInfo =
                    TexasBuilder.getTexasHistoryPlayerInfo(texasHistoryPlayerInfo, allCards);
                roundPlayerInfo.layCardIds = entry.getValue();
                texasHistoryRoundInfo.roundInfo.add(roundPlayerInfo);

            }
            texasHistory.texasHistoryRoundInfos.add(texasHistoryRoundInfo);
        }
        texasHistory.totalPlayerBetInfo = new ArrayList<>(sendTotalPlayerBetInfo.values());
        return texasHistory;
    }
}
