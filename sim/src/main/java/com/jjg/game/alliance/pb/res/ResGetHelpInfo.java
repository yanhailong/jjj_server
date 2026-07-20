package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.pb.struct.AllianceHelpOrderInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 获取求助订单详情返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_GET_HELP_INFO, resp = true)
@ProtoDesc("获取求助信息返回")
public class ResGetHelpInfo extends AbstractResponse {
    @ProtoDesc("求助信息")
    public AllianceHelpOrderInfo helpOrderInfo;

    public ResGetHelpInfo(int code) {
        super(code);
    }
}
