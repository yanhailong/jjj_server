package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/28
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SIM_PLAYER_INFO, resp = true)
@ProtoDesc("获取sim玩家信息返回")
public class ResSimPlayerInfo extends AbstractResponse {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("玩家昵称")
    public String playerName;
    @ProtoDesc("头像id")
    public int headImgId;
    @ProtoDesc("头像框id")
    public int headFrameId;
    @ProtoDesc("国旗id")
    public int nationalId;
    @ProtoDesc("联盟名称")
    public String allianceName;
    @ProtoDesc("以解锁场景id")
    public List<Integer> unlockCasinoIds;
    @ProtoDesc("VIP等级")
    public int vipLevel;
    @ProtoDesc("经营角色等级(所有娱乐城等级之和)")
    public int roleLevel;
    @ProtoDesc("注册时间(秒)")
    public int createTime;
    @ProtoDesc("性别")
    public int gender;
    @ProtoDesc("该玩家当前展示的成就勋章配置id")
    public List<Integer> displayedMedalIds;

    public ResSimPlayerInfo(int code) {
        super(code);
    }
}
