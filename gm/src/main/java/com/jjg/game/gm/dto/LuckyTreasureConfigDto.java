package com.jjg.game.gm.dto;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.config.bean.LuckyTreasureConfig;

import java.util.List;

public record LuckyTreasureConfigDto(
        int id,
        int bestValue,
        List<Integer> consumption,
        String des,
        int itemId,
        int itemNum,
        int total,
        int collectTime,
        String name,
        int time,
        int type,
        boolean repeated
) {
    public LuckyTreasureConfig castToLuckyTreasureConfig() {
        String jsonString = JSON.toJSONString(this);
        return JSON.parseObject(jsonString, LuckyTreasureConfig.class);
    }
}
