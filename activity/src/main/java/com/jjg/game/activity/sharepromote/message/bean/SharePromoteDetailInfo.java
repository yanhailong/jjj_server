package com.jjg.game.activity.sharepromote.message.bean;

import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/3 18:00
 */
@ProtobufMessage
@ProtoDesc("推广分享")
public class SharePromoteDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("需要达到的人数")
    public int needNum;
}
