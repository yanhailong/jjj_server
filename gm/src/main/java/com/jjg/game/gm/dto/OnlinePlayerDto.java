package com.jjg.game.gm.dto;

/**
 * @author 11
 * @date 2025/8/27 17:26
 */
public record OnlinePlayerDto(
        int gameId,
        int registerChannel,   //注册渠道
        int pageSize, //页大小
        int page //页码,从1开始
) {
}
