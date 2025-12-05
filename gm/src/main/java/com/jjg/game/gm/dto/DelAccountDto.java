package com.jjg.game.gm.dto;

/**
 * @author 11
 * @date 2025/8/25 14:46
 */
public record DelAccountDto(
        int type,  //1.请求验证码    2.确认并删除
        long playerId,
        String phone,
        int smsCode
) {
}
