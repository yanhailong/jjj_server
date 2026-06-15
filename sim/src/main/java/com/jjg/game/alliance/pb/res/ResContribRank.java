package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.ContribRankInfo;

import java.util.List;

/**
 * 贡献度周榜返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_CONTRIB_RANK, resp = true)
@ProtoDesc("贡献度周榜返回")
public class ResContribRank extends AbstractResponse {
    @ProtoDesc("榜单(前50)")
    public List<ContribRankInfo> list;
    @ProtoDesc("我的排名(未上榜rank=-1)")
    public ContribRankInfo my;

    public ResContribRank(int code) {
        super(code);
    }
}
