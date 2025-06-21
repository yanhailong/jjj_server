package com.jjg.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/18 17:51
 */
@ProtobufMessage(messageType = DollarExpressConst.MsgBean.TYPE, cmd = DollarExpressConst.MsgBean.NOTICE_CONFIG_INFO,resp = true)
@ProtoDesc("推送配置信息")
public class NoticeConfigInfo extends AbstractMessage {
    public List<Integer> stakeList;
}
