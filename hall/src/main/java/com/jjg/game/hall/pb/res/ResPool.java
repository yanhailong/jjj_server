package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.pb.struct.WarePoolInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/7 10:05
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_POOL,resp = true)
@ProtoDesc("倍场界面奖池信息返回")
public class ResPool extends AbstractResponse {
    @ProtoDesc("倍场界面奖池信息")
    public List<WarePoolInfo> warePoolInfoList;
    public ResPool(int code) {
        super(code);
    }
}
