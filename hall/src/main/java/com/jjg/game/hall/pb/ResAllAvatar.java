package com.jjg.game.hall.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/13 11:13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_ALL_AVATAR,resp = true)
@ProtoDesc("头像等信息返回")
public class ResAllAvatar extends AbstractResponse {
    @ProtoDesc("头像")
    public List<Integer> avatars;
    @ProtoDesc("头像框")
    public List<Integer> frames;
    @ProtoDesc("称号")
    public List<Integer> titles;

    public ResAllAvatar(int code) {
        super(code);
    }
}
