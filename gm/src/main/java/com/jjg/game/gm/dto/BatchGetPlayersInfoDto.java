package com.jjg.game.gm.dto;

import java.util.List;

public record BatchGetPlayersInfoDto(
        List<Long> playerIds
) {
}
