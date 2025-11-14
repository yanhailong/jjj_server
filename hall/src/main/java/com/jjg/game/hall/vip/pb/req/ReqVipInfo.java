package com.jjg.game.hall.vip.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author lm
 * @date 2025/8/28 09:31
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = HallConstant.MsgBean.REQ_VIP_INFO)
@ProtoDesc("请求vip信息")
public class ReqVipInfo extends AbstractMessage {
}
