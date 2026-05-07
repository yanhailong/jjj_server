package com.jjg.game.poker.game.tosouthfree.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthfree.constant.ToSouthFreeConstant;
import com.jjg.game.poker.game.tosouthfree.message.bean.ToSouthFreeBombDetail;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH_FREE, cmd = ToSouthFreeConstant.MsgBean.NOTIFY_BOMB_SETTLEMENT, resp = true)
@ProtoDesc("南方前进-免费炸弹结算通知")
public class NotifyToSouthFreeBombSettlement extends AbstractNotice {
    @ProtoDesc("炸弹赔付明细列表")
    public List<ToSouthFreeBombDetail> details;
}
