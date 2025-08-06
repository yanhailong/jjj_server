package com.jjg.game.poker.game.texas.room;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerSampleCardOperation;
import com.jjg.game.poker.game.common.message.req.ReqPokerBet;
import com.jjg.game.poker.game.common.message.req.ReqPokerSampleCardOperation;
import com.jjg.game.poker.game.sample.bean.TexasCfg;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.data.Pot;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.gamephase.TexasPlayCardPhase;
import com.jjg.game.poker.game.texas.gamephase.TexasProcessorHandler;
import com.jjg.game.poker.game.texas.gamephase.TexasSettlementPhase;
import com.jjg.game.poker.game.texas.message.TexasBuilder;
import com.jjg.game.poker.game.texas.message.bean.TexasPlayerInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasBet;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasPublicCardChange;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasShowCard;
import com.jjg.game.poker.game.texas.message.reps.RepsTexasRoomBaseInfo;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.poker.game.texas.util.HandResult;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.*;
import java.util.stream.Collectors;

import static com.jjg.game.poker.game.texas.constant.TexasConstant.Common.*;

/**
 * @author lm
 */
@GameController(gameType = EGameType.TEXAS)
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
        if (getCurrentGamePhase() == EGamePhase.PLAY_CART) {
            if (Objects.nonNull(gameDataVo.getPublicCards())) {
                repsTexasRoomBaseInfo.publicCards = PokerDataHelper.getClientId(gameDataVo.getPublicCards(), TexasDataHelper.getPoolId(gameDataVo));
            }
            repsTexasRoomBaseInfo.waitPlayerId = gameDataVo.getCurrentPlayerSeatInfo().getPlayerId();
            repsTexasRoomBaseInfo.waitEndTime = gameDataVo.getPlayerTimerEvent().getNextTime();
        }
        if (getCurrentGamePhase() != EGamePhase.WAIT_READY) {
            List<Pot> pool = gameDataVo.getPool();
            repsTexasRoomBaseInfo.bottomPool = pool.get(0).getAmount();
            repsTexasRoomBaseInfo.seatId = gameDataVo.getDealerSeatId();
            if (pool.size() > 1) {
                repsTexasRoomBaseInfo.edgePool = pool.subList(1, pool.size())
                        .stream()
                        .map(Pot::getAmount)
                        .toList();
            }
        }
        repsTexasRoomBaseInfo.notifyTexasSettlementInfo = gameDataVo.getNotifySettlementInfo();
        List<TexasPlayerInfo> playerInfos = new ArrayList<>();
        //构建玩家信息
        for (Map.Entry<Integer, SeatInfo> entry : gameDataVo.getSeatInfo().entrySet()) {
            SeatInfo seatInfo = entry.getValue();
            Integer seatId = entry.getKey();
            PokerPlayerInfo pokerPlayerInfo = new PokerPlayerInfo();
            long playerId = seatInfo.getPlayerId();
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
            pokerPlayerInfo.playerId = playerId;
            pokerPlayerInfo.accountNumber = gameDataVo.getTempGold().getOrDefault(playerId, 0L);
            pokerPlayerInfo.name = gamePlayer.getNickName();
            pokerPlayerInfo.icon = gamePlayer.getNickName();
            pokerPlayerInfo.seatIndex = seatId;
            pokerPlayerInfo.status = seatInfo.isSeatDown();
            pokerPlayerInfo.playerStatus = seatInfo.isJoinGame();
            TexasPlayerInfo texasPlayerInfo = new TexasPlayerInfo();
            texasPlayerInfo.pokerPlayerInfo = pokerPlayerInfo;
            texasPlayerInfo.totalBet = gameDataVo.getBaseBetInfo().getOrDefault(playerId, 0L);
            for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
                if (info.getPlayerId() == entry.getKey()) {
                    texasPlayerInfo.handCards = info.getCurrentCards();
                    pokerPlayerInfo.operationType = info.getOperationType();
                }
            }
            playerInfos.add(texasPlayerInfo);
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
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerController.playerId(), repsTexasRoomBaseInfo));
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
            //正常下注
            betValue = reqPokerBet.betValue;
        } else if (reqPokerBet.betType == PokerConstant.PlayerOperation.FOLLOW_CARD) {
            //跟注
            betValue = gameDataVo.getMaxBetValue() - baseBetInfo.getOrDefault(playerId, 0L);
        }
        Long remain = gameDataVo.getTempGold().get(playerId);
        long tempTotal = baseBetInfo.getOrDefault(playerId, 0L) + betValue;
        //TODO 大大小小
        if (betValue <= 0 || betValue > remain || !allIn && tempTotal < gameDataVo.getMaxBetValue()) {
            //TODO 通知 ~
            passCards(playerId);
            return;
        }
        info.setOperationType(reqPokerBet.betType);
        info.setOver(true);
        //通知
        changePlayerGold(gamePlayer, -betValue);
        baseBetInfo.merge(playerId, betValue, Long::sum);
        baseBetInfo.entrySet().stream().max(Comparator.comparingLong(Map.Entry::getValue))
                .ifPresent((entry) -> gameDataVo.setMaxBetValue(entry.getValue()));
        NotifyTexasBet notifyTexasBet = new NotifyTexasBet();
        notifyTexasBet.betType = reqPokerBet.betType;
        notifyTexasBet.betValue = betValue;
        notifyTexasBet.playerId = playerId;
        if (hasAllIn()) {
            gameDataVo.setPool(buildPots(gameDataVo));
        } else {
            Pot pot = gameDataVo.getPool().get(0);
            pot.addChips(betValue);
            pot.addEligiblePlayer(playerId);
        }
        notifyTexasBet.poolList = gameDataVo.getPool().stream().map(Pot::getAmount).collect(Collectors.toList());
        PlayerSeatInfo nextExePlayer = getNextExePlayer(false);
        if (Objects.nonNull(nextExePlayer)) {
            addNextTimer(nextExePlayer, 0);
            notifyTexasBet.nextPlayerId = nextExePlayer.getPlayerId();
            notifyTexasBet.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyTexasBet));
        } else {
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyTexasBet));
            startNextRoundOrSettlement();
        }
    }

    public boolean hasAllIn() {
        return gameDataVo.getPlayerSeatInfoList()
                .stream()
                .anyMatch(info -> info.getOperationType() == PokerConstant.PlayerOperation.ALL_IN);
    }

    /**
     * 获取初始执行的index
     */
    public int getInitIndex() {
        List<PlayerSeatInfo> playerSeatInfoList = gameDataVo.getPlayerSeatInfoList();
        int size = playerSeatInfoList.size();
        int dealerIndex = gameDataVo.getDealerIndex();
        if (size <= 3) {
            return dealerIndex;
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
        log.info("玩家过牌 {}", playerId);
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
        log.info("玩家弃牌 {}", playerId);
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

    /**
     * 开启下一轮还是进行结算
     */
    public void startNextRoundOrSettlement() {
        boolean nextRoundOrSettlement = isNextRoundOrSettlement();
        //结算
        if (nextRoundOrSettlement) {
            log.info("进行结算");
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
            PlayerSeatInfo nextExePlayer = getNextExePlayer(false);
            //如果找不到下一轮说明全all
            if (Objects.isNull(nextExePlayer)) {
                gameDataVo.setSettlement(ALL_SETTLEMENT);
                //设置阶段结束事件
                addPokerPhaseTimer(new TexasSettlementPhase(this));
                return;
            }
            //下一轮
            gameDataVo.nextRound();
            gameDataVo.setMaxBetValue(0);
            log.info("进行下一轮: {}", gameDataVo.getRound());
            //发牌
            int sendCardNum = gameDataVo.getRound() == FLIP_CARDS_ROUND ? SEND_CARD_NUM : ADD_CARDS;
            List<Integer> addCards = new ArrayList<>(sendCardNum);
            for (int i = 0; i < sendCardNum; i++) {
                Integer card = gameDataVo.getCards().remove(0);
                addCards.add(card);
            }
            if (gameDataVo.getPublicCards() == null) {
                gameDataVo.setPublicCards(addCards);
            } else {
                gameDataVo.getPublicCards().addAll(addCards);
            }
            addNextTimer(nextExePlayer, sendCardNum);
            //下发本轮数据
            Map<Long, PlayerSeatInfo> collect = gameDataVo.getPlayerSeatInfoList().stream()
                    .collect(Collectors.toMap(PlayerSeatInfo::getPlayerId, info -> info));
            for (SeatInfo seatInfo : gameDataVo.getSeatInfo().values()) {
                PlayerSeatInfo info = null;
                if (seatInfo.isJoinGame()) {
                    info = collect.get(seatInfo.getPlayerId());
                }
                NotifyTexasPublicCardChange notifyTexasPublicCardChange = TexasBuilder.getNotifyPublicCardChange(info, nextExePlayer, addCards, gameDataVo);
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(seatInfo.getPlayerId(), notifyTexasPublicCardChange));
            }
        }
    }

    @Override
    public void onPlayerJoinRoomAction(GamePlayer gamePlayer) {
        //增加临时货币
        gameDataVo.getTempGold().put(gamePlayer.getId(), TexasDataHelper.getDefaultCoinsNum(gameDataVo));
    }

    @Override
    public void onPlayerLeaveRoomAction(RoomPlayer roomPlayer, SeatInfo remove) {
        //如果在游戏中删除数据
        gameDataVo.getTempGold().remove(remove.getPlayerId());
        if (inRunPhase()) {
            runPlayerSeatChange(remove, remove.isSeatDown() && remove.isJoinGame());
        }
    }

    /**
     * 运行时玩家座位改变
     */
    public void runPlayerSeatChange(SeatInfo remove, boolean isPlaying) {
        //正在游玩
        List<PlayerSeatInfo> playerSeatInfos = gameDataVo.getPlayerSeatInfoList();
        if (isPlaying) {
            final Iterator<PlayerSeatInfo> each = playerSeatInfos.iterator();
            while (each.hasNext()) {
                PlayerSeatInfo next = each.next();
                if (next.getPlayerId() == remove.getPlayerId()) {
                    each.remove();
                    break;
                }
            }
            remove.setSeatDown(false);
            remove.setJoinGame(false);
        }
        //如果他是执行人 直接下一轮或结算   他不是执行人 剩一个直接结算
        PlayerSeatInfo nextExePlayer = getNextExePlayer(true);
        if (Objects.isNull(nextExePlayer)) {
            //判断是否结算开启下一轮
            startNextRoundOrSettlement();
        }
    }

    public boolean inRunPhase() {
        return getCurrentGamePhase() == EGamePhase.PLAY_CART;
    }

    public void addNextTimer(PlayerSeatInfo nextExePlayer, int sendCardNum) {
        int time = TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.PLAY_CARDS);
        int sendTime = TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS);
        gameDataVo.addTimerId();
        addPlayerTimer(new TexasProcessorHandler(nextExePlayer.getPlayerId(), gameDataVo.getId(), this, gameDataVo.getTimerId()),
                time + sendTime * sendCardNum);
    }

    /**
     * 玩家是否已经结束本轮游戏
     */
    public boolean isOverGame(PlayerSeatInfo info) {
        return info.getOperationType() == PokerConstant.PlayerOperation.ALL_IN
                || info.getOperationType() == PokerConstant.PlayerOperation.DISCARD;
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
        return gameDataVo.getRound() == TexasConstant.Common.MAX_ROUND && result.getSecond() == 0;
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
                if (info.getOperationType() == PokerConstant.PlayerOperation.DISCARD) {
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
        //通知其他人
        NotifyPokerSampleCardOperation notifyPokerSampleCardOperation = new NotifyPokerSampleCardOperation();
        notifyPokerSampleCardOperation.operationType = info.getOperationType();
        notifyPokerSampleCardOperation.playerId = playerId;
        PlayerSeatInfo nextExePlayer = getNextExePlayer(false);
        if (Objects.nonNull(nextExePlayer)) {
            addNextTimer(nextExePlayer, 0);
            notifyPokerSampleCardOperation.nextPlayerId = nextExePlayer.getPlayerId();
            notifyPokerSampleCardOperation.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyPokerSampleCardOperation));
        } else {
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyPokerSampleCardOperation));
            //判断结算 还是 下一轮
            startNextRoundOrSettlement();
        }
    }


    /**
     * 获取下一个执行的玩家Id
     *
     * @param tryGet true尝试获取下一个玩家
     */
    public PlayerSeatInfo getNextExePlayer(boolean tryGet) {
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
                if (!tryGet) {
                    gameDataVo.setIndex(newIndex);
                }
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
                    if (!tryGet) {
                        info.setOperationType(PokerConstant.PlayerOperation.NONE);
                        info.setOver(false);
                        gameDataVo.setIndex(newIndex);
                    }
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
            gamePlayer.setGold(gamePlayer.getGold() + change);
            gameDataVo.getTempGold().merge(gamePlayer.getId(), change, Long::sum);
        } else {
            gamePlayer.setGold(gamePlayer.getGold() - change);
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
                return true;
            } else {
                seatInfo.setSeatDown(false);
                return false;
            }
        }
        return false;
    }

    /**
     * 构建主池 和 边池
     *
     * @return 第0个索引是主池
     */
    public List<Pot> buildPots(TexasGameDataVo gameDataVo) {
        Map<Long, Long> bets = new HashMap<>(gameDataVo.getBaseBetInfo()); // 复制下注信息
        List<Pot> pots = new ArrayList<>();
        while (!bets.isEmpty()) {
            long minBet = Collections.min(bets.values());
            Pot pot = new Pot();
            for (Iterator<Map.Entry<Long, Long>> it = bets.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Long, Long> entry = it.next();
                long playerId = entry.getKey();
                long betAmount = entry.getValue();

                long contribution = Math.min(betAmount, minBet);
                pot.addChips(contribution * bets.size()); // 每人出 minBet，加总
                pot.addEligiblePlayer(playerId);
                long remaining = betAmount - contribution;
                if (remaining == 0) {
                    it.remove(); // 移除已用完筹码的玩家
                } else {
                    entry.setValue(remaining); // 更新剩余下注
                }
            }
            pots.add(pot);
        }
        return pots;
    }

    /**
     * 请求亮牌
     */
    public void reqShowCard(long playerId, TexasGameController controller) {
        //暂定弃牌玩家才可以亮牌
        Optional<PlayerSeatInfo> playerSeatInfo = gameDataVo.getPlayerSeatInfoList().
                stream()
                .filter(info -> info.getPlayerId() == playerId &&
                        info.getOperationType() == PokerConstant.PlayerOperation.DISCARD)
                .findFirst();
        NotifyTexasShowCard notifyTexasShowCard = new NotifyTexasShowCard();
        if (playerSeatInfo.isEmpty() || controller.getCurrentGamePhase() != EGamePhase.GAME_ROUND_OVER_SETTLEMENT) {
            notifyTexasShowCard.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyTexasShowCard));
            return;
        }
        PlayerSeatInfo info = playerSeatInfo.get();
        notifyTexasShowCard.playerId = playerId;
        notifyTexasShowCard.cards = TexasDataHelper.getClientId(info.getCurrentCards(), TexasDataHelper.getPoolId(gameDataVo));
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
        boolean changed = abstractRoomController.changeRoom(playerController, room.getId(), room.getGameType(), room.getRoomCfgId());
        RepsTexasRoomBaseInfo repsTexasRoomBaseInfo = new RepsTexasRoomBaseInfo(changed ? Code.SUCCESS : Code.FAIL);
        playerController.send(repsTexasRoomBaseInfo);
    }
}
