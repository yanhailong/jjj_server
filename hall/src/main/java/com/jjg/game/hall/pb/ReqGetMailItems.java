package com.jjg.game.hall.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/14 10:11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_GET_MAIL_ITEMS)
@ProtoDesc("获取邮件内道具")
public class ReqGetMailItems extends AbstractMessage {
    public int id;
}
