package com.jjg.game.gm.dto;

/**
 * @author lm
 * @date 2025/7/10 11:04
 */
public record GameStatusDto(
        int number,
        int open,
        int status,
        //角标
        String right_top_icon) {
}
