package com.jjg.game.slots.game.thor.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.thor.ThorConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/12/1 18:13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.THOR, cmd = ThorConstant.MsgBean.RES_ENTER_GAME, resp = true)
@ProtoDesc("进入游戏，返回配置信息")
public class ResThorEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("状态  0.普通   1.二选一   2.火焰   3.冰冻")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("奖池配置信息")
    public List<ThorPoolInfo> poolList;

    public ResThorEnterGame(int code) {
        super(code);
    }
}
