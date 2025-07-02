package com.jjg.game.table.common.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.table.common.message.TableRoomMessageConstant;

import java.util.List;

/**
 * 百人牌桌返回玩家列表信息
 *
 * @author 2CL
 */

@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BACCARAT_TYPE,
    resp = true,
    cmd = TableRoomMessageConstant.RespMsgBean.RESP_TABLE_PLAYER_INFO
)
@ProtoDesc("返回百人牌桌玩家列表的下注信息")
public class RespTablePlayerInfo extends AbstractResponse {

    @ProtoDesc("百人牌桌的玩家信息")
    public List<TablePlayerInfo> tablePlayerInfo;

    public RespTablePlayerInfo(int code) {
        super(code);
    }
}
