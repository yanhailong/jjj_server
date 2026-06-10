package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.pb.struct.FriendInfo;

import java.util.List;

/**
 * 处理好友申请返回。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_HANDLE_REQUEST, resp = true)
@ProtoDesc("处理好友申请返回")
public class ResHandleRequest extends AbstractResponse {
    @ProtoDesc("成功处理的申请人id")
    public List<Long> handledIds;
    @ProtoDesc("同意后新增的好友(用于刷新列表)")
    public List<FriendInfo> addedFriends;

    public ResHandleRequest(int code) {
        super(code);
    }
}
