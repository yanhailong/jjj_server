package com.jjg.game.core.utils;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;

import javax.smartcardio.Card;
import java.util.*;
import java.util.stream.Collectors;

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

    public static final List<EPokerSuit> SUIT_LIST = Arrays.stream(EPokerSuit.values()).toList();

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
    public static List<Integer> getPokerIntIdExceptJoker() {
        Set<Byte> pokers = getPokerIdExceptJoker();
        return pokers.stream().map(Byte::intValue).collect(Collectors.toList());
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

    /**
     * 获取特殊的两张牌
     *
     * @param type 1第一张牌点数大 2第二张牌点数大 3点数一样大
     * @return
     */
    public static Pair<Integer, Integer> getTwoSpecificCard(int type) {
        int point = RandomUtils.randomNum(2, POKER_POINT_K + 1);
        List<EPokerSuit> ePokerSuits = RandomUtils.randomEleList(SUIT_LIST, 2);
        if (type == 3) {
            return Pair.newPair(getCardId(ePokerSuits.get(0), point), (getCardId(ePokerSuits.get(1), point)));
        }
        int nextPoint = RandomUtils.randomNum(1, point);
        return type == 1 ? Pair.newPair(getCardId(ePokerSuits.get(0), point), getCardId(ePokerSuits.get(1), nextPoint))
                : Pair.newPair(getCardId(ePokerSuits.get(0), nextPoint), getCardId(ePokerSuits.get(1), point));
    }

    public static int getCardId(EPokerSuit suit, int point) {
        return (suit.suitId - 1) * POKER_POINT_K + point;
    }

    /**
     * 通过牌ID获取可读的字符串列表
     */
    public static String toHumanString(Byte cardId) {
        int pointId = getPointId(cardId);
        EPokerHumanStr ePokerHumanStr = EPokerHumanStr.getPokerHumanStrById(pointId);
        return ePokerHumanStr == null ? "" : ePokerHumanStr.getHumanStr();
    }

    /**
     * 通过牌ID获取可读的字符串列表
     */
    public static String toHumanString(Collection<Byte> cardIds) {
        return cardIds.stream().map(PokerCardUtils::toHumanString).collect(Collectors.joining(","));
    }

    /**
     * 通过牌ID获取可读的字符串列表带花色
     */
    public static String toHumanStringWithSuit(Collection<Byte> cardIds) {
        return cardIds.stream().map(cardId -> {
            int pointId = getPointId(cardId);
            EPokerHumanStr ePokerHumanStr = EPokerHumanStr.getPokerHumanStrById(pointId);
            EPokerSuit ePokerSuit = getSuit(cardId);
            return (ePokerHumanStr == null ? "" : ePokerHumanStr.getHumanStr()) +
                    (ePokerSuit == null ? "" : ePokerSuit.getSuitName());
        }).collect(Collectors.joining(","));
    }

    public enum EPokerHumanStr {
        TWO(2, "2"),
        THREE(3, "3"),
        FOUR(4, "4"),
        FIVE(5, "5"),
        SIX(6, "6"),
        SEVEN(7, "7"),
        EIGHT(8, "8"),
        NINE(9, "9"),
        TEN(10, "10"),
        J(11, "J"),
        Q(12, "Q"),
        K(13, "K"),
        A(1, "A"),
        LITTLE_JOKER(14, "小王"),
        BIG_JOKER(15, "大王");
        final int pointId;
        final String humanStr;

        EPokerHumanStr(int pointId, String humanStr) {
            this.pointId = pointId;
            this.humanStr = humanStr;
        }

        public int getPointId() {
            return pointId;
        }

        public String getHumanStr() {
            return humanStr;
        }

        public static EPokerHumanStr getPokerHumanStrById(int pointId) {
            for (EPokerHumanStr value : values()) {
                if (value.getPointId() == pointId) {
                    return value;
                }
            }
            return null;
        }

        public static EPokerHumanStr getPokerHumanStrByHumanStr(String humanStr) {
            for (EPokerHumanStr value : values()) {
                if (value.getHumanStr().equals(humanStr)) {
                    return value;
                }
            }
            return null;
        }
    }

    public static EPokerSuit getSuitByConfig(String configSuit) {
        switch (configSuit) {
            case "Diamond" -> {
                return EPokerSuit.DIAMOND;
            }
            case "Club" -> {
                return EPokerSuit.CLUBS;
            }
            case "Heart" -> {
                return EPokerSuit.HEART;
            }
            case "Spade" -> {
                return EPokerSuit.SPADES;
            }
        }
        return null;
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
