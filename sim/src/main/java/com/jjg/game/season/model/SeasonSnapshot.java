package com.jjg.game.season.model;

/**
 * 指定时刻的玩家赛季快照。
 *
 * @param seasonId  当前赛季配置 ID
 * @param phase     赛季阶段
 * @param cycleIndex 循环赛季序号；前置赛季为 0
 * @param startTime 开始自然日 0 点（毫秒）
 * @param endTime   持续天数后的自然日 0 点（毫秒，左闭右开）
 * @param day       当前赛季内第几个自然日，从 1 开始
 * @param seasonKey 竞技隔离键
 */
public record SeasonSnapshot(int seasonId, SeasonPhase phase, int cycleIndex,
                             long startTime, long endTime, int day, String seasonKey) {
}
