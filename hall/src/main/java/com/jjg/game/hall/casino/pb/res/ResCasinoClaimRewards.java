package com.jjg.game.hall.casino.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.casino.pb.bean.CasinoSimpleInfo;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.pb.struct.ItemInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/18 14:33
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_CASINO_CLAIM_REWARDS, resp = true)
@ProtoDesc("响应领取赌场收益")
public class ResCasinoClaimRewards extends AbstractResponse {
    @ProtoDesc("机台id 为0时为一键领取")
    public long machineId;
    @ProtoDesc("奖励道具信息")
    public List<ItemInfo> itemInfos;
    @ProtoDesc("机台信息")
    public List<CasinoSimpleInfo> casinoSimpleInfos;

    public ResCasinoClaimRewards() {
        super(Code.SUCCESS);
    }
}
