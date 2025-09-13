package com.jjg.game.activity.piggybank.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:28
 */
@ProtobufMessage
@ProtoDesc("储钱罐类型活动信息")
public class PiggyBankActivityInfo {
    @ProtoDesc("活动详细信息")
    public List<PiggyBankDetailInfo> detailInfos;
}
