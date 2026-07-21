package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

/**
 * 广播某玩家准备状态变化(主动准备/取消准备/机器人自动准备)，DESIGN.md 3.1 匹配阶段
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_PLAYER_READY, resp = true)
@ProtoDesc("斗仙牌通知玩家准备状态")
public class NotifyDouXianPlayerReady extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("1准备 2取消")
    public int status;
}
