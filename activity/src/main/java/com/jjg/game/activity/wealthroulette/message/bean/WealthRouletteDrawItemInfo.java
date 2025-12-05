package com.jjg.game.activity.wealthroulette.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

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

    public WealthRouletteDrawItemInfo() {
    }

    public WealthRouletteDrawItemInfo(int configId, String icon) {
        this.configId = configId;
        this.icon = icon;
    }
}
