package com.jjg.game.hall.casino.pb.bean;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/8 17:13
 */
@ProtobufMessage
@ProtoDesc("奖励信息")
public class CasinoRewardsInfo {
    @ProtoDesc("机台id ")
    public long machineId;
    @ProtoDesc("奖励道具信息")
    public ItemInfo itemInfo;
}
