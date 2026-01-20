package com.jjg.game.activity.wealthroulette.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.wealthroulette.message.bean.WealthRouletteDrawInfo;
import com.jjg.game.activity.wealthroulette.message.bean.WealthRouletteHistoryInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/12/1 10:15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_WEALTH_ROULETTE_HISTORY, resp = true)
@ProtoDesc("财富轮盘历史记录")
public class ResWealthRouletteHistory extends AbstractResponse {
    @ProtoDesc("历史记录")
    public List<WealthRouletteHistoryInfo> wealthRouletteHistoryInfos;
    @ProtoDesc("起始索引")
    public int startIndex;
    @ProtoDesc("是否还有数据")
    public boolean hasNext;
    @ProtoDesc("总条数")
    public long totalCount;

    public ResWealthRouletteHistory(int code) {
        super(code);
    }
}
