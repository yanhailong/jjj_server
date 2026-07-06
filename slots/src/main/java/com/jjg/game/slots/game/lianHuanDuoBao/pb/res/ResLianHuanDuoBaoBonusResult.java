package com.jjg.game.slots.game.lianHuanDuoBao.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.lianHuanDuoBao.constant.LianHuanDuoBaoConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.LIAN_HUAN_DUO_BAO_TYPE, cmd = LianHuanDuoBaoConstant.MsgBean.RES_BONUS_RESULT, resp = true)
@ProtoDesc("bonus 小游戏结算返回")
public class ResLianHuanDuoBaoBonusResult extends AbstractResponse {
    @ProtoDesc("每个龙珠掉落抽到的奖金（按掉落顺序，长度=龙珠数）")
    public List<Long> ballWins;
    @ProtoDesc("bonus 总赢金")
    public long totalWin;
    @ProtoDesc("结算后玩家金币")
    public long allGold;
    @ProtoDesc("bonus 结束后玩家关卡（重置为 1）")
    public int layerNumber;
    @ProtoDesc("bonus 结束后聚宝盆余额（清零为 0）")
    public long treasureBowlAmount;

    public ResLianHuanDuoBaoBonusResult(int code) {
        super(code);
    }
}
