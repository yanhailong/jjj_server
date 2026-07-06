package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * 旋转进度增量广播 (高频轻量: 单成员血条 + 共享进度)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.COOP_ROOM, cmd = SlotsConst.SlotsCommon.NOTIFY_COOP_SPIN, resp = true)
@ProtoDesc("协作房间旋转进度广播")
public class NotifyCoopSpin extends AbstractResponse {
    @ProtoDesc("本次旋转的玩家id")
    public long playerId;
    @ProtoDesc("该玩家剩余血量")
    public int hpLeft;
    @ProtoDesc("共享特殊事件累计")
    public int sharedProgress;
    @ProtoDesc("共享特殊事件目标")
    public int sharedTarget;

    public NotifyCoopSpin(int code) {
        super(code);
    }
}
