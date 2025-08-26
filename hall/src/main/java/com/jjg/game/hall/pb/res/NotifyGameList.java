package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.pb.struct.GameListConfig;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/21 13:47
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.NOTIFY_GAME_LIST,resp = true)
@ProtoDesc("推送游戏列表")
public class NotifyGameList extends AbstractNotice {
    @ProtoDesc("游戏列表")
    public List<GameListConfig> gameList;
}
