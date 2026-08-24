package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON,
        cmd = SlotsConst.SlotsCommon.RES_TOGETHER_PLAY_PLAYER_LIST, resp = true)
@ProtoDesc("分页获取好友同玩玩家列表返回")
public class ResTogetherPlayPlayerList extends AbstractResponse {
    @ProtoDesc("列表类型 0当前游戏全部玩家 1我邀请且当前在该游戏的玩家")
    public int listType;
    @ProtoDesc("本次请求页码")
    public int pageIndex;
    @ProtoDesc("每页数量")
    public int pageSize;
    @ProtoDesc("下一页页码，-1表示没有下一页")
    public int nextPageIndex = -1;
    @ProtoDesc("玩家列表")
    public List<TogetherPlayPlayerInfo> playerInfos;

    public ResTogetherPlayPlayerList(int code) {
        super(code);
    }
}
