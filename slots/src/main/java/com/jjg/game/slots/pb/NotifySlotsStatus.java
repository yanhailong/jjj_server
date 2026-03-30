package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author 11
 * @date 2026/1/23
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON, cmd = SlotsConst.SlotsCommon.RES_SLOTS_STATUS, resp = true)
@ProtoDesc("获取slots游戏状态")
public class NotifySlotsStatus extends AbstractNotice {
    @ProtoDesc("暂停类型：1.房主暂停 2. 准备金不足或者没有庄家 3. 时间不足 4. 房间解散")
    public int pauseType;
}
