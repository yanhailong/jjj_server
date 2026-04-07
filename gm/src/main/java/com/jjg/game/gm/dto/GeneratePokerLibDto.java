package com.jjg.game.gm.dto;

/**
 * 生成poker请求参数
 */
public record GeneratePokerLibDto(
        int count,
        String nodeName,
        int gameType
) {
}
