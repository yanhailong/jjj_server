package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 捐献返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_DONATE, resp = true)
@ProtoDesc("捐献返回")
public class ResAllianceDonate extends AbstractResponse {
    @ProtoDesc("获得贡献值")
    public long rewardContribution;
    @ProtoDesc("给联盟的声誉值")
    public long rewardReputation;
    @ProtoDesc("捐献后我的贡献值")
    public long myContribution;
    @ProtoDesc("捐献后联盟声誉值")
    public long allianceReputation;
    @ProtoDesc("今日剩余捐献次数")
    public int remainCount;

    public ResAllianceDonate(int code) {
        super(code);
    }
}
