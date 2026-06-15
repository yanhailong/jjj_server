package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceBrief;
import com.jjg.game.alliance.pb.struct.BattleStageInfo;

import java.util.List;

/**
 * 对决信息返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_BATTLE_INFO, resp = true)
@ProtoDesc("对决信息返回")
public class ResBattleInfo extends AbstractResponse {
    @ProtoDesc("期号")
    public String period;
    @ProtoDesc("阶段(AllianceConst.BattleState)")
    public int state;
    @ProtoDesc("报名开始(ms)")
    public long signupStartTime;
    @ProtoDesc("报名截止(ms)")
    public long signupEndTime;
    @ProtoDesc("开战时间(ms)")
    public long fightStartTime;
    @ProtoDesc("结束时间(ms)")
    public long fightEndTime;
    @ProtoDesc("我盟是否已报名")
    public boolean signedUp;
    @ProtoDesc("对手联盟概要(未匹配为null)")
    public AllianceBrief opponent;
    @ProtoDesc("我盟总积分")
    public long myScore;
    @ProtoDesc("对手总积分")
    public long oppScore;
    @ProtoDesc("我的个人积分")
    public long myPersonalScore;
    @ProtoDesc("阶段奖励列表")
    public List<BattleStageInfo> stages;
    @ProtoDesc("已结算时: 是否获胜(平局按胜利)")
    public boolean win;

    public ResBattleInfo(int code) {
        super(code);
    }
}
