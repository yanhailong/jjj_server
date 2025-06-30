package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.slots.game.dollarexpress.constant.DollarExpressConst;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/30 15:21
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE, cmd = DollarExpressConst.MsgBean.RES_CONFIG_INFO, resp = true)
@ProtoDesc("请求配置信息")
public class ResConfigInfo extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Integer> stakeList;
    @ProtoDesc("默认押注")
    public int defaultBet;

    public ResConfigInfo(int code) {
        super(code);
    }
}
