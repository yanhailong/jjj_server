package com.jjg.game.hall.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/11 15:45
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_USE_ITEM)
@ProtoDesc("请求使用道具")
public class ReqUseItem extends AbstractMessage {
    @ProtoDesc("格子id")
    public int girdId;
    @ProtoDesc("道具id")
    public int itemId;
    @ProtoDesc("使用道具数量")
    public long useItemCount;
}
