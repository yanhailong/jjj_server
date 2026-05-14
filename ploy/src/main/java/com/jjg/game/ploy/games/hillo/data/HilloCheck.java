package com.jjg.game.ploy.games.hillo.data;

import com.jjg.game.core.data.Card;

@FunctionalInterface
public interface HilloCheck {
    boolean check(Card currentCard, Card nextCard);
}
