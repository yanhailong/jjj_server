package com.jjg.game.gm.dto.config;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * 覆盖配置信息
 */
public record GetConfigDto(
        //excel表名
        String name
) {
}
