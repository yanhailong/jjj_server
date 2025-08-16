package com.jjg.game.gm.dto;

/**
 * 跑马灯
 * @author 11
 * @date 2025/8/6 13:32
 */
public record MarqueeDto(
        int id,
        String content,
        int showTime,
        int interval_time,
        int priority,
        String start_time,
        String end_time,
        int type
) {
}
