package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON,
        cmd = SlotsConst.SlotsCommon.REQ_TOGETHER_PLAY_PLAYER_LIST)
@ProtoDesc("分页获取好友同玩玩家列表")
public class ReqTogetherPlayPlayerList extends AbstractMessage {
    @ProtoDesc("列表类型 0当前游戏全部玩家 1我邀请且当前在该游戏的玩家")
    public int listType;
    @ProtoDesc("页码，从0开始")
    public int pageIndex;
    @ProtoDesc("每页数量，最大20")
    public int pageSize;
}
