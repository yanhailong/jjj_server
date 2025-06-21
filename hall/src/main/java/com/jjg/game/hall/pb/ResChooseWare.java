package com.jjg.game.hall.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallMessageConst;

/**
 * @author 11
 * @date 2025/6/13 14:00
 */
@ProtobufMessage(messageType = HallMessageConst.MsgBean.TYPE, cmd = HallMessageConst.MsgBean.RES_CHOOSE_WARE,resp = true)
@ProtoDesc("选择游戏场次进入")
public class ResChooseWare extends AbstractResponse {

    public ResChooseWare(int code) {
        super(code);
    }
}