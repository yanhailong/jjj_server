package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.pb.struct.BlacklistInfo;

import java.util.List;

/**
 * 黑名单列表返回。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_BLACKLIST_LIST, resp = true)
@ProtoDesc("黑名单列表返回")
public class ResBlacklistList extends AbstractResponse {
    @ProtoDesc("黑名单列表")
    public List<BlacklistInfo> list;

    public ResBlacklistList(int code) {
        super(code);
    }
}
