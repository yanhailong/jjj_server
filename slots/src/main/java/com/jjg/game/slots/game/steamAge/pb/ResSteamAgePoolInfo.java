package com.jjg.game.slots.game.steamAge.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.steamAge.SteamAgeConstant;

/**
 * @author lihaocao
 * @date 2025/12/2 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.STEAM_AGE, cmd = SteamAgeConstant.MsgBean.RES_POOL_INFO, resp = true)
@ProtoDesc("返回奖池信息")
public class ResSteamAgePoolInfo extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResSteamAgePoolInfo(int code) {
        super(code);
    }
}
