package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.pb.struct.RequestInfo;

import java.util.List;

/**
 * 好友申请列表返回。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_REQUEST_LIST, resp = true)
@ProtoDesc("好友申请列表返回")
public class ResRequestList extends AbstractResponse {
    @ProtoDesc("申请列表")
    public List<RequestInfo> requests;

    public ResRequestList(int code) {
        super(code);
    }
}
