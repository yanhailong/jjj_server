package com.jjg.game.poker.game.douxian.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

/**
 * 等待阶段请求准备/取消准备，DESIGN.md 3.1 匹配阶段：仿南方前进，人齐后不再自动开局，
 * 需要全员(真人+机器人)都确认准备才会进 {@link com.jjg.game.poker.game.douxian.gamephase.DouXianDealPhase}。
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.REQ_DOU_XIAN_GO_READY)
@ProtoDesc("斗仙牌请求准备")
public class ReqDouXianGoReady extends AbstractMessage {
    @ProtoDesc("1准备 2取消")
    public int status;
}
