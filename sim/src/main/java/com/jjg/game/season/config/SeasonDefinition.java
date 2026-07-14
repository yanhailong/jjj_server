package com.jjg.game.season.config;

/**
 * 赛季时间线所需的最小配置视图。
 *
 * @param id           赛季配置 ID
 * @param durationDays 持续自然日数
 * @param loopSequence 循环顺序，0 表示前置赛季
 */
public record SeasonDefinition(int id, int durationDays, int loopSequence) {
}
