package com.jjg.game.core.data;

/**
 * 订单状态
 * @author 11
 * @date 2025/9/18 14:54
 */
public enum OrderStatus {
    ORDER(1),  //下订单
    CANCEL(2), //取消订单
    FAIL(3), //失败
    SUCCESS(4); //成功

    public int code;

    OrderStatus(int code){
        this.code = code;
    }
}
