package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 帮助返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_HELP, resp = true)
@ProtoDesc("帮助返回")
public class ResHelp extends AbstractResponse {
    @ProtoDesc("订单id")
    public long orderId;
    @ProtoDesc("获得贡献值")
    public long rewardContribution;
    @ProtoDesc("今日剩余帮助次数")
    public int remainHelp;

    public ResHelp(int code) {
        super(code);
    }
}
