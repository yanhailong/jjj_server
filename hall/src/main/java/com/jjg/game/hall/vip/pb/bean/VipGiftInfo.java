package com.jjg.game.hall.vip.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/8/28 09:36
 */
@ProtobufMessage
@ProtoDesc("vip礼包信息")
public class VipGiftInfo {
    @ProtoDesc("类型 1周工资 2生日彩金 3晋级彩金 4年终奖")
    public int type;
    @ProtoDesc("是否可以领取")
    public boolean camClaim;
    @ProtoDesc("下次领取时间")
    public long nextTime;
    @ProtoDesc("领取还需充值")
    public long needRecharge;
}
