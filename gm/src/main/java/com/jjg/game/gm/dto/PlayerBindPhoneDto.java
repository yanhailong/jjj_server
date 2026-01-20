package com.jjg.game.gm.dto;

/**
 * @author 11
 * @date 2026/1/19
 */
public record PlayerBindPhoneDto(
        long playerId,
        String phone,
        int type  //1.绑定  2.解绑
) {
}
