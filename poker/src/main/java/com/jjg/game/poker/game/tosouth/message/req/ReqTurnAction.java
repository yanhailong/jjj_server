package com.jjg.game.poker.game.tosouth.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouth.constant.ToSouthConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthConstant.MsgBean.REQ_TURN_ACTION)
@ProtoDesc("轮到指定玩家回合，玩家的操作")
public class ReqTurnAction {
    @ProtoDesc("0 出牌  1 pass")
    public int actionType;
    @ProtoDesc("出牌列表")
    public List<Integer> cards;
}
