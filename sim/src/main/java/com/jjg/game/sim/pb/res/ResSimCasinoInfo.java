package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.CasinoUpgradeCondition;
import com.jjg.game.sim.pb.struct.CoopTaskInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_CASINO_INFO, resp = true)
@ProtoDesc("获取场景信息")
public class ResSimCasinoInfo extends AbstractResponse {
    @ProtoDesc("场景等级")
    public int level;
    @ProtoDesc("场景经验")
    public int exp;
    @ProtoDesc("升级消耗的经验")
    public int upgradeCost;
    @ProtoDesc("联盟id")
    public long allianceId;
    @ProtoDesc("场景id")
    public int casinoId;
    @ProtoDesc("今日剩余帮助次数(建筑加速)")
    public int remainHelp;
    @ProtoDesc("每日帮助上限(建筑加速)")
    public int dailyHelpLimit;
    @ProtoDesc("今日剩余建筑分享(加速求助)次数")
    public int remainShare;
    @ProtoDesc("每日建筑分享上限")
    public int dailyShareLimit;
    @ProtoDesc("已绑定的多人任务房间")
    public CoopTaskInfo coopTaskInfo;
    @ProtoDesc("升级需要的条件")
    public List<CasinoUpgradeCondition> upgradeLevelConditions;
    @ProtoDesc("好友同玩断线恢复游戏类型，0表示不恢复")
    public int togetherPlayGameType;
    @ProtoDesc("在线收益视频冷却结束时间，毫秒时间戳，0表示无冷却")
    public long adCdEndTime;

    public ResSimCasinoInfo(int code) {
        super(code);
    }
}
