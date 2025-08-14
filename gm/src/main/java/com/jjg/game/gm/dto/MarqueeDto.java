package com.jjg.game.gm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 跑马灯
 * @author 11
 * @date 2025/8/6 13:32
 */
public record MarqueeDto(
        @Positive(message = "id不能为负")
        int id,
        @NotBlank(message = "跑马灯内容不能为空")
        String content,
        @Positive(message = "间隔时间")
        int interval_time,
        @Positive(message = "次数不能为负")
        int nums,
        @Positive(message = "权重不能为负")
        int priority,
        @NotBlank(message = "开始时间不能为空")
        String start_time,
        @NotBlank(message = "结束时间不能为空")
        String end_time,
        int type
) {
}
