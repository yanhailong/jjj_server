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
    @ProtoDesc("当前时段已手动刷新次数")
    public int refreshCount;
    @ProtoDesc("下一次手动刷新消耗")
    public ItemInfo nextRefreshCost;
    @ProtoDesc("付费游客下次定时刷新时间，毫秒时间戳，0表示不按时段刷新")
    public long nextRefreshTime;
    @ProtoDesc("广告游客下次定时刷新时间，毫秒时间戳，0表示不按时段刷新")
    public long adNextRefreshTime;
    @ProtoDesc("每个时间段手动刷新的次数上限")
    public int maxManualRefreshPerPeriod;

    public ResSpecialGuestList(int code) {
        super(code);
    }
}
