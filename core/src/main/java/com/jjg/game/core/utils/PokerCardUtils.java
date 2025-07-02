package com.jjg.game.core.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * 扑克牌工具类 方块 黑桃 红心 梅花
 *
 * @author 2CL
 */
public class PokerCardUtils {

    public static final int ONE_POKERS_NUM = 54;

    public static final byte POKER_POINT_J = 11;

    public static final byte POKER_POINT_K = 13;

    public static final byte LITTLE_JOKER = 53;

    public static final byte BIG_JOKER = 54;

    /**
     * 扑克牌点数
     */
    public static byte[] POKER_IDS = new byte[ONE_POKERS_NUM];

    static {
        for (byte i = 1; i <= POKER_IDS.length; i++) {
            POKER_IDS[i - 1] = i;
        }
    }

    /**
     * 获取点数
     *
     * @param cardId 牌ID
     * @return 点数
     */
    public static byte getPointId(byte cardId) {
        return (byte) (((cardId - 1) % 13) + 1);
    }

    /**
     * 通过点数获取花色
     *
     * @param cardId 点数
     * @return 花色枚举
     */
    public static EPokerSuit getSuit(byte cardId) {
        return EPokerSuit.getPokerSuitById((cardId - 1) / 13 + 1);
    }

    /**
     * 获取扑克牌ID
     */
    public static Set<Byte> getWholePokerIds() {
        Set<Byte> pokers = new HashSet<>();
        for (byte i = 1; i <= BIG_JOKER; i++) {
            pokers.add(i);
        }
        return pokers;
    }

    /**
     * 获取除了大小王之外的扑克牌id列表
     */
    public static Set<Byte> getPokerIdExceptJoker() {
        Set<Byte> pokers = getWholePokerIds();
        pokers.remove(LITTLE_JOKER);
        pokers.remove(BIG_JOKER);
        return pokers;
    }

    public enum EPokerSuit {
        DIAMOND(1, "方块"),
        CLUBS(2, "梅花"),
        HEART(3, "红心"),
        SPADES(4, "黑桃"),
        ;
        final int suitId;
        final String suitName;

        EPokerSuit(int suitId, String suitName) {
            this.suitId = suitId;
            this.suitName = suitName;
        }

        public int getSuitId() {
            return suitId;
        }

        public String getSuitName() {
            return suitName;
        }

        public static EPokerSuit getPokerSuitById(int suitId) {
            for (EPokerSuit value : values()) {
                if (value.getSuitId() == suitId) {
                    return value;
                }
            }
            return null;
        }
    }
}
