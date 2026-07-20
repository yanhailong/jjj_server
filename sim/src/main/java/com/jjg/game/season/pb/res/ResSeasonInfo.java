package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;
import com.jjg.game.season.pb.struct.SeasonInfo;
import com.jjg.game.season.pb.struct.SeasonSettlementInfo;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_INFO, resp = true)
@ProtoDesc("当前赛季信息返回")
public class ResSeasonInfo extends AbstractResponse {
    @ProtoDesc("当前赛季信息")
    public SeasonInfo info;
    @ProtoDesc("上赛季结算信息；仅跨赛季后首次请求时下发，无则为空")
    public SeasonSettlementInfo lastSettlement;

    public ResSeasonInfo(int code) {
        super(code);
    }
}
