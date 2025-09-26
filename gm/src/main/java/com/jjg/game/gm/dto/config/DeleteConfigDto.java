package com.jjg.game.gm.dto.config;

import java.util.List;

/**
 * 删除配置
 */
public record DeleteConfigDto(
        //删除的id
        List<Integer> ids,
        //excel表名
        String name
) {
}
