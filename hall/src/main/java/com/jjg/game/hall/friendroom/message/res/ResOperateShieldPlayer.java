package com.jjg.game.hall.friendroom.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

import java.util.List;

/**
 * 返回操作屏蔽玩家
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ResMsgCons.RES_OPERATE_SHIELD_PLAYER,
    resp = true
)
@ProtoDesc("返回操作屏蔽玩家消息")
public class ResOperateShieldPlayer extends AbstractResponse {

    @ProtoDesc("玩家ID")
    public List<Long> playerId;

    @ProtoDesc("操作码，1: 屏蔽 2: 取消屏蔽 3: 全部清除")
    public int operateCode;

    public ResOperateShieldPlayer(int code) {
        super(code);
    }
}
