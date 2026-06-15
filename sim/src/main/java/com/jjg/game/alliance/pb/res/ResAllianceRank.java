package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceRankInfo;

import java.util.List;

/**
 * 联盟声誉排行返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_ALLIANCE_RANK, resp = true)
@ProtoDesc("联盟声誉排行返回")
public class ResAllianceRank extends AbstractResponse {
    @ProtoDesc("榜单(前10)")
    public List<AllianceRankInfo> list;
    @ProtoDesc("我所在联盟排名(无盟为null, 未上榜rank=-1)")
    public AllianceRankInfo my;

    public ResAllianceRank(int code) {
        super(code);
    }
}
