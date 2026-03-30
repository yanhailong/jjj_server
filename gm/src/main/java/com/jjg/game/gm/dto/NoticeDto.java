package com.jjg.game.gm.dto;

/**
 * @author 11
 * @date 2025/11/10 10:35
 */
public record NoticeDto(
        long id,
        // 公告名称
        String name,
        // 标题
        String title,
        // 内容
        String content,
        //类型   1.普通公告   2.场景公告  3.外部地址
        int type,
        // 排序
        int sort,
        // 是否开启（1开 2闭）
        int open,
        // 角标资源
        String corner_mark,
        // 背景图
        String backdrop,
        // 按钮图
        String button,
        // 开始时间
        int start_time,
        // 结束时间
        int end_time,
        // 跳转类型
        int scene,
        // 跳转资源
        String jump_url,
        //大类型  0.公告  1.活动
        int big_type
) {
}
