package com.jjg.game.table.common.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.table.common.message.TableRoomMessageConstant;
import com.jjg.game.table.common.message.bean.TablePlayerInfo;

import java.util.List;

/**
 * 返回牌桌玩家列表的下注信息
 *
 * @author 2CL
 */

@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    cmd = TableRoomMessageConstant.RespMsgBean.RESP_TABLE_PLAYER_INFO,
    resp = true
)
@ProtoDesc("返回牌桌玩家列表的下注信息")
public class RespTablePlayerInfo extends AbstractResponse {

    @ProtoDesc("牌桌的玩家信息")
    public List<TablePlayerInfo> tablePlayerInfo;

    public RespTablePlayerInfo(int code) {
        super(code);
    }
}
