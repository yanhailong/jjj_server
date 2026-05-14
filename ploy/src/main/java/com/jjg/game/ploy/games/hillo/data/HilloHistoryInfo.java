package com.jjg.game.ploy.games.hillo.data;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("HILLO history item")
public class HilloHistoryInfo {
    @ProtoDesc("current card id")
    private int cardId;
    @ProtoDesc("choose id")
    private int chooseId;
    @ProtoDesc("odd")
    private String odd;
    @ProtoDesc("result card id")
    private int resultCardId;
    @ProtoDesc("skipped")
    private boolean skipped;

    public HilloHistoryInfo() {
    }

    public HilloHistoryInfo(int cardId, int chooseId, String odd) {
        this.cardId = cardId;
        this.chooseId = chooseId;
        this.odd = odd;
    }

    public static HilloHistoryInfo skipped(int cardId) {
        HilloHistoryInfo info = new HilloHistoryInfo();
        info.cardId = cardId;
        info.skipped = true;
        return info;
    }

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public int getChooseId() {
        return chooseId;
    }

    public void setChooseId(int chooseId) {
        this.chooseId = chooseId;
    }

    public String getOdd() {
        return odd;
    }

    public void setOdd(String odd) {
        this.odd = odd;
    }

    public int getResultCardId() {
        return resultCardId;
    }

    public void setResultCardId(int resultCardId) {
        this.resultCardId = resultCardId;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }
}
