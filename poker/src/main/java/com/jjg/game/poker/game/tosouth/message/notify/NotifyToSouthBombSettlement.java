package com.jjg.game.poker.game.tosouth.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouth.constant.ToSouthConstant;

import com.jjg.game.poker.game.tosouth.message.bean.ToSouthBombDetail;
import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthConstant.MsgBean.NOTIFY_BOMB_SETTLEMENT)
@ProtoDesc("南方前进炸弹结算通知")
public class NotifyToSouthBombSettlement extends AbstractNotice {
    @ProtoDesc("炸弹赔付明细列表")
    public List<ToSouthBombDetail> details;
}
