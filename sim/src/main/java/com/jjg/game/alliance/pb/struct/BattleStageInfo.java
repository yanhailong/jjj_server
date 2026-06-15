package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 对决阶段奖励项 (个人)。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage
@ProtoDesc("对决阶段奖励")
public class BattleStageInfo {
    @ProtoDesc("阶段序号(0起)")
    public int stage;
    @ProtoDesc("达标所需个人积分")
    public long scoreThreshold;
    @ProtoDesc("奖励列表")
    public List<ItemInfo> rewards;
    @ProtoDesc("是否已领取")
    public boolean claimed;
}
