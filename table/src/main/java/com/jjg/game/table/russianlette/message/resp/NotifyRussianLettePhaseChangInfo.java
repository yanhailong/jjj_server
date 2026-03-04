package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.table.common.message.TableRoomMessageConstant;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;

/**
 * @author lm
 * @date 2025/7/24 15:24
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE,
        resp = true,
        cmd = RussianLetteMessageConstant.RespMsgBean.NOTIFY_RUSSIAN_LETTE_PHASE_CHANGE_INFO
)
@ProtoDesc("通知俄罗斯转爬满阶段变化信息")
public class NotifyRussianLettePhaseChangInfo extends AbstractNotice {
    @ProtoDesc("当前阶段")
    public EGamePhase gamePhase;
    @ProtoDesc("结束时间戳")
    public long endTime;
    @ProtoDesc("概率信息")
    public RussianLetteProb prob;
}
