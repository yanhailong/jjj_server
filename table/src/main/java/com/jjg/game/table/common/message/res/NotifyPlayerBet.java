package com.jjg.game.table.common.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.common.message.TableRoomMessageConstant;
import com.jjg.game.table.common.message.bean.BetTableInfo;

import java.util.List;

/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    resp = true,
    cmd = TableRoomMessageConstant.RespMsgBean.RESP_BET
)
@ProtoDesc("请求下注返回")
public class NotifyPlayerBet extends AbstractResponse {

    @ProtoDesc("玩家ID")
    public long playerId;

    @ProtoDesc("玩家此次下注时的筹码皮肤id")
    public long chipId;

    @ProtoDesc("下注玩家当前金币")
    public long playerCurGold;

    @ProtoDesc("玩家押注信息列表")
    public List<BetTableInfo> betTableInfoList;

    public NotifyPlayerBet(int code) {
        super(code);
    }
}
