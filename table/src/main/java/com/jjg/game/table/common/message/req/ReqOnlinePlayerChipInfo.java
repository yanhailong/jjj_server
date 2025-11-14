package com.jjg.game.table.common.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.common.message.TableRoomMessageConstant;

/**
 * @author lm
 * @date 2025/9/8 18:22
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
        cmd = TableRoomMessageConstant.ReqMsgBean.REQ_ONLINE_PLAYER_CHIP_INFO)
@ProtoDesc("请求在线玩家筹码皮肤id")
public class ReqOnlinePlayerChipInfo extends AbstractMessage {
}
