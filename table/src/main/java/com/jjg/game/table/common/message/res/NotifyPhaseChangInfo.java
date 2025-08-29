package com.jjg.game.table.common.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.table.common.message.TableRoomMessageConstant;

/**
 * @author lm
 * @date 2025/7/24 15:24
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
        resp = true,
        cmd = TableRoomMessageConstant.RespMsgBean.NOTIFY_PHASE_CHANG_INFO
)
@ProtoDesc("通知阶段变化信息")
public class NotifyPhaseChangInfo extends AbstractNotice {
    @ProtoDesc("当前阶段")
    public EGamePhase gamePhase;
    @ProtoDesc("结束时间戳")
    public long endTime;
}
