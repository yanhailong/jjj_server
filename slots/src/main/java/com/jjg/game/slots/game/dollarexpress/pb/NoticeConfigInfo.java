package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.slots.game.dollarexpress.constant.DollarExpressConst;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/18 17:51
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE, cmd = DollarExpressConst.MsgBean.NOTICE_CONFIG_INFO,resp = true)
@ProtoDesc("推送配置信息")
public class NoticeConfigInfo extends AbstractMessage {
    public List<Integer> stakeList;
    @ProtoDesc("默认押注")
    public int defaultBet;

}
