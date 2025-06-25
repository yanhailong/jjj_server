package com.jjg.game.sample;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/6/18 15:37
 */
@ProtobufMessage
@ProtoDesc("每个场次配置信息")
public class WareHouseConfigInfo {
    @ProtoDesc("场次id")
    public int wareId;
    @ProtoDesc("场次名称")
    public String name;
    @ProtoDesc("奖池")
    public long pool;
    @ProtoDesc("进入vip最低等级")
    public int limitVipMin;
    @ProtoDesc("进入最低金币数量")
    public long limitGoldMin;
}
