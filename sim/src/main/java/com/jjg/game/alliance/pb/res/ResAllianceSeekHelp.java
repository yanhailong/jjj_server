package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceHelpOrderInfo;

/**
 * 发起求助返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_SEEK_HELP, resp = true)
@ProtoDesc("发起求助返回")
public class ResAllianceSeekHelp extends AbstractResponse {
    @ProtoDesc("新建求助订单")
    public AllianceHelpOrderInfo order;
    @ProtoDesc("今日剩余求助次数")
    public int remainSeek;

    public ResAllianceSeekHelp(int code) {
        super(code);
    }
}
