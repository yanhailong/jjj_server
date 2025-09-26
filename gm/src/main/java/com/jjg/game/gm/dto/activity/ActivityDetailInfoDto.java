package com.jjg.game.gm.dto.activity;

/**
 * @author lm
 * @date 2025/9/23 19:11
 */
public record ActivityDetailInfoDto(
        //配置信息 json数组
        String cfgInfos,
        //活动类型
        int activityType,
        //活动id
        int activityId) {

}
