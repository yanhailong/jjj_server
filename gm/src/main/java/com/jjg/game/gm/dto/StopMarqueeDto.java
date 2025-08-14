package com.jjg.game.gm.dto;

import jakarta.validation.constraints.Positive;

/**
 * 跑马灯
 * @author 11
 * @date 2025/8/6 13:32
 */
public record StopMarqueeDto(
        @Positive(message = "id不能为负")
        int id
) {
}
