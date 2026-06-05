package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.NOTIFY_ITEM_DROP, resp = true)
@ProtoDesc("通知道具掉落")
public class NotifyItemDrop extends AbstractNotice {
    @ProtoDesc("道具信息")
    public List<ItemInfo> items;
}
