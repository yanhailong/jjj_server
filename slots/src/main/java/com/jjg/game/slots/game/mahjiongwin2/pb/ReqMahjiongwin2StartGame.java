package com.jjg.game.slots.game.mahjiongwin2.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.mahjiongwin2.MahjiongWin2Constant;

/**
 * @author 11
 * @date 2025/8/1 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MAHJIONG_WIN2_TYPE, cmd = MahjiongWin2Constant.MsgBean.REQ_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqMahjiongwin2StartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
