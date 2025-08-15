package com.jjg.game.hall.pb.friendroom.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.constant.HallConstant;

import java.util.List;

/**
 * 请求在好友房中屏蔽玩家
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = HallConstant.MsgBean.REQ_OPERATE_SHIELD_PLAYER
)
@ProtoDesc("请求操作屏蔽玩家")
public class ReqOperateShieldPlayer  extends AbstractMessage {

    @ProtoDesc("玩家ID")
    public List<Long> playerId;

    @ProtoDesc("操作码，1: 屏蔽 2: 取消屏蔽 3: 全部清除")
    public int operateCode;
}
