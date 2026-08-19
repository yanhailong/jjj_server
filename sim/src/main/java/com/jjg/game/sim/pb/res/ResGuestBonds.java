package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/6/1
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_UNLOCK_BONDS, resp = true)
@ProtoDesc("获取已解锁羁绊返回")
public class ResGuestBonds extends AbstractResponse {
    public List<Integer> bonds;
    public List<KVInfo> rewards;

    public ResGuestBonds(int code) {
        super(code);
    }
}
