package com.jjg.game.slots.game.lianHuanDuoBao.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 宝箱奖励信息（连环夺宝特有）
 *
 * @author lm
 * @date 2026/6/2
 */
@ProtobufMessage
@ProtoDesc("宝箱奖励信息")
public class LianHuanDuoBaoChestRewardPb {
    @ProtoDesc("宝箱在盘面上的格子索引")
    public int chestIndex;
    @ProtoDesc("奖金倍数（实际奖金 = 倍数 × 单押注金额）")
    public int rewardTimes;
    @ProtoDesc("本宝箱是否额外掉了龙珠")
    public boolean dropDragonBall;
}
