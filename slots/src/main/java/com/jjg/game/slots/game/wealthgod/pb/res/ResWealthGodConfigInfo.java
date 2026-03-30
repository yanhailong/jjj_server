package com.jjg.game.slots.game.wealthgod.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wealthgod.WealthGodConstant;

import java.util.List;

/**
 *
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WEALTH_GOD, cmd = WealthGodConstant.MsgBean.RES_CONFIG_INFO, resp = true)
@ProtoDesc("请求配置信息")
public class ResWealthGodConfigInfo extends AbstractResponse {

    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;

    public ResWealthGodConfigInfo(int code) {
        super(code);
    }
}
