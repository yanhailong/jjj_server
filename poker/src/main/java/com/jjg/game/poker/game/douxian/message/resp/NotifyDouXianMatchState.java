package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE,
        cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_MATCH_STATE, resp = true)
@ProtoDesc("斗仙牌通知匹配状态")
public class NotifyDouXianMatchState extends AbstractNotice {
    @ProtoDesc("匹配状态(0未匹配/已取消 1匹配中 2匹配成功 3匹配超时)")
    public int state;
    @ProtoDesc("匹配倒计时结束时间戳(毫秒)，非匹配中为0")
    public long endTime;
    @ProtoDesc("当前已入座人数")
    public int currentPlayerNum;
    @ProtoDesc("匹配目标人数")
    public int maxPlayerNum;
}
