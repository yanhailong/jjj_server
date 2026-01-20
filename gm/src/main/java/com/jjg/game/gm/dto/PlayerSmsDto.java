package com.jjg.game.gm.dto;

/**
 * @author 11
 * @date 2026/1/20
 */
public record PlayerSmsDto(
        long playerId,
        int type,  //0.绑定手机号  2.短信登录  3.删除账号  4.验证用户
        String phone,
        int smsCode
) {
}
