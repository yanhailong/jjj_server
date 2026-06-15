package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.ItemInfo;

import java.util.List;

/**
 * 领取阶段奖励返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_BATTLE_STAGE_CLAIM, resp = true)
@ProtoDesc("领取阶段奖励返回")
public class ResBattleStageClaim extends AbstractResponse {
    @ProtoDesc("阶段序号")
    public int stage;
    @ProtoDesc("奖励列表")
    public List<ItemInfo> rewards;

    public ResBattleStageClaim(int code) {
        super(code);
    }
}
