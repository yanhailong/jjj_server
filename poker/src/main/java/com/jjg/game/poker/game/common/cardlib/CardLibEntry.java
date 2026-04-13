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
}
