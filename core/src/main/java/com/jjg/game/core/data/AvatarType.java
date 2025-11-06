package com.jjg.game.core.data;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author lm
 * @date 2025/8/27 13:46
 */
public enum AvatarType {
    AVATAR("unlockAvatarSet", 0, Player::setHeadImgId, Player::getHeadImgId, PlayerSkin::setUnlockAvatarSet, PlayerSkin::getUnlockAvatarSet),
    FRAME("unlockFrameSet", 1, Player::setHeadFrameId, Player::getHeadFrameId, PlayerSkin::setUnlockFrameSet, PlayerSkin::getUnlockFrameSet),
    NATIONAL("", 2, Player::setNationalId, Player::getNationalId, null, null),
    TITLE("unlockTitleSet", 3, Player::setTitleId, Player::getTitleId, PlayerSkin::setUnlockTitleSet, PlayerSkin::getUnlockTitleSet),
    CHIP("unlockChipsSet", 4, Player::setChipsId, Player::getChipsId, PlayerSkin::setUnlockChipsSet, PlayerSkin::getUnlockChipsSet),
    CARD_BACKGROUND("unlockCardBackgroundSet", 5, Player::setCardBackgroundId, Player::getCardBackgroundId, PlayerSkin::setUnlockCardBackgroundSet, PlayerSkin::getUnlockCardBackgroundSet),
    BACKGROUND("unlockBackgroundSet", 6, Player::setBackgroundId, Player::getBackgroundId, PlayerSkin::setUnlockBackgroundSet, PlayerSkin::getUnlockBackgroundSet),
    ;
    private final String field;
    private final int type;
    private final BiConsumer<Player, Integer> consumer;
    private final Function<Player, Integer> getter;

    private final BiConsumer<PlayerSkin, Set<Integer>> skinConsumer;
    private final Function<PlayerSkin, Set<Integer>> skinGetter;

    AvatarType(String field, int type,
               BiConsumer<Player, Integer> consumer,
               Function<Player, Integer> getter,
               BiConsumer<PlayerSkin, Set<Integer>> skinConsumer,
               Function<PlayerSkin, Set<Integer>> skinGetter) {
        this.field = field;
        this.type = type;
        this.consumer = consumer;
        this.getter = getter;
        this.skinConsumer = skinConsumer;
        this.skinGetter = skinGetter;
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

    public BiConsumer<PlayerSkin, Set<Integer>> getSkinConsumer() {
        return skinConsumer;
    }

    public Function<PlayerSkin, Set<Integer>> getSkinGetter() {
        return skinGetter;
    }
}