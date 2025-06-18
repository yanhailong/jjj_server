package com.jjg.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/13 13:37
 */
@ProtobufMessage(messageType = DollarExpressConst.MSGBEAN.TYPE, cmd = DollarExpressConst.MSGBEAN.RES_WARE_HOUSE,resp = true)
@ProtoDesc("返回场次列表")
public class ResWareHouse extends AbstractResponse {
    @ProtoDesc("场次列表")
    public List<WareHouseConfigInfo> wareHouseList;

    public ResWareHouse(int code) {
        super(code);
    }
}
