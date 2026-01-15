package com.jjg.game.gm.dto;

/**
 * @author 11
 * @date 2025/8/25 14:46
 */
public record BanAccountDto(
        int type,  //1.正常  2.封
        String playerIds
) {
}
