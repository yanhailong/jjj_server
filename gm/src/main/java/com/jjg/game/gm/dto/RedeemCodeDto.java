package com.jjg.game.gm.dto;

import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2026/3/12 09:36
 */
public record RedeemCodeDto(
        /*
          礼包id
         */
        long id,
        /*
          开始时间戳(毫秒)
         */
        long startTime,
        /*
          结束时间戳(毫秒)
         */
        long endTime,
        /*
          奖励(json数据)
         */
        Map<Integer, Long> rewardsItem,
        /*
          兑换码列表
         */
        List<String> codeList
) {
}
