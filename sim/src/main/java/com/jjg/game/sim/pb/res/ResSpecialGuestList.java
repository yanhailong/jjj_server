package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.SpecialGuestInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SPECIAL_GUEST_LIST,resp = true)
@ProtoDesc("获取特殊游客列表返回")
public class ResSpecialGuestList extends AbstractResponse {
    public List<SpecialGuestInfo> specialGuestList;
    @ProtoDesc("今日已手动刷新次数")
    public int refreshCount;
    @ProtoDesc("下一次手动刷新消耗")
    public ItemInfo nextRefreshCost;

    public ResSpecialGuestList(int code) {
        super(code);
    }
}
