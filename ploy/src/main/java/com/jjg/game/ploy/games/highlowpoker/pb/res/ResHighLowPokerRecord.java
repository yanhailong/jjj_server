package com.jjg.game.ploy.games.highlowpoker.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowPokerConstant;
import com.jjg.game.ploy.games.highlowpoker.pb.bean.HighLowRecordInfo;

import java.util.List;

/**
 * @author lm
 * @date 2026/4/1 09:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HIGH_LOW_POKER, cmd = HighLowPokerConstant.MsgBean.RES_HIGH_LOW_POKER_RECORD, resp = true)
@ProtoDesc("下注返回")
public class ResHighLowPokerRecord extends AbstractResponse {
    @ProtoDesc("历史记录信息")
    public List<HighLowRecordInfo> historyInfoList;

    public ResHighLowPokerRecord(int code) {
        super(code);
    }
}
