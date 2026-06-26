package com.jjg.game.social.data;

/**
 * @author 11
 * @date 2026/6/26
 */
public record SendGiftConfig (
    //道具id
    int itemId,
    //道具数量
    long count,
    //赠送次数上限(每人)
    int sendCountPerPersonLimit,
    //赠送人数上限
    int sendPersonLimit
){}
