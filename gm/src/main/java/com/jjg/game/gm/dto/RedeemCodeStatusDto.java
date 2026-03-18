package com.jjg.game.gm.dto;

/**
 * @author lm
 * @date 2026/3/12 17:39
 */
public record RedeemCodeStatusDto(
        /*
          礼包id
         */
        long id,
        /*
          是否启用
         */
        Boolean use
) {
}
