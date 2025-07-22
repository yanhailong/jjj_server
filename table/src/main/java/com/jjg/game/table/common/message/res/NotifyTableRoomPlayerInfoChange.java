package com.jjg.game.table.common.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.table.common.message.TableRoomMessageConstant;

import java.util.List;

/**
 * 通知百家乐玩家加入房间
 *
 * @author 2CL
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.BACCARAT_TYPE,
        cmd = TableRoomMessageConstant.RespMsgBean.NOTIFY_PLAYER_JOIN_ROOM
        , resp = true
)
@ProtoDesc("通知百家乐玩家加入房间")
public class NotifyTableRoomPlayerInfoChange extends AbstractNotice {

    @ProtoDesc("产生变化的玩家ID")
    public long changedPlayerId;

    @ProtoDesc("变化后的场上玩家信息")
    public List<TablePlayerInfo> tableChangedPlayerInfos;
}
