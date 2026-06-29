package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.pb.struct.AllianceDonateInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 捐献界面信息返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_DONATE_INFO, resp = true)
@ProtoDesc("捐献界面信息返回")
public class ResAllianceDonateInfo extends AbstractResponse {
    @ProtoDesc("捐献配置")
    public AllianceDonateInfo donate;
    @ProtoDesc("今日已捐献次数")
    public int todayDonatedCount;

    public ResAllianceDonateInfo(int code) {
        super(code);
    }
}
