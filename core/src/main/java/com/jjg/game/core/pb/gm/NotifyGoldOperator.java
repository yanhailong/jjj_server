package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/10/22 17:05
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTIFY_GOLD_OPERATE, resp = true, toPbFile = false)
@ProtoDesc("通知修改玩家金币")
public class NotifyGoldOperator extends AbstractNotice {
    public long playerId;
    //货币类型
    public int currency_id;
    //操作类型  1.增加  2.减少
    public int type;
    // 增减数量
    public long quantity;
    public String addType;
    public String remark;
}
