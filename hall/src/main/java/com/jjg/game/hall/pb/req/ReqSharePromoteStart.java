package com.jjg.game.hall.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author lhc
 * @date 2026/2/9 09:36
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_SHARE_PROMOTE_START)
@ProtoDesc("请求开始分享推广信息")
public class ReqSharePromoteStart extends AbstractMessage {
}
