package com.jjg.game.hall.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;

import java.util.List;

/**
 * @author 11
 * @date 2025/5/26 15:22
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CERTIFY_MESSAGE_TYPE, cmd = MessageConst.CertifyMessage.RES_LOGIN,resp = true)
@ProtoDesc("登录返回")
public class ResLogin extends AbstractResponse {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("昵称")
    public String nickName;
    @ProtoDesc("金币")
    public long gold;
    @ProtoDesc("钻石")
    public long diamond;
    @ProtoDesc("vip等级")
    public int vipLevel;
    @ProtoDesc("游戏列表")
    public List<GameListConfig> gameList;

    public ResLogin(int code) {
        super(code);
    }
}
