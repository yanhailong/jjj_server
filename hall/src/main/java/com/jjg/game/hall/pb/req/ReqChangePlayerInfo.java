package com.jjg.game.hall.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/6 16:12
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_CHANGE_PLAYER_INFO)
@ProtoDesc("请求修改玩家信息")
public class ReqChangePlayerInfo extends AbstractMessage {
    @ProtoDesc("昵称")
    public String nick;
    @ProtoDesc("性别")
    public int gender;
}
