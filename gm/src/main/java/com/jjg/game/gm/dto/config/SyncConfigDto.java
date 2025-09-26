package com.jjg.game.gm.dto.config;

import java.util.List;

public record SyncConfigDto(
        //excel配置表名字
        String name,
        //同步的配置信息
        List<String> configs
) {
}
