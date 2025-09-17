package com.jjg.game.activity.sharepromote.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.sharepromote.message.bean.SharePromoteSelfRankInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/16 16:26
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_SHARE_PROMOTE_SELF_RANK_INFO,resp = true)
@ProtoDesc("推广分享我的收益排行榜信息")
public class ResSharePromoteSelfRankInfo extends AbstractResponse {
    @ProtoDesc("排行信息")
    public List<SharePromoteSelfRankInfo> rankInfoList;
    @ProtoDesc("起始索引")
    public int startIndex;
    @ProtoDesc("是否还有数据")
    public boolean hasNext;

    public ResSharePromoteSelfRankInfo(int code) {
        super(code);
    }
}
