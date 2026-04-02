package com.jjg.game.ploy.games.highlowpoker.data;

import com.jjg.game.core.data.Card;


/**
 * @author lm
 * @date 2026/4/1 14:34
 */
@FunctionalInterface
public interface HighLowCheck {
    boolean check(Card old, Card now);
}
