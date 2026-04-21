package com.jjg.game.poker.game.tosouthblood.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthblood.constant.ToSouthBloodConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthBloodConstant.MsgBean.RESP_SEND_CARDS_INFO, resp = true)
@ProtoDesc("响应南方前进房间基本信息")
public class RespToSouthBloodSendCardsInfo extends AbstractNotice {
    @ProtoDesc("玩家原始手牌")
    public List<Integer> originalHandCards;
    @ProtoDesc("排好序的手牌")
    public List<Integer> sortedHandCards;
    @ProtoDesc("高亮手牌列表 (包含2、炸弹、连对)")
    public List<Integer> highlightCards;

    public RespToSouthBloodSendCardsInfo() {

    }
}
