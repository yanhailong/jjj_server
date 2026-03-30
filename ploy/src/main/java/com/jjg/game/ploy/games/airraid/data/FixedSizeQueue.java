package com.jjg.game.ploy.games.airraid.data;

import java.util.LinkedList;

/**
 * 固定长度、自动淘汰队列
 *
 * @author 11
 * @date 2026/3/27
 */
public class FixedSizeQueue<T> extends LinkedList<T> {
    private final int maxSize;

    public FixedSizeQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(T t) {
        boolean added = super.add(t);
        while (size() > maxSize) {
            super.remove(); // 移除队首
        }
        return added;
    }
}
