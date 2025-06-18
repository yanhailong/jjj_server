package com.jjg.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;

/**
 * @author 11
 * @date 2025/6/13 13:45
 */
@ProtoDesc("每个场次配置信息")
public class WareHouseConfigInfo {
    @ProtoDesc("场次id")
    public int sid;
    @ProtoDesc("场次名称")
    public String name;
    @ProtoDesc("奖池")
    public long pool;
    @ProtoDesc("进入vip最低等级")
    public int limitVipMin;
    @ProtoDesc("进入最低金币数量")
    public long limitGoldMin;
}
