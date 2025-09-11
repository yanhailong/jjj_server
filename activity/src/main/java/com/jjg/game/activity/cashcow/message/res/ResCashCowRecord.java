package com.jjg.game.activity.cashcow.message.res;

import com.jjg.game.activity.cashcow.message.bean.CashCowShowRecord;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/10 09:39
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_CASH_COW_RECORD,resp = true)
@ProtoDesc("摇钱树响应记录")
public class ResCashCowRecord extends AbstractResponse {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("类型")
    public int type;
    @ProtoDesc("记录")
    public List<CashCowShowRecord> recordList;

    public ResCashCowRecord(int code) {
        super(code);
    }
}
