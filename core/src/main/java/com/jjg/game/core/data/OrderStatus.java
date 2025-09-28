package com.jjg.game.core.data;

/**
 * 订单状态
 * @author 11
 * @date 2025/9/18 14:54
 */
public enum OrderStatus {
    ORDER(0),  //下订单
    CANCEL(1), //取消订单
    FAIL(2), //失败
    SUCCESS(3); //成功

    public int code;

    OrderStatus(int code){
        this.code = code;
    }
}
