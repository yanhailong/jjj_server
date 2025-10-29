package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/10/29 11:24
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.RES_PLAYER_MONEY, resp = true)
@ProtoDesc("获取玩家的账户信息")
public class ResPlayerMoney extends AbstractResponse {
    @ProtoDesc("金币")
    public long gold;
    @ProtoDesc("钻石")
    public long diamond;
    @ProtoDesc("保险箱金币")
    public long safeBoxGold;
    @ProtoDesc("保险箱钻石")
    public long safeBoxDiamond;

    public ResPlayerMoney(int code) {
        super(code);
    }
}
