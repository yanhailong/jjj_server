package com.jjg.game.gm.dto;

import com.jjg.game.core.data.SmsConfigInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/1/20
 */
public record SaveSmsConfigDto(
        List<SmsConfigInfo> list
) {
}
