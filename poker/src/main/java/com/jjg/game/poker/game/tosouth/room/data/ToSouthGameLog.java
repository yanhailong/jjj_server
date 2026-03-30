package com.jjg.game.poker.game.tosouth.room.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 南方前进一局日志累积器
 * <p>
 * 在游戏过程中收集各阶段事件，结算时统一输出两种日志：
 * <ul>
 *   <li>流程日志 - 完整记录一局从发牌到结算的每一步操作</li>
 *   <li>结算日志 - 汇总所有结算（通杀结算、炸弹结算、最终结算）</li>
 * </ul>
 */
public class ToSouthGameLog {

    /** 流程事件列表（按时间顺序累积） */
    private final List<String> flowEvents = new ArrayList<>();
    /** 结算事件列表（仅包含结算相关记录） */
    private final List<String> settlementEvents = new ArrayList<>();

    // ======================== 发牌阶段 ========================

    /**
     * 记录发牌
     *
     * @param playerId 玩家ID
     * @param seatId   座位号
     * @param cardsStr 手牌字符串
     */
    public void recordDeal(long playerId, int seatId, String cardsStr) {
        flowEvents.add(String.format("[发牌] 玩家:%d 座位:%d 手牌:%s", playerId, seatId, cardsStr));
    }

    /**
     * 记录首出玩家
     *
     * @param playerId         首出玩家ID
     * @param isWinnerContinue 是否上局赢家续出
     */
    public void recordFirstPlayer(long playerId, boolean isWinnerContinue) {
        flowEvents.add(String.format("[首出] 玩家:%d (%s)", playerId,
                isWinnerContinue ? "上局赢家续出" : "黑桃3先出"));
    }

    /**
     * 记录通杀检测
     *
     * @param playerId 通杀玩家ID
     * @param winType  通杀类型 (InstantWinType: 1=4个2, 2=一条龙, 3=同色, 4=6对, 5=5连对, 6=6连对)
     * @param cardsStr 通杀牌面字符串
     */
    public void recordInstantWin(long playerId, int winType, String cardsStr) {
        String typeName = switch (winType) {
            case 1 -> "4个2";
            case 2 -> "一条龙";
            case 3 -> "同色";
            case 4 -> "6对";
            case 5 -> "5连对";
            case 6 -> "6连对";
            default -> "未知(" + winType + ")";
        };
        flowEvents.add(String.format("[通杀] 玩家:%d 类型:%s 牌:%s", playerId, typeName, cardsStr));
        settlementEvents.add(String.format("[通杀结算] 玩家:%d 类型:%s", playerId, typeName));
    }

    // ======================== 出牌阶段 ========================

    /**
     * 记录出牌
     *
     * @param playerId    玩家ID
     * @param seatId      座位号
     * @param cardType    牌型名称
     * @param cardsStr    出的牌字符串
     * @param remainCount 剩余手牌数
     */
    public void recordPlay(long playerId, int seatId, String cardType, String cardsStr, int remainCount) {
        flowEvents.add(String.format("[出牌] 玩家:%d 座位:%d 牌型:%s 牌:%s 剩余:%d",
                playerId, seatId, cardType, cardsStr, remainCount));
    }

    /**
     * 记录过牌
     *
     * @param playerId 玩家ID
     * @param seatId   座位号
     */
    public void recordPass(long playerId, int seatId) {
        flowEvents.add(String.format("[过牌] 玩家:%d 座位:%d", playerId, seatId));
    }

    /**
     * 记录新一轮开始（所有人过牌后，球权回到最后出牌者）
     *
     * @param leaderId 新一轮球权玩家ID
     */
    public void recordNewRound(long leaderId) {
        flowEvents.add(String.format("[新一轮] 球权玩家:%d", leaderId));
    }

    // ======================== 炸弹结算 ========================

    /**
     * 记录炸弹结算
     *
     * @param winnerId  赢家ID
     * @param loserId   输家ID
     * @param loseScore 输家扣分
     * @param winScore  赢家得分（税后）
     * @param chainSize 炸弹链长度
     */
    public void recordBombSettlement(long winnerId, long loserId, long loseScore, long winScore, int chainSize) {
        String event = String.format("[炸弹结算] 赢家:%d(+%d) 输家:%d(-%d) 连炸:%d",
                winnerId, winScore, loserId, loseScore, chainSize);
        flowEvents.add(event);
        settlementEvents.add(event);
    }

    // ======================== 最终结算 ========================

    /**
     * 记录游戏结束（有人出完牌或通杀触发）
     *
     * @param winnerId 赢家玩家ID
     */
    public void recordGameEnd(long winnerId) {
        flowEvents.add(String.format("[游戏结束] 赢家:%d", winnerId));
    }

    /**
     * 记录最终结算（每个玩家）
     *
     * @param playerId    玩家ID
     * @param winScore    净输赢（正=赢，负=输）
     * @param isWinner    是否赢家
     * @param remainCards 剩余手牌数
     * @param detail      结算明细（倍数计算等）
     */
    public void recordFinalSettlement(long playerId, long winScore, boolean isWinner, int remainCards, String detail) {
        String event = String.format("[最终结算] 玩家:%d %s %+d (剩余牌:%d%s)",
                playerId,
                isWinner ? "赢" : "输",
                winScore,
                remainCards,
                detail != null && !detail.isEmpty() ? ", " + detail : "");
        settlementEvents.add(event);
    }

    // ======================== 构建日志 ========================

    /**
     * 构建流程日志（包含发牌->出牌->过牌->新一轮->炸弹结算->游戏结束的完整流水）
     *
     * @param roomInfo 房间信息字符串（如 "房间:123"）
     * @return 格式化后的流程日志
     */
    public String buildFlowLog(String roomInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== 南方前进流程日志 (").append(roomInfo).append(") ==========");
        for (String event : flowEvents) {
            sb.append("\n  ").append(event);
        }
        sb.append("\n========================================================");
        return sb.toString();
    }

    /**
     * 构建结算日志（汇总通杀结算、炸弹结算、最终结算）
     *
     * @param roomInfo 房间信息字符串
     * @return 格式化后的结算日志
     */
    public String buildSettlementLog(String roomInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== 南方前进结算日志 (").append(roomInfo).append(") ==========");
        for (String event : settlementEvents) {
            sb.append("\n  ").append(event);
        }
        sb.append("\n========================================================");
        return sb.toString();
    }
}
