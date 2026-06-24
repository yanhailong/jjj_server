package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.pb.struct.AllianceFailApply;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 处理入盟申请返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_HANDLE_APPLICATION, resp = true)
@ProtoDesc("处理入盟申请返回")
public class ResHandleApplication extends AbstractResponse {
    @ProtoDesc("成功入盟的玩家id")
    public List<Long> agreedIds;
    @ProtoDesc("处理失败的玩家")
    public List<AllianceFailApply> failApplyList;

    public ResHandleApplication(int code) {
        super(code);
    }
}
