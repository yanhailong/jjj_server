package com.jjg.game.slots.game.steamAge.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.steamAge.SteamAgeConstant;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/12/2 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.STEAM_AGE, cmd = SteamAgeConstant.MsgBean.RES_CONFIG_INFO, resp = true)
@ProtoDesc("返回配置信息")
public class ResSteamAgeEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("累计中奖金币")
    public long totalWinGold;
    @ProtoDesc("当前状态 0.正常  1.免费")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("奖池信息")
    public List<SteamAgePoolInfo> poolList;
    @ProtoDesc("连续倍数")
    public int baseTimes;

    public ResSteamAgeEnterGame(int code) {
        super(code);
    }
}
