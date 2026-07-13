package com.jjg.game.poker.game.douxian.constant;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 凡界/灵界/仙界三区域定义，见 DESIGN.md 2.2 区域体系
 */
public enum DouXianZone {
    //凡界（英雄阶）容量2，第1回合开放
    MORTAL(1, 2, 1),
    //灵界（超凡阶）容量3，第1回合开放
    SPIRIT(2, 3, 1),
    //仙界（史诗阶）容量5，第2回合开放，不再飞升
    IMMORTAL(3, 5, 2);

    private final int id;
    private final int capacity;
    private final int openRound;

    DouXianZone(int id, int capacity, int openRound) {
        this.id = id;
        this.capacity = capacity;
        this.openRound = openRound;
    }

    public int getId() {
        return id;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getOpenRound() {
        return openRound;
    }

    /**
     * 飞升目标区域，仙界飞升返回null（直接舍弃回公共牌库）
     */
    public DouXianZone nextTierZone() {
        return switch (this) {
            case MORTAL -> SPIRIT;
            case SPIRIT -> IMMORTAL;
            case IMMORTAL -> null;
        };
    }

    /**
     * 该区域在指定回合是否已开放
     */
    public boolean isOpenAt(int round) {
        return round >= openRound;
    }

    private static final Map<Integer, DouXianZone> ID_MAP =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(DouXianZone::getId, Function.identity()));

    public static DouXianZone fromId(int id) {
        return ID_MAP.get(id);
    }
}
