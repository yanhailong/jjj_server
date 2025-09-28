package com.jjg.game.gm.dto.config;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * 覆盖配置信息
 */
public record ReplaceConfigDto(
        //更新的配置信息
        List<JSONObject> configs,
        //excel表名
        String name
) {
}
