package com.jjg.game.hall.casino.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.hall.casino.pb.bean.CasinoSimpleInfo;
import com.jjg.game.hall.constant.HallConstant;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/25 10:31
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.NOTIFY_CASINO_SIMPLE_CHANGE, resp = true)
@ProtoDesc("通知我的赌场简单信息变化")
public class NotifyCasinoSimpleChange extends AbstractNotice {
    @ProtoDesc("信息")
    public List<CasinoSimpleInfo> infos;
}
