package com.jjg.game.poker.game.tosouthblood.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthblood.constant.ToSouthBloodConstant;
import com.jjg.game.poker.game.tosouthblood.message.bean.ToSouthBloodBombDetail;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthBloodConstant.MsgBean.NOTIFY_BOMB_SETTLEMENT, resp = true)
@ProtoDesc("南方前进炸弹结算通知")
public class NotifyToSouthBloodBombSettlement extends AbstractNotice {
    @ProtoDesc("炸弹赔付明细列表")
    public List<ToSouthBloodBombDetail> details;
}
