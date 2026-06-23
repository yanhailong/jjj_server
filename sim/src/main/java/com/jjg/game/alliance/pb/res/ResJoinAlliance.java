package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 加入联盟返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_JOIN_ALLIANCE, resp = true)
@ProtoDesc("加入联盟返回")
public class ResJoinAlliance extends AbstractResponse {
    @ProtoDesc("如果直接加入则返回加入成功的id")
    public long joinAllianceId;
    @ProtoDesc("如果没有直接加入，则返回申请列表的id")
    public List<Long> applyIdList;

    public ResJoinAlliance(int code) {
        super(code);
    }
}
