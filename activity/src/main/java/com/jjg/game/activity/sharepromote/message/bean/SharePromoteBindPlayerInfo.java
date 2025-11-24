package com.jjg.game.activity.sharepromote.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/16 16:37
 */
@ProtobufMessage
@ProtoDesc("我的推广分享绑定未领取玩家信息")
public class SharePromoteBindPlayerInfo {
    @ProtoDesc("昵称")
    public String nickname;
    @ProtoDesc("等级")
    public int level;
    @ProtoDesc("头像id")
    public int headImgId;
}
