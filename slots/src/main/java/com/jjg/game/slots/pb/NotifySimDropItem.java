package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

import java.util.List;

/**
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON, cmd = SlotsConst.SlotsCommon.NOTIFY_SIM_DROP_ITEM, resp = true)
@ProtoDesc("推送sim的道具掉落")
public class NotifySimDropItem extends AbstractNotice {
    @ProtoDesc("道具信息")
    public List<ItemInfo> itemMap;
    @ProtoDesc("能量")
    public int power;
    @ProtoDesc("研究点")
    public int researchPoint;
}
