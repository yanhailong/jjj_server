package com.jjg.game.core.data;


/**
 * @param gameId         游戏id
 * @param open           开放状态 1开放 2关闭
 * @param status         游戏上下架状态 1上架 2下架
 * @param right_top_icon 角标
 * @author lm
 * @date 2025/7/10 17:07
 */
public record GameStatus(int gameId, int open, int status, String right_top_icon) {

}
