package com.jjg.game.gm.dto;

/**
 * @author lm
 * @date 2025/7/10 11:04
 */
public record GameStatusDto(
        int number,
        int open,
        int status,
        //角标
        String right_top_icon,
        //大小图标 0.大  1.小
        int icon_category,
        //排序  值越小，越在左边显示
        int sort) {
}
