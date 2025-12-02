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
@ProtoDesc("财富轮盘商城道具信息")
public class WealthRouletteGoodInfo {
    @ProtoDesc("id")
    public int id;
    @ProtoDesc("排序")
    public int sort;
    @ProtoDesc("最大购买次数")
    public int maxBuyTimes;
    @ProtoDesc("奖励")
    public List<ItemInfo> itemInfos;
    @ProtoDesc("当前购买次数")
    public int currentBuyTimes;
    @ProtoDesc("所需积分")
    public int needPoint;
}
