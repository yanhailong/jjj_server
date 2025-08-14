package com.jjg.game.room.datatrack.logdata;

import com.jjg.game.room.data.room.GamePlayer;

import java.util.Map;

/**
 * @author lm
 * @date 2025/8/13 17:01
 */
public record LogParam<T, F>(
        T param,
        Map<Long, GamePlayer> playerData,
        F result
) {
}
