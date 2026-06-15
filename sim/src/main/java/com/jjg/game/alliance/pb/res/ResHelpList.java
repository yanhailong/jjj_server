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
public class ResHelpList extends AbstractResponse {
    @ProtoDesc("进行中的求助订单")
    public List<AllianceHelpOrderInfo> orders;
    @ProtoDesc("今日剩余求助次数")
    public int remainSeek;
    @ProtoDesc("每日求助上限")
    public int dailySeekLimit;
    @ProtoDesc("今日剩余帮助次数")
    public int remainHelp;
    @ProtoDesc("每日帮助上限")
    public int dailyHelpLimit;

    public ResHelpList(int code) {
        super(code);
    }
}
