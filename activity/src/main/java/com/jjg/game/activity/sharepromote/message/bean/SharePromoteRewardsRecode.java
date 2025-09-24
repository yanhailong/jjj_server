package com.jjg.game.activity.sharepromote.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/16 15:41
 */
@ProtobufMessage
@ProtoDesc("推广分享领取记录")
public class SharePromoteRewardsRecode {
    @ProtoDesc("数量")
    public int getNum;
    @ProtoDesc("领取时间")
    public long getTime;
}
