package com.jjg.game.activity.cashcow.message.bean;

import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/3 18:00
 */
@ProtobufMessage
@ProtoDesc("摇钱树")
public class CashCowDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("消耗")
    public List<ItemInfo> costItems;
    @ProtoDesc("类型 1初级场 2中级场 3高级场 4.进度奖励")
    public int type;
    @ProtoDesc("奖池")
    public long pool;
    @ProtoDesc("需要进度")
    public long needProgress;
    @ProtoDesc("剩余免费次数")
    public int remainFreeTimes;
}
