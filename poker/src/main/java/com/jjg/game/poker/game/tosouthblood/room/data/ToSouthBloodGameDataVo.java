package com.jjg.game.poker.game.tosouthblood.room.data;

import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.tosouthblood.data.ToSouthBloodDataHelper;
import com.jjg.game.poker.game.tosouthblood.data.ToSouthBloodSettlementContext;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.*;

public class ToSouthBloodGameDataVo extends BasePokerGameDataVo {
    private long roomBet;
    // 上一家出牌的人所出的牌  中间可能有pass的
    private List<Integer> lastPlayCards;
    // 上一家出牌牌型
    private int lastPlayCardsType;
    // 上一家出牌人座位
    private int lastPlaySeatId;
    // 当前轮的首出者
    private int roundLeaderSeatId;
    // 是否为第一轮
    private boolean isFirstRound = true;
    // 当前轮连续过牌次数
    private int passCount;
    // 本轮已过牌玩家列表
    private Set<Integer> curRoundPassedPlayerSeats = new HashSet<>();
    // 当前回合出牌记录 (用于炸弹结算)
    private final List<ToSouthBloodRoundRecord> currentRoundPlays = new ArrayList<>();
    // 炸弹结算积分变动 (playerId -> score change)
    private final Map<Long, Long> bombSettlementMap = new HashMap<>();

    private Map<Long, List<Integer>> playerHighlightCards = new HashMap<>(); // playerId -> highlightCardIds

    // 准备阶段：已准备的玩家ID集合
    private Set<Long> readyPlayerIds = new HashSet<>();
    // 准备阶段：已启动准备倒计时的玩家ID集合（防止重复调度定时器）
    private Set<Long> readyTimerScheduled = new HashSet<>();
    // 准备倒计时版本号（playerId -> version），用于退出房间后使旧定时器失效
    private final Map<Long, Long> readyTimerVersion = new HashMap<>();
    // 通杀结算上下文（开局阶段检测通杀后保存，供准备完成后判断走结算还是打牌）
    private ToSouthBloodSettlementContext instantWinContext;
    // 一局日志累积器（记录从发牌到结算的完整流水，结算时统一输出）
    private ToSouthBloodGameLog gameLog = new ToSouthBloodGameLog();

    // ========== 跨局保留字段（不在resetData中清除） ==========
    // 上一局的赢家ID（最先出完牌的人），0表示没有上一局
    private long lastGameWinnerPlayerId;
    // 上一局的玩家ID集合，用于判断是否同桌续局
    private Set<Long> lastGamePlayerIds = new HashSet<>();
    // 玩家连赢/连输计数（正=连赢, 负=连输），跨局保留，从Redis加载
    private Map<Long, Integer> playerWinStreakMap = new HashMap<>();
    // 玩家总盈亏（正=盈利, 负=亏损），跨局保留，从Redis加载
    private Map<Long, Long> playerTotalProfitMap = new HashMap<>();

    private Set<Long> exitPlayerIds = new HashSet<>();

    // ========== 血战专用字段（每局重置） ==========
    // 按出完牌顺序记录玩家ID，第1个出完的排第0，第3个出完后牌局结束
    private final List<Long> finishedPlayerOrder = new ArrayList<>();
    // 血战中间结算累计净值（playerId -> 累计净赢/净输），不含炸弹结算
    private final Map<Long, Long> bloodWinSettlementMap = new HashMap<>();

    public Set<Long> getExitPlayerIds() {
        return exitPlayerIds;
    }

    public void setExitPlayerIds(Set<Long> exitPlayerIds) {
        this.exitPlayerIds = exitPlayerIds;
    }

    public List<Long> getFinishedPlayerOrder() {
        return finishedPlayerOrder;
    }

    /**
     * 记录玩家出完牌，返回其名次（1、2、3）
     */
    public int addFinishedPlayer(long playerId) {
        finishedPlayerOrder.add(playerId);
        return finishedPlayerOrder.size();
    }

    public Map<Long, Long> getBloodWinSettlementMap() {
        return bloodWinSettlementMap;
    }

    /**
     * 累加血战中间结算金额
     */
    public void addBloodSettlement(long playerId, long delta) {
        bloodWinSettlementMap.merge(playerId, delta, Long::sum);
    }

    /**
     * 必须初始化的参数是房间配置RoomCfg，如果后续子类添加数据需要在自己的构造函数中添加
     *
     * @param roomCfg
     */


    public ToSouthBloodGameDataVo(Room_ChessCfg roomCfg) {
        super(roomCfg);
    }

    public long getRoomBet() {
        return roomBet;
    }

    public void setRoomBet(long roomBet) {
        this.roomBet = roomBet;
    }

    public List<Integer> getLastPlayCards() {
        return lastPlayCards;
    }

    public void setLastPlayCards(List<Integer> lastPlayCards) {
        this.lastPlayCards = lastPlayCards;
    }

    public int getLastPlayCardsType() {
        return lastPlayCardsType;
    }

    public void setLastPlayCardsType(int lastPlayCardsType) {
        this.lastPlayCardsType = lastPlayCardsType;
    }

    public int getLastPlaySeatId() {
        return lastPlaySeatId;
    }

    public void setLastPlaySeatId(int lastPlaySeatId) {
        this.lastPlaySeatId = lastPlaySeatId;
    }

    public int getRoundLeaderSeatId() {
        return roundLeaderSeatId;
    }

    public void setRoundLeaderSeatId(int roundLeaderSeatId) {
        this.roundLeaderSeatId = roundLeaderSeatId;
    }

    public boolean isFirstRound() {
        return isFirstRound;
    }

    public void setFirstRound(boolean firstRound) {
        isFirstRound = firstRound;
    }

    public int getPassCount() {
        return passCount;
    }

    public void setPassCount(int passCount) {
        this.passCount = passCount;
    }

    public Set<Integer> getCurRoundPassedPlayerSeats() {
        return curRoundPassedPlayerSeats;
    }

    public void setCurRoundPassedPlayerSeats(Set<Integer> curRoundPassedPlayerSeats) {
        this.curRoundPassedPlayerSeats = curRoundPassedPlayerSeats;
    }

    public List<ToSouthBloodRoundRecord> getCurrentRoundPlays() {
        return currentRoundPlays;
    }

    public Map<Long, Long> getBombSettlementMap() {
        return bombSettlementMap;
    }

    public Map<Long, List<Integer>> getPlayerHighlightCards() {
        return playerHighlightCards;
    }

    public void setPlayerHighlightCards(Map<Long, List<Integer>> playerHighlightCards) {
        this.playerHighlightCards = playerHighlightCards;
    }

    public Set<Long> getReadyPlayerIds() {
        return readyPlayerIds;
    }

    public Set<Long> getReadyTimerScheduled() {
        return readyTimerScheduled;
    }

    public Map<Long, Long> getReadyTimerVersion() {
        return readyTimerVersion;
    }

    public ToSouthBloodSettlementContext getInstantWinContext() {
        return instantWinContext;
    }

    public void setInstantWinContext(ToSouthBloodSettlementContext instantWinContext) {
        this.instantWinContext = instantWinContext;
    }

    public long getLastGameWinnerPlayerId() {
        return lastGameWinnerPlayerId;
    }

    public void setLastGameWinnerPlayerId(long lastGameWinnerPlayerId) {
        this.lastGameWinnerPlayerId = lastGameWinnerPlayerId;
    }

    public Set<Long> getLastGamePlayerIds() {
        return lastGamePlayerIds;
    }

    public void setLastGamePlayerIds(Set<Long> lastGamePlayerIds) {
        this.lastGamePlayerIds = lastGamePlayerIds;
    }

    public Map<Long, Integer> getPlayerWinStreakMap() {
        return playerWinStreakMap;
    }

    public void setPlayerWinStreakMap(Map<Long, Integer> playerWinStreakMap) {
        this.playerWinStreakMap = playerWinStreakMap;
    }

    public Map<Long, Long> getPlayerTotalProfitMap() {
        return playerTotalProfitMap;
    }

    public void setPlayerTotalProfitMap(Map<Long, Long> playerTotalProfitMap) {
        this.playerTotalProfitMap = playerTotalProfitMap;
    }

    public ToSouthBloodGameLog getGameLog() {
        return gameLog;
    }

    @Override
    public int getPoolId() {
        return ToSouthBloodDataHelper.getPoolId(this);
    }

    @Override
    public void resetData(BasePokerGameController<? extends BasePokerGameDataVo> controller) {
        super.resetData(controller);
        this.lastPlayCards = null;
        this.isFirstRound = true;
        this.passCount = 0;
        this.curRoundPassedPlayerSeats = new HashSet<>();
        this.currentRoundPlays.clear();
        this.bombSettlementMap.clear();
        this.playerHighlightCards.clear();
        this.readyPlayerIds = new HashSet<>();
        this.readyTimerScheduled = new HashSet<>();
        this.readyTimerVersion.clear();
        this.instantWinContext = null;
        this.gameLog = new ToSouthBloodGameLog();
        this.finishedPlayerOrder.clear();
        this.bloodWinSettlementMap.clear();
    }
}
