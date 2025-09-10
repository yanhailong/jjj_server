package com.jjg.game.activity.cashcow.message.res;

import com.jjg.game.activity.cashcow.message.bean.CashCowDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/3 17:47
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_CASH_COW_DETAIL_INFO, resp = true)
@ProtoDesc("响应摇钱树活动详细信息")
public class ResCashCowDetailInfo extends AbstractResponse {
    @ProtoDesc("活动详细信息")
    public List<CashCowDetailInfo> detailInfo;

    public ResCashCowDetailInfo(int code) {
        super(code);
    }
}
