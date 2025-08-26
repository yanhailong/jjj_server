package com.jjg.game.table.common.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.table.common.message.TableRoomMessageConstant;
import com.jjg.game.table.common.message.bean.ReqBetBean;

import java.util.List;

/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    cmd = TableRoomMessageConstant.ReqMsgBean.REQ_BET
)
@ProtoDesc("请求下注")
public class ReqBet extends AbstractMessage {

    @ProtoDesc("请求押注和续押")
    public List<ReqBetBean> reqBetBeans;
}
