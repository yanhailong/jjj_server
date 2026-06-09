package com.jjg.game.ploy.games.hillo.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;
import com.jjg.game.ploy.games.hillo.pb.bean.HilloChooseInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HILLO, cmd = HilloConstant.MsgBean.RES_HILLO_BET, resp = true)
@ProtoDesc("HILLO 下注结果")
public class ResHilloBet extends AbstractResponse {
    @ProtoDesc("当前公牌")
    public int currentCard;
    @ProtoDesc("本局剩余可猜次数")
    public int remainRoundNum;
    @ProtoDesc("本局剩余跳过次数")
    public int remainSkipTimes;
    @ProtoDesc("当前可选投注项")
    public List<HilloChooseInfo> chooseInfos;

    public ResHilloBet(int code) {
        super(code);
    }
}
