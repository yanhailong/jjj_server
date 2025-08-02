package com.jjg.game.slots.game.mahjiongwin.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.slots.game.mahjiongwin.MahjiongWinConstant;

/**
 * @author 11
 * @date 2025/8/1 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MAHJIONG_WIN_TYPE, cmd = MahjiongWinConstant.MsgBean.RES_START_GAME,resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResStartGame extends AbstractResponse {

    public ResStartGame(int code) {
        super(code);
    }
}
