package com.jjg.game.gm.dto.config;

import java.util.List;

/**
 * 覆盖配置信息
 */
public record ReplaceConfigDto(
        //更新的配置信息
        List<String> configs,
        //excel表名
        String name
) {
}
