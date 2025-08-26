package com.jjg.game.gm.dto;

/**
 * @author 11
 * @date 2025/8/18 11:12
 */
public record QueryAccountDto(
        long playerId,
        String registerMac,
        String loginMac,
        String nickName,
        String phone
) {
}