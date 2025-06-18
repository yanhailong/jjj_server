package com.jjg.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/13 14:36
 */
@ProtoDesc("每条中奖线信息")
public class ResultLineInfo {
    @ProtoDesc("中奖线id")
    public int id;
    @ProtoDesc("这条中奖线对应的坐标id")
    public List<Integer> indexList;
    @ProtoDesc("倍率")
    public int times;
    @ProtoDesc("中奖金币")
    public long winGold;
}
