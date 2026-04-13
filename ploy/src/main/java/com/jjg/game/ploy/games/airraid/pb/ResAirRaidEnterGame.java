package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/27
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.RES_AIR_RAID_ENTER_GAME, resp = true)
@ProtoDesc("进入游戏返回")
public class ResAirRaidEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Integer> stakeList;
    @ProtoDesc("阶段配置信息 k(阶段) -> 0.下注  1.停止下注  2.飞行(配置中没有该配置)  3.结算 , v(阶段时长，单位:毫秒) ")
    public List<KVInfo> phaseCfgList;
    @ProtoDesc("当局投注信息")
    public List<AirRaidBetInfo> betInfoList;
    @ProtoDesc("回合历史")
    public List<Integer> roundHistory;
    @ProtoDesc("当前阶段 0=下注 1=飞行 2=坠毁")
    public int phase;
    @ProtoDesc("当前倍率(万分比，10000=1.00x)")
    public int currentMultiplier;
    @ProtoDesc("阶段停止时间(毫秒)")
    public long phaseStopTime;

    public ResAirRaidEnterGame(int code) {
        super(code);
    }
}
