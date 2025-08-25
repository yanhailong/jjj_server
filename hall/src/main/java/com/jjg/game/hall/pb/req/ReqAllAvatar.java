package com.jjg.game.hall.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/13 11:12
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_ALL_AVATAR)
@ProtoDesc("请求获取所有头像信息(头像，头像框，称号，国旗)")
public class ReqAllAvatar extends AbstractMessage {
}
