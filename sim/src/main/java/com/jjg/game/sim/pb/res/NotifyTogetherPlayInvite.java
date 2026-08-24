package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME,
        cmd = SimConstant.MsgBean.NOTIFY_TOGETHER_PLAY_INVITE, resp = true)
@ProtoDesc("好友同玩邀请通知")
public class NotifyTogetherPlayInvite extends AbstractResponse {
    @ProtoDesc("邀请人id")
    public long inviterId;
    @ProtoDesc("邀请人昵称")
    public String inviterNick;
    @ProtoDesc("邀请人头像id")
    public int inviterHeadImg;
    @ProtoDesc("邀请人头像框id")
    public int inviterHeadFrame;
    @ProtoDesc("游戏类型")
    public int gameType;
    @ProtoDesc("机台id，客户端以enterType=0进入")
    public int wareId;
    @ProtoDesc("邀请提示失效时间，毫秒时间戳")
    public long expireTime;

    public NotifyTogetherPlayInvite(int code) {
        super(code);
    }
}
