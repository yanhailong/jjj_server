package com.jjg.game.activity.grandroulette.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.grandroulette.data.GrandRouletteRecord;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/12/1 10:15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_GRAND_ROULETTE_HISTORY, resp = true)
@ProtoDesc("大轮盘历史记录")
public class ResGrandRouletteHistory extends AbstractResponse {
    @ProtoDesc("历史记录")
    public List<GrandRouletteRecord> recordList;
    @ProtoDesc("起始索引")
    public int startIndex;
    @ProtoDesc("是否还有数据")
    public boolean hasNext;
    @ProtoDesc("类型 1个人 2全服")
    public int type;
    public ResGrandRouletteHistory(int code) {
        super(code);
    }
}
