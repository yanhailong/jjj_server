package com.jjg.game.activity.sharepromote.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/16 16:26
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_SHARE_PROMOTE_SELF_RANK_INFO)
@ProtoDesc("请求推广分享我的收益排行榜信息")
public class ReqSharePromoteSelfRankInfo {
    @ProtoDesc("起始索引")
    public int startIndex;
    @ProtoDesc("每页数目")
    public int size;
}
