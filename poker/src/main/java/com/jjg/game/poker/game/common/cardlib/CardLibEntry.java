package com.jjg.game.poker.game.common.cardlib;

/**
 * 牌库条目通用接口
 * 所有要接入牌库系统的 Poker 游戏数据模型都必须实现此接口
 * <p>
 * 用于在 {@link AbstractCardLibDao#findSectionKey} 中按倍数定位 Redis 分区
 */
public interface CardLibEntry {

    /**
     * 有符号倍数（正=赢, 负=输），用于定位 Redis 分区
     */
    long getMultiplier();

    /**
     * 可选的二级分区。同一个收益区间还需要按场景拆库时覆写，例如斗仙牌按真人数量拆分。
     * 旧游戏默认返回空串，Redis key 与改造前完全一致。
     */
    default String getPartitionKey() {
        return "";
    }
}
