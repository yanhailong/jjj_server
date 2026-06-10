package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

/**
 * 搜索玩家返回 (code=PLAYER_NOT_EXIST 表示玩家不存在)。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_SEARCH_PLAYER, resp = true)
@ProtoDesc("搜索玩家返回")
public class ResSearchPlayer extends AbstractResponse {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("昵称")
    public String nick;
    @ProtoDesc("头像id")
    public int headImg;
    @ProtoDesc("头像框id")
    public int headFrame;
    @ProtoDesc("等级")
    public int level;
    @ProtoDesc("与我的关系 0.陌生 1.好友 2.已申请")
    public int relation;

    public ResSearchPlayer(int code) {
        super(code);
    }
}
