package com.jjg.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;

/**
 * @author 11
 * @date 2025/6/19 14:58
 */
@ProtobufMessage(messageType = DollarExpressConst.MsgBean.TYPE, cmd = DollarExpressConst.MsgBean.RES_CHOOSE_FREE_MODEL,resp = true)
@ProtoDesc("返回选择免费模式的游戏")
public class ResChooseFreeModel extends AbstractResponse {
    @ProtoDesc("火车模式")
    public TrainInfo trainInfo;


    public ResChooseFreeModel(int code) {
        super(code);
    }
}
