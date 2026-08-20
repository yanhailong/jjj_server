package com.jjg.game.season.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.REQ_SEASON_INFO)
@ProtoDesc("请求当前赛季信息")
public class ReqSeasonInfo extends AbstractMessage {
    @ProtoDesc("请求类型 0.单纯获取信息  1.若为该类型，则第一次请求时可能会参与结算")
    public int reqType;
}
