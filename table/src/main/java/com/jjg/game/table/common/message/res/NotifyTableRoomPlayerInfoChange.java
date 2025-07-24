package com.jjg.game.table.common.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.table.common.message.TableRoomMessageConstant;
import com.jjg.game.table.common.message.bean.TablePlayerInfo;

import java.util.List;

/**
 * 通知押注类房间玩家信息变化
 *
 * @author 2CL
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
        cmd = TableRoomMessageConstant.RespMsgBean.NOTIFY_PLAYER_JOIN_ROOM,
        resp = true
)
@ProtoDesc("通知押注类房间玩家信息变化")
public class NotifyTableRoomPlayerInfoChange extends AbstractNotice {

    @ProtoDesc("产生变化的玩家ID")
    public long changedPlayerId;

    @ProtoDesc("总人数")
    public int totalPlayerNum;

    @ProtoDesc("变化后的场上玩家信息")
    public List<TablePlayerInfo> tableChangedPlayerInfos;
}
