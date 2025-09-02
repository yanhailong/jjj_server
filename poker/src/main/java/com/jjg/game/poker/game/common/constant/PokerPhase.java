package com.jjg.game.poker.game.common.constant;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 扑克类游戏阶段枚举
 *
 * @author lm
 * @date 2025/7/26 09:36
 */
public enum PokerPhase {
    /**
     * 准备
     */
    PREPARE(1),
    /**
     * 洗牌
     */
    SHUFFLE(2),
    /**
     * 押分
     */
    BET(3),
    /**
     * 发牌
     */
    SEND_CARDS(4),
    /**
     * 确认
     */
    CONFIRM(5),
    /**
     * 保险
     */
    INSURANCE(6),
    /**
     * 打牌
     */
    PLAY_CARDS(7),
    /**
     * 开牌(比牌)
     */
    OPEN_CARDS(8),
    /**
     * 总结算
     */
    SETTLEMENT(9),
    /**
     * 关闭
     */
    CLOSE(10),
    /**
     * 定庄
     */
    FIND_DEALER(11),
    /**
     * 接牌
     */
    PICK_UP_CARDS(12),
    /**
     * 边池结算
     */
    SIDE_POOL(13),
    ;
    private final int value;

    PokerPhase(int value) {
        this.value = value;
    }

    private static final Map<Integer, PokerPhase> VALUE_MAP = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(PokerPhase::getValue, Function.identity()));

    public static PokerPhase fromValue(int value) {
        return VALUE_MAP.get(value);
    }

    public int getValue() {
        return value;
    }
}
