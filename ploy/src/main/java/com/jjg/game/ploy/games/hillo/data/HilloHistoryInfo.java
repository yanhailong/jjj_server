package com.jjg.game.ploy.games.hillo.data;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("HILLO 过程记录项")
public class HilloHistoryInfo {
    // 一条过程记录可能是猜牌，也可能是跳过；skipped=true 时 chooseId/odd/resultCardId 不参与展示。
    @ProtoDesc("当时作为公牌的牌 id")
    private int cardId;
    @ProtoDesc("当时选择的投注项 id")
    private int chooseId;
    @ProtoDesc("当时赔率")
    private String odd;
    @ProtoDesc("本次猜牌结果牌 id")
    private int resultCardId;
    @ProtoDesc("是否为跳过记录")
    private boolean skipped;

    public HilloHistoryInfo() {
    }

    public HilloHistoryInfo(int cardId, int chooseId, String odd) {
        this.cardId = cardId;
        this.chooseId = chooseId;
        this.odd = odd;
    }

    // 跳过只记录被跳过的公牌，用于前端还原本局过程。
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
