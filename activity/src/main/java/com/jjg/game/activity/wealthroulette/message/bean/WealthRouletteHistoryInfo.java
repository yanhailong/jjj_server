package com.jjg.game.activity.wealthroulette.message.bean;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/12/1 10:28
 */
@ProtobufMessage
@ProtoDesc("财富轮盘奖励信息")
public class WealthRouletteHistoryInfo {
    @ProtoDesc("时间")
    public long date;
    @ProtoDesc("类型")
    public int type;
    @ProtoDesc("消耗")
    public int cost;
    @ProtoDesc("奖励")
    public List<ItemInfo> itemInfo;
}
