package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/28
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_CLEAR_BUILDING_CD)
@ProtoDesc("清除建筑升级CD")
public class ReqClearBuildingCD extends AbstractMessage {
    @ProtoDesc("建筑id")
    public int id;
    @ProtoDesc("消耗道具列表 (itemId -> count)")
    public List<KVInfo> costItems;
}
