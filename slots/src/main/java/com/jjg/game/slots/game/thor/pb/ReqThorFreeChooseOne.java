package com.jjg.game.slots.game.thor.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.thor.ThorConstant;

/**
 * @author 11
 * @date 2025/12/1 18:13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.THOR, cmd = ThorConstant.MsgBean.REQ_FREE_CHOOSE_ONE)
@ProtoDesc("请求免费模式二选一")
public class ReqThorFreeChooseOne extends AbstractMessage {
    @ProtoDesc("免费模式类型  0.火焰  1.冰冻")
    public int type;

    public ReqThorFreeChooseOne(int type) {
        this.type = type;
    }
}
