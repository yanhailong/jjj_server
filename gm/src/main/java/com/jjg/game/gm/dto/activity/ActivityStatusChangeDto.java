package com.jjg.game.gm.dto.activity;

/**
 * @author lm
 * @date 2025/9/22 10:46
 */
public record ActivityStatusChangeDto(
        //活动id
        long activityId,
        //活动状态 1未开始 2进行中 3已结束
        int status
) {}
