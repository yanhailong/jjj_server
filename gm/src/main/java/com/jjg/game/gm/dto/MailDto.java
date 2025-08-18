package com.jjg.game.gm.dto;

/**
 * @author 11
 * @date 2025/8/6 13:47
 */
public record MailDto(
        String designated,
        String title,
        String content,
        //道具列表
        String items
) {
}
