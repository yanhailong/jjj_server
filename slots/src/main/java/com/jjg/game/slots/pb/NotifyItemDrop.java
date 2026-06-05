package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON, cmd = SlotsConst.SlotsCommon.NOTIFY_ITEM_DROP, resp = true)
@ProtoDesc("通知道具掉落")
public class NotifyItemDrop extends AbstractResponse {
    @ProtoDesc("道具信息")
    public List<ItemInfo> items;

    public NotifyItemDrop(int code) {
        super(code);
    }
}
