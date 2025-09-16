package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 通知道具掉落信息
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE,
    cmd = MessageConst.CoreMessage.NOTIFY_ITEM_DROP,
    resp = true
)
@ProtoDesc("通知道具掉落信息")
public class NotifyItemDropInfo extends AbstractNotice {

    @ProtoDesc("活动道具掉落信息列表")
    public List<ActivityItemDropInfo> itemDropInfos;
}
