package com.jjg.game.ploy.games.highlowpoker.data;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2026/4/1 15:07
 */
@ProtobufMessage
@ProtoDesc("历史信息")
public class HighLowHistoryInfo {
    @ProtoDesc("牌id")
    private int cardId;
    @ProtoDesc("赔率")
    private String odd;

    public HighLowHistoryInfo() {
    }

    public HighLowHistoryInfo(int cardId, String odd) {
        this.cardId = cardId;
        this.odd = odd;
    }

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public String getOdd() {
        return odd;
    }

    public void setOdd(String odd) {
        this.odd = odd;
    }
}
