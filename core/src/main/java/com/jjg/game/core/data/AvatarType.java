package com.jjg.game.core.data;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author lm
 * @date 2025/8/27 13:46
 */
public enum AvatarType {
    AVATAR("unlockAvatarSet", 0, Player::setHeadImgId, Player::getHeadImgId),
    FRAME("unlockFrameSet", 1, Player::setHeadFrameId, Player::getHeadFrameId),
    NATIONAL("", 2, (player, integer) -> {}, Player::getNationalId),
    TITLE("unlockTitleSet", 3, Player::setTitleId, Player::getTitleId),
    CHIP("unlockChipsSet", 4, Player::setChipsId, Player::getChipsId),
    CARD_BACKGROUND("unlockCardBackgroundSet", 5, Player::setBackgroundId, Player::getCardBackgroundId),
    BACKGROUND("unlockBackgroundSet", 6, Player::setCardBackgroundId, Player::getBackgroundId),
    ;
    private final String field;
    private final int type;
    private final BiConsumer<Player, Integer> consumer;
    private final Function<Player, Integer> getter;
    AvatarType(String field, int type, BiConsumer<Player, Integer> consumer, Function<Player, Integer> getter) {
        this.field = field;
        this.type = type;
        this.consumer = consumer;
        this.getter = getter;
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

    public Function<Player, Integer> getGetter() {
        return getter;
    }
}