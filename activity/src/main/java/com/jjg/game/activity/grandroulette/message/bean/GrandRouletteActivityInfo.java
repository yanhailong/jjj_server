package com.jjg.game.activity.grandroulette.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:28
 */
@ProtobufMessage
@ProtoDesc("大转盘活动信息")
public class GrandRouletteActivityInfo {
    @ProtoDesc("奖励详细信息")
    public List<GrandRouletteRewardInfo> detailInfos;
    @ProtoDesc("玩家状态 1未领取 2可领取 3已经领取 4未参加")
    public int playerState;
    @ProtoDesc("剩余次数")
    public int remainTimes;
    @ProtoDesc("当前金币")
    public long currentGold;
    @ProtoDesc("目标金币")
    public long targetGold;
    @ProtoDesc("结束时间戳")
    public long endTime;
    @ProtoDesc("是否绑定手机号 1已经绑定")
    public int bindPhoneState;
    @ProtoDesc("绑定信息")
    public List<GrandRouletteSubordinate> bindSubordinates;
}
