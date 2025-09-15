package com.jjg.game.activity.activitylog.data;

import java.util.Map;

/**
 * @author lm
 * @date 2025/9/15 14:13
 */
public record ScratchCardsResult(
        //刮出图标数
        int iconNum,
        //奖励
        Map<Integer, Long> rewards,
        //详情id
        int detailId
) {
}
