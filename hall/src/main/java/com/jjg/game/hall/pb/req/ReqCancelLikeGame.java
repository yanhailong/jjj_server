package com.jjg.game.hall.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.hall.constant.HallConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/21 11:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_CANCEL_LIKE_GAME)
@ProtoDesc("请求取消收藏游戏列表")
public class ReqCancelLikeGame extends AbstractMessage {
    public List<Integer> gameTypes;
}
