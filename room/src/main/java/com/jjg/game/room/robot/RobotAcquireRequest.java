package com.jjg.game.room.robot;

/**
 * 获取机器人时的筛选条件。
 *
 * @param itemId           房间入场货币道具ID
 * @param enterLimit       入场最低携带数量
 * @param enterMax         入场最高携带数量，-1 表示不限制
 * @param playerLevelLimit 玩家等级下限
 */
public record RobotAcquireRequest(int itemId, long enterLimit, long enterMax, int playerLevelLimit) {
}
