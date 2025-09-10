package com.jjg.game.activity.cashcow.data;

/**
 * 摇钱树记录
 *
 * @author lm
 * @date 2025/9/9 17:19
 */
public record CashCowRecordData(
        //期数
        long round,
        //时间
        long recordTime,
        //昵称
        String name,
        //类型
        int type,
        //获奖数量
        long num
) {
}
