package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.ContribRankInfo;

import java.util.List;

/**
 * 对决贡献榜单返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_BATTLE_RANK, resp = true)
@ProtoDesc("对决贡献榜单返回")
public class ResAllianceBattleRank extends AbstractResponse {
    @ProtoDesc("盟内全部比赛值排名")
    public List<ContribRankInfo> list;
    @ProtoDesc("我的排名")
    public ContribRankInfo my;

    public ResAllianceBattleRank(int code) {
        super(code);
    }
}
