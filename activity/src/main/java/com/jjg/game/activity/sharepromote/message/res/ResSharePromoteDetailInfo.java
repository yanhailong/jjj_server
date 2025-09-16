package com.jjg.game.activity.sharepromote.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.sharepromote.message.bean.SharePromoteDetailInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/3 17:47
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_SHARE_PROMOTE_DETAIL_INFO, resp = true)
@ProtoDesc("响应每日奖金活动详细信息")
public class ResSharePromoteDetailInfo extends AbstractResponse {
    @ProtoDesc("活动详细信息")
    public List<SharePromoteDetailInfo> detailInfo;

    public ResSharePromoteDetailInfo(int code) {
        super(code);
    }
}
