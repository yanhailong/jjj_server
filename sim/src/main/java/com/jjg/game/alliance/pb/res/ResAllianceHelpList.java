package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceHelpOrderInfo;

import java.util.List;

/**
 * 求助订单列表返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_HELP_LIST, resp = true)
@ProtoDesc("求助订单列表返回")
public class ResAllianceHelpList extends AbstractResponse {
    @ProtoDesc("进行中的求助订单")
    public List<AllianceHelpOrderInfo> orders;
    @ProtoDesc("今日剩余任务求助次数")
    public int remainSeek;
    @ProtoDesc("每日任务求助上限")
    public int dailySeekLimit;
    @ProtoDesc("今日剩余帮助次数(建筑加速)")
    public int remainHelp;
    @ProtoDesc("每日帮助上限(建筑加速)")
    public int dailyHelpLimit;
    @ProtoDesc("今日剩余建筑分享(加速求助)次数")
    public int remainShare;
    @ProtoDesc("每日建筑分享上限")
    public int dailyShareLimit;

    public ResAllianceHelpList(int code) {
        super(code);
    }
}
