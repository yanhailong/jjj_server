package com.jjg.game.table.russianlette.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;

/**
 * 进入俄罗斯转盘请求初始信息
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BACCARAT_TYPE,
    cmd = RussianLetteMessageConstant.ReqMsgBean.REQ_RUSSIAN_LETTE_INFO
)
@ProtoDesc("请求俄罗斯转盘桌面信息")
public class ReqRussianLetteInfo extends AbstractMessage {
}
