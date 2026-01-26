package com.jjg.game.gm.dto;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/25 14:46
 */
public record GenerateLibDto(
        int gameType,
        int count,
        String nodeName,
        List<GenerateLibCfgDto> list
) {
}
