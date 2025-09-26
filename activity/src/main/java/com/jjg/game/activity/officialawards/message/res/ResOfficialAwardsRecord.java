package com.jjg.game.activity.officialawards.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.officialawards.message.bean.OfficialAwardsShowRecord;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/10 09:39
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_OFFICIAL_AWARDS_RECORD, resp = true)
@ProtoDesc("官方派奖响应记录")
public class ResOfficialAwardsRecord extends AbstractResponse {
    @ProtoDesc("记录")
    public List<OfficialAwardsShowRecord> recordList;
    @ProtoDesc("起始索引")
    public int startIndex;
    @ProtoDesc("是否还有数据")
    public boolean hasNext;
    public ResOfficialAwardsRecord(int code) {
        super(code);
    }
}
