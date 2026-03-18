package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2026/3/13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_LIKE_NEW_GAME, resp = true)
@ProtoDesc("新游期待榜点赞返回")
public class ResLikeNewGame extends AbstractResponse {

    public ResLikeNewGame(int code) {
        super(code);
    }
}