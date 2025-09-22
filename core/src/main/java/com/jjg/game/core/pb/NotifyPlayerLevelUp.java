package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/22 15:10
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE,
        cmd = MessageConst.CoreMessage.NOTIFY_PLAYER_LEVEL_UP, resp = true)
@ProtoDesc("通知玩家升级奖励")
public class NotifyPlayerLevelUp extends AbstractNotice {
    @ProtoDesc("当前等级")
    public int level;
    @ProtoDesc("获得道具")
    public List<ItemInfo> items;
}
