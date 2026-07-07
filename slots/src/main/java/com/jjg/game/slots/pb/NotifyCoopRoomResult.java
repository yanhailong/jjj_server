package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * 协作任务结算广播 (成功/失败弹窗)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON, cmd = SlotsConst.SlotsCommon.NOTIFY_COOP_ROOM_RESULT, resp = true)
@ProtoDesc("协作任务结算广播")
public class NotifyCoopRoomResult extends AbstractResponse {
    @ProtoDesc("任务配置id")
    public int taskId;
    @ProtoDesc("是否完成")
    public boolean success;
    @ProtoDesc("共享特殊事件最终累计")
    public long sharedProgress;
    @ProtoDesc("共享特殊事件目标")
    public long sharedTarget;
    @ProtoDesc("房间自动解散时间 (倒计时用, ms)")
    public long dissolveTime;

    public NotifyCoopRoomResult(int code) {
        super(code);
    }
}
