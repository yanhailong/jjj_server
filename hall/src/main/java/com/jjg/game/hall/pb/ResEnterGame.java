package com.jjg.game.hall.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallMessageConst;

/**
 * @author 11
 * @date 2025/6/10 17:03
 */
@ProtobufMessage(messageType = HallMessageConst.MSGBEAN.TYPE, cmd = HallMessageConst.MSGBEAN.RES_ENTER_GAME,resp = true)
@ProtoDesc("进入游戏返回")
public class ResEnterGame extends AbstractResponse {

    public ResEnterGame(int code) {
        super(code);
    }
}
