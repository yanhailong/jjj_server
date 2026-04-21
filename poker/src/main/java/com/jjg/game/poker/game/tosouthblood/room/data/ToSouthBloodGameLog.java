package com.jjg.game.poker.game.tosouthblood.room.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 南方前进-血战一局日志累积器
 * <p>
 * 以结构化 {@link GameEvent} 对象存储一局中所有事件，结算时统一输出：
 * <ul>
 *   <li>流程日志 - 全部事件的完整流水</li>
 *   <li>结算日志 - 仅包含结算类事件（炸弹结算、通杀结算、最终结算）</li>
 * </ul>
 */
public class ToSouthBloodGameLog {

    // ======================== 事件类型枚举 ========================

    /**
     * 事件类型
     */
    public enum EventType {
        /** 发牌 */
        DEAL("发牌"),
        /** 出牌 - 首出（首局黑桃3先出） */
        FIRST_PLAY("首出"),
        /** 出牌 - 跟牌 */
        FOLLOW_PLAY("跟牌"),
        /** 出牌 - 新一轮出牌（球权回来后首出） */
        NEW_ROUND_PLAY("新一轮出牌"),
        /** 过牌 */
        PASS("过牌"),
        /** 炸弹结算 */
        BOMB_SETTLEMENT("炸弹结算"),
        /** 通杀结算 */
        INSTANT_WIN_SETTLEMENT("通杀结算"),
        /** 一局最后结算 */
        FINAL_SETTLEMENT("最终结算");

        private final String desc;

        EventType(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }
    }

    // ======================== 事件数据对象 ========================

    /**
     * 一条游戏事件记录
     * <p>
     * 不同 {@link EventType} 使用不同的字段子集，未使用的字段保持默认值（0/null/false）。
     */
    public static class GameEvent {
        /** 事件类型 */
        public EventType type;

        // ---------- 通用字段 ----------
        /** 玩家ID（发牌/出牌/过牌/通杀结算/最终结算） */
        public long playerId;
        /** 座位号 */
        public int seatId;

        // ---------- 发牌 / 出牌 / 通杀 ----------
        /** 手牌或出牌的字符串表示 */
        public String cardsStr;

        // ---------- 出牌专用 ----------
        /** 牌型名称（SINGLE, PAIR, BOMB_QUAD 等） */
        public String cardType;
        /** 出牌后剩余手牌数 / 最终结算时的剩余手牌数 */
        public int remainCount;

        // ---------- 炸弹结算专用 ----------
        /** 赢家ID */
        public long winnerId;
        /** 输家ID */
        public long loserId;
        /** 输家扣分（税前） */
        public long loseScore;
        /** 炸弹链长度 */
        public int chainSize;

        // ---------- 结算通用 ----------
        /** 赢家得分(税后) / 最终结算净得分 */
        public long winScore;

        // ---------- 通杀结算专用 ----------
        /** 通杀类型 (1=4个2, 2=一条龙, 3=同色, 4=6对, 5=5连对, 6=6连对) */
        public int instantWinType;

        // ---------- 最终结算专用 ----------
        /** 是否赢家 */
        public boolean winner;
        /** 结算明细描述（倍数分解等） */
        public String detail;

        @Override
        public String toString() {
            return switch (type) {
                //[%s] 玩家:%d 座位:%d 手牌:%s"
                case DEAL -> String.format("[%s] Player:%d Seat:%d Hand:%s",
                        type.desc, playerId, seatId, cardsStr);
                //[%s] 玩家:%d 座位:%d 牌型:%s 牌:%s 剩余:%d
                case FIRST_PLAY, FOLLOW_PLAY, NEW_ROUND_PLAY -> String.format("[%s] Player:%d Seat:%d Type:%s Cards:%s Remaining:%d",
                        type.desc, playerId, seatId, cardType, cardsStr, remainCount);
                //[%s] 玩家:%d 座位:%d
                case PASS -> String.format("[%s] Player:%d Seat:%d",
                        type.desc, playerId, seatId);
                //[%s] 赢家:%d(+%d) 输家:%d(-%d) 连炸:%d
                case BOMB_SETTLEMENT -> String.format("[%s] Winner:%d(+%d) Loser:%d(-%d) Bomb Streak:%d",
                        type.desc, winnerId, winScore, loserId, loseScore, chainSize);
                //[%s] 玩家:%d 类型:%s 牌:%s
                case INSTANT_WIN_SETTLEMENT -> String.format("[%s] Player:%d Type:%s Cards:%s",
                        type.desc, playerId, getInstantWinTypeName(instantWinType), cardsStr);
                //[%s] 玩家:%d %s %+d (剩余牌:%d%s)
                case FINAL_SETTLEMENT -> String.format("[%s] Player:%d %s %+d (Remaining:%d%s)",
                        type.desc, playerId, winner ? "win" : "lose", winScore, remainCount,
                        detail != null && !detail.isEmpty() ? ", " + detail : "");
            };
        }

        private static String getInstantWinTypeName(int winType) {
//            return switch (winType) {
//                case 1 -> "4个2";
//                case 2 -> "一条龙";
//                case 3 -> "同色";
//                case 4 -> "6对";
//                case 5 -> "5连对";
//                case 6 -> "6连对";
//                default -> "未知(" + winType + ")";
//            };
            return switch (winType) {
                case 1 -> "4x 2";
                case 2 -> "Straight";
                case 3 -> "Flush";
                case 4 -> "6 Pairs";
                case 5 -> "5x Pairs";
                case 6 -> "6x Pairs";
                default -> "Unknown(" + winType + ")";
            };
        }
    }

    // ======================== 事件列表 ========================

    /** 全部事件（按时间顺序） */
    private final List<GameEvent> events = new ArrayList<>();

    /**
     * 获取全部事件列表
     */
    public List<GameEvent> getEvents() {
        return events;
    }

    // ======================== 发牌 ========================

    public void recordDeal(long playerId, int seatId, String cardsStr) {
        GameEvent e = new GameEvent();
        e.type = EventType.DEAL;
        e.playerId = playerId;
        e.seatId = seatId;
        e.cardsStr = cardsStr;
        events.add(e);
    }

    // ======================== 出牌（三种子类型） ========================

    /** 首出（首局黑桃3先出） */
    public void recordFirstPlay(long playerId, int seatId, String cardType, String cardsStr, int remainCount) {
        addPlayEvent(EventType.FIRST_PLAY, playerId, seatId, cardType, cardsStr, remainCount);
    }

    /** 跟牌 */
    public void recordFollowPlay(long playerId, int seatId, String cardType, String cardsStr, int remainCount) {
        addPlayEvent(EventType.FOLLOW_PLAY, playerId, seatId, cardType, cardsStr, remainCount);
    }

    /** 新一轮出牌（球权回来后首出） */
    public void recordNewRoundPlay(long playerId, int seatId, String cardType, String cardsStr, int remainCount) {
        addPlayEvent(EventType.NEW_ROUND_PLAY, playerId, seatId, cardType, cardsStr, remainCount);
    }

    private void addPlayEvent(EventType type, long playerId, int seatId, String cardType, String cardsStr, int remainCount) {
        GameEvent e = new GameEvent();
        e.type = type;
        e.playerId = playerId;
        e.seatId = seatId;
        e.cardType = cardType;
        e.cardsStr = cardsStr;
        e.remainCount = remainCount;
        events.add(e);
    }

    // ======================== 过牌 ========================

    public void recordPass(long playerId, int seatId) {
        GameEvent e = new GameEvent();
        e.type = EventType.PASS;
        e.playerId = playerId;
        e.seatId = seatId;
        events.add(e);
    }

    // ======================== 结算（三种子类型） ========================

    /** 炸弹结算 */
    public void recordBombSettlement(long winnerId, long loserId, long loseScore, long winScore, int chainSize) {
        GameEvent e = new GameEvent();
        e.type = EventType.BOMB_SETTLEMENT;
        e.winnerId = winnerId;
        e.loserId = loserId;
        e.loseScore = loseScore;
        e.winScore = winScore;
        e.chainSize = chainSize;
        events.add(e);
    }

    /** 通杀结算 */
    public void recordInstantWinSettlement(long playerId, int instantWinType, String cardsStr) {
        GameEvent e = new GameEvent();
        e.type = EventType.INSTANT_WIN_SETTLEMENT;
        e.playerId = playerId;
        e.instantWinType = instantWinType;
        e.cardsStr = cardsStr;
        events.add(e);
    }

    /** 一局最后结算 */
    public void recordFinalSettlement(long playerId, long winScore, boolean isWinner, int remainCards, String detail) {
        GameEvent e = new GameEvent();
        e.type = EventType.FINAL_SETTLEMENT;
        e.playerId = playerId;
        e.winScore = winScore;
        e.winner = isWinner;
        e.remainCount = remainCards;
        e.detail = detail;
        events.add(e);
    }

    // ======================== 构建日志字符串 ========================

    /**
     * 构建流程日志（全部事件）
     */
    public String buildFlowLog(String roomInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== 南方前进-血战流程日志 (").append(roomInfo).append(") ==========");
        for (GameEvent event : events) {
            sb.append("\n  ").append(event);
        }
        sb.append("\n========================================================");
        return sb.toString();
    }

    /**
     * 构建结算日志（仅结算类事件：炸弹结算 + 通杀结算 + 最终结算）
     */
    public String buildSettlementLog(String roomInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== 南方前进-血战结算日志 (").append(roomInfo).append(") ==========");
        for (GameEvent event : events) {
            if (event.type == EventType.BOMB_SETTLEMENT
                    || event.type == EventType.INSTANT_WIN_SETTLEMENT
                    || event.type == EventType.FINAL_SETTLEMENT) {
                sb.append("\n  ").append(event);
            }
        }
        sb.append("\n========================================================");
        return sb.toString();
    }
}
