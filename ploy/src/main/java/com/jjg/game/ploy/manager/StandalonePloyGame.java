package com.jjg.game.ploy.manager;

/**
 * 不使用下注房间控制器、但需要独立接入 Ploy 节点的游戏。
 */
public interface StandalonePloyGame {
    int gameType();
}
