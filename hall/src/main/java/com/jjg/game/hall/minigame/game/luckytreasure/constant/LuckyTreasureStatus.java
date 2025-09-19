package com.jjg.game.hall.minigame.game.luckytreasure.constant;


/**
 * 夺宝商品状态 1=可购买,2=等待开奖,3=待领取,4=已领取,5=领奖结束(中奖未领取),6=未中奖
 */
public enum LuckyTreasureStatus {
    INIT("未开启", 0),
    BUY("可购买", 1),
    WAIT("等待开奖", 2),
    WAIT_RECEIVE("待领取", 3),
    ALREADY_RECEIVE("已领取", 4),
    RECEIVE_FINISH("领奖结束", 5),
    NOT_WON("未中奖", 6),
    ;

    private final String str;

    private final int code;

    LuckyTreasureStatus(String str, int code) {
        this.str = str;
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public String getStr() {
        return str;
    }
}
