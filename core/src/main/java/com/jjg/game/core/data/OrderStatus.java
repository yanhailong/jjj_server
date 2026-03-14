package com.jjg.game.core.data;

/**
 * 订单状态
 *
 * @author 11
 * @date 2025/9/18 14:54
 */
public enum OrderStatus {
    ORDER(1),  //下订单
    CANCEL(2), //取消订单
    FAIL(3), //失败
    CALLBACK(4),//收到支付回调
    PROCESSING(5),//处理订单相关逻辑
    SUCCESS(6);//全部执行成功

    public final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    /**
     * 已经处理的订单
     */
    public boolean isProcessingOrder() {
        return this == PROCESSING || this == SUCCESS;
    }
}
