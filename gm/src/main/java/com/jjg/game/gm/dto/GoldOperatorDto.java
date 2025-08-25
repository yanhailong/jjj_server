package com.jjg.game.gm.dto;

/**
 * @author 11
 * @date 2025/8/25 11:04
 */
public record GoldOperatorDto(
        long playerId,
        int currency_id,  //货币类型
        int type, //操作类型  1.增加  2.减少
        int operator_type, // 1.账户  2.保险箱
        long quantity, // 增减数量
        String remark
){}
