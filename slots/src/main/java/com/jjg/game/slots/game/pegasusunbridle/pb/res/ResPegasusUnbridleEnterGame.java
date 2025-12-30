package com.jjg.game.slots.game.pegasusunbridle.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.pegasusunbridle.constant.PegasusUnbridleConstant;
import com.jjg.game.slots.game.pegasusunbridle.pb.bean.PegasusUnbridlePoolInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PEGASUS_UNBRIDLE, cmd = PegasusUnbridleConstant.MsgBean.RES_PEGASUS_UNBRIDLE_ENTER_GAME, resp = true)
@ProtoDesc("返回配置信息")
public class ResPegasusUnbridleEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("累计中奖金币")
    public long totalWinGold;
    @ProtoDesc("当前状态 0.正常 ")
    public int status;
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("滚轴类型")
    public int scrollType;
    @ProtoDesc("奖池信息")
    public List<PegasusUnbridlePoolInfo> poolList;
    public ResPegasusUnbridleEnterGame(int code) {
        super(code);
    }
}
