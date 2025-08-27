package com.jjg.game.core.data;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author lm
 * @date 2025/8/27 13:46
 */
public enum AvatarType {
    AVATAR("unlockAvatarSet", 0, Player::setHeadImgId),
    FRAME("unlockFrameSet", 1, Player::setHeadFrameId),
    NATIONAL("", 2, (player, integer) -> {
    }),
    TITLE("unlockTitleSet", 3, Player::setTitleId),
    CHIP("unlockChipsSet", 4, Player::setChipsId),
    CARD_BACKGROUND("unlockBackgroundSet", 5, Player::setBackgroundId),
    BACKGROUND("unlockCardBackgroundSet", 6, Player::setCardBackgroundId),
    ;
    private final String field;
    private final int type;
    private final BiConsumer<Player, Integer> consumer;

    AvatarType(String field, int type, BiConsumer<Player, Integer> consumer) {
        this.field = field;
        this.type = type;
        this.consumer = consumer;
    }

    public String getField() {
        return field;
    }

    public int getType() {
        return type;
    }

    public BiConsumer<Player, Integer> getConsumer() {
        return consumer;
    }
}