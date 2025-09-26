package com.jjg.game.slots.game.mahjiongwin.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.slots.game.mahjiongwin.MahjiongWinConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MAHJIONG_WIN_TYPE, cmd = MahjiongWinConstant.MsgBean.RES_CONFIG_INFO, resp = true)
@ProtoDesc("返回配置信息")
public class ResMahjiongwinEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("连续中奖倍数信息")
    public List<MahjiongwinAddTimesInfo> timesInfoList;
    @ProtoDesc("累计中奖金币")
    public long totalWinGold;
    @ProtoDesc("当前状态 0.正常  1.免费")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;

    public ResMahjiongwinEnterGame(int code) {
        super(code);
    }
}
