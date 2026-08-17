package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_OWNED_SPECIAL_GUEST_LIST, resp = true)
@ProtoDesc("获取当前场景已购买的特殊游客返回")
public class ResOwnedSpecialGuestList extends AbstractResponse {
    @ProtoDesc("当前场景持有的特殊游客及数量")
    public List<ItemInfo> specialGuests;

    public ResOwnedSpecialGuestList(int code) {
        super(code);
    }
}
