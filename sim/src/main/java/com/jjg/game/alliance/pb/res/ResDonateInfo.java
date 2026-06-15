package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceDonateInfo;
import com.jjg.game.alliance.pb.struct.AllianceBrief;

import java.util.List;

/**
 * 捐献界面信息返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_DONATE_INFO, resp = true)
@ProtoDesc("捐献界面信息返回")
public class ResDonateInfo extends AbstractResponse {
    @ProtoDesc("捐献档位")
    public List<AllianceDonateInfo> donates;
    @ProtoDesc("今日剩余捐献次数")
    public int remainCount;
    @ProtoDesc("每日捐献上限")
    public int dailyLimit;
    @ProtoDesc("联盟概要(经验进度条用)")
    public AllianceBrief alliance;

    public ResDonateInfo(int code) {
        super(code);
    }
}
