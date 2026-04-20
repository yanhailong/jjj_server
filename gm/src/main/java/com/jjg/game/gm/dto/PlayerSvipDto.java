package com.jjg.game.gm.dto;

import java.util.List;

/**
 * @author 11
 * @date 2026/4/17
 */
public record PlayerSvipDto(
        //玩家id
        List<Long> playerIds,
        //是否标记
        boolean mark
) {
}
