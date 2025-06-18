package com.jjg.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/13 14:00
 */
@ProtobufMessage(messageType = DollarExpressConst.MSGBEAN.TYPE, cmd = DollarExpressConst.MSGBEAN.RES_CHOOSE_WARE,resp = true)
@ProtoDesc("选择游戏场次进入")
public class ResChooseWare extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;

    public ResChooseWare(int code) {
        super(code);
    }
}