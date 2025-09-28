package com.jjg.game.gm.dto.config;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

public record SyncConfigDto(
        //excel配置表名字
        String name,
        //同步的配置信息
        List<JSONObject> configs
) {
}
