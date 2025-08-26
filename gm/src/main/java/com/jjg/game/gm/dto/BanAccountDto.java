package com.jjg.game.gm.dto;

/**
 * @author 11
 * @date 2025/8/25 14:46
 */
public record BanAccountDto(
        int type,  //1.封  2.解
        String playerIds
) {
}
