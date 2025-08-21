package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/21 11:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_CANCEL_LIKE_GAME,resp = true)
@ProtoDesc("取消收藏返回")
public class ResCancelLikeGame extends AbstractResponse {
    public ResCancelLikeGame(int code) {
        super(code);
    }
}
