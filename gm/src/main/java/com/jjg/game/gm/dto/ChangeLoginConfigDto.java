package com.jjg.game.gm.dto;

import java.util.List;

/**
 * @author 11
 * @date 2025/10/22 11:53
 */
public record ChangeLoginConfigDto(
        int channel,
        List<ChannelLoginOpenDto> list
) {
}
