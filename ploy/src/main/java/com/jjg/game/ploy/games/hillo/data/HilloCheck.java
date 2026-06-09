package com.jjg.game.ploy.games.hillo.data;

import com.jjg.game.core.data.Card;

@FunctionalInterface
public interface HilloCheck {
    // 封装不同投注区域的命中判断，避免在控制器里堆 chooseId 分支。
    boolean check(Card currentCard, Card nextCard);
}
