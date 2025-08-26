package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/21 9:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_LIKE_GAME,resp = true)
@ProtoDesc("返回收藏游戏列表")
public class ResLikeGame extends AbstractResponse {
    @ProtoDesc("收藏游戏列表")
    public List<Integer> gameTypeList;

    public ResLikeGame(int code) {
        super(code);
    }
}
