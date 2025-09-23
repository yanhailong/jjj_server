package com.jjg.game.activity.officialawards.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.officialawards.message.bean.OfficialAwardsDetailInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/3 17:47
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_OFFICIAL_AWARDS_TYPE_INFO, resp = true)
@ProtoDesc("官方派奖详细信息")
public class ResOfficialAwardsDetailInfo extends AbstractResponse {
    @ProtoDesc("活动详细信息")
    public List<OfficialAwardsDetailInfo> detailInfo;

    public ResOfficialAwardsDetailInfo(int code) {
        super(code);
    }
}
