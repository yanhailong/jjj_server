package com.jjg.game.hall.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.constant.HallMessageConst;

/**
 * @author 11
 * @date 2025/6/10 16:58
 */
@ProtobufMessage(messageType = HallMessageConst.MsgBean.TYPE, cmd = HallMessageConst.MsgBean.REQ_ENTER_GAME)
@ProtoDesc("请求进入游戏")
public class ReqChooseGame extends AbstractMessage {
    @ProtoDesc("游戏类型")
    public int gameType;
}
