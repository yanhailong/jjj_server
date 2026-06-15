package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 被助力通知(发给求助者)。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.NOTIFY_HELPED, resp = true)
@ProtoDesc("被助力通知(发给求助者)")
public class NotifyHelped extends AbstractResponse {
    @ProtoDesc("订单id")
    public long orderId;
    @ProtoDesc("1任务 2建筑加速")
    public int type;
    @ProtoDesc("帮助者id")
    public long helperId;
    @ProtoDesc("帮助者昵称")
    public String helperNick;
    @ProtoDesc("任务=进度+1后的值; 加速=本次减少秒数")
    public long value;

    public NotifyHelped(int code) {
        super(code);
    }
}
