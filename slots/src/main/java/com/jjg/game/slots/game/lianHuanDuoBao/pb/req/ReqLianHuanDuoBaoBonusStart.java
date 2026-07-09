package com.jjg.game.slots.game.lianHuanDuoBao.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.lianHuanDuoBao.constant.LianHuanDuoBaoConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.LIAN_HUAN_DUO_BAO_TYPE, cmd = LianHuanDuoBaoConstant.MsgBean.REQ_BONUS_START)
@ProtoDesc("请求开始 bonus 小游戏（玩家点开始或 15s 倒计时结束）")
public class ReqLianHuanDuoBaoBonusStart extends AbstractMessage {
}
