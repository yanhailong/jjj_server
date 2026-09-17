package com.jjg.game.sim.data;

import java.math.BigDecimal;

/** 具体RPC返回类型，避免CommonResult泛型在跨节点反序列化时丢失。 */
public class ActivePassPurchase {
    public int code;
    public String productId;
    public BigDecimal price;
    public ActivePassPurchase() { }
    public ActivePassPurchase(int code) { this.code = code; }
    public ActivePassPurchase(int code, String productId, BigDecimal price) {
        this.code = code; this.productId = productId; this.price = price;
    }
}
