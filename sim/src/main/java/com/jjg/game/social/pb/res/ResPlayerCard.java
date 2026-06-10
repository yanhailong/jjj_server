package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.pb.struct.PlayerCardInfo;

/**
 * 玩家信息卡返回。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_PLAYER_CARD, resp = true)
@ProtoDesc("玩家信息卡返回")
public class ResPlayerCard extends AbstractResponse {
    @ProtoDesc("信息卡")
    public PlayerCardInfo card;

    public ResPlayerCard(int code) {
        super(code);
    }
}
