package com.jjg.game.gm.dto;

/**
 * 轮播数据
 */
public record CarouselDto(
        //类型 1轮播 2活动
        int activityImageType,
        //资源名字
        String sourceName,
        //跳转类型 1 游戏 2 网址 3 无 4 活动
        int jumpType,
        //跳转值
        String jumpValue,
        //排序值
        int sort,
        //动图 1是 2不是
        int showType,
        //id唯一值
        long id
) {
}
