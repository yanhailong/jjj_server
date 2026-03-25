package com.jjg.game.slots.game.mahjiongwin2.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.mahjiongwin2.MahjiongWin2Constant;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MAHJIONG_WIN2_TYPE, cmd = MahjiongWin2Constant.MsgBean.RES_CONFIG_INFO, resp = true)
@ProtoDesc("返回配置信息")
public class ResMahjiongwin2EnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("连续中奖倍数信息")
    public List<Mahjiongwin2AddTimesInfo> timesInfoList;
    @ProtoDesc("累计中奖金币")
    public long totalWinGold;
    @ProtoDesc("当前状态 0.正常  1.免费")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;

    public ResMahjiongwin2EnterGame(int code) {
        super(code);
    }
}
