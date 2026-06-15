package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 一键帮助返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_ONE_KEY_HELP, resp = true)
@ProtoDesc("一键帮助返回")
public class ResOneKeyHelp extends AbstractResponse {
    @ProtoDesc("成功帮助人数")
    public int helpedCount;
    @ProtoDesc("跳过人数(已达上限)")
    public int skippedCount;
    @ProtoDesc("获得贡献值")
    public long rewardContribution;
    @ProtoDesc("今日剩余帮助次数")
    public int remainHelp;

    public ResOneKeyHelp(int code) {
        super(code);
    }
}
