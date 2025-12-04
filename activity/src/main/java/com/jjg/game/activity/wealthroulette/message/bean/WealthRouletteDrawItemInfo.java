package com.jjg.game.activity.wealthroulette.message.bean;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/12/2 11:14
 */
@ProtobufMessage
@ProtoDesc("财富轮盘 抽奖道具信息")
public class WealthRouletteDrawItemInfo {
    @ProtoDesc("配置id")
    public int configId;
    @ProtoDesc("道具icon")
    public String icon;
    @ProtoDesc("奖励")
    public List<ItemInfo> itemInfo;
    public WealthRouletteDrawItemInfo() {
    }

    public WealthRouletteDrawItemInfo(int configId, String icon, List<ItemInfo> itemInfo) {
        this.configId = configId;
        this.icon = icon;
        this.itemInfo = itemInfo;
    }
}
