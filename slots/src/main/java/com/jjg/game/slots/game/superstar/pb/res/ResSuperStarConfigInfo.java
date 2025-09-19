package com.jjg.game.slots.game.superstar.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.superstar.SuperStarConstant;
import com.jjg.game.slots.game.superstar.pb.SuperStarPoolInfo;

import java.util.List;

/**
 *
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SUPER_STAR_TYPE, cmd = SuperStarConstant.MsgBean.RES_CONFIG_INFO, resp = true)
@ProtoDesc("请求配置信息")
public class ResSuperStarConfigInfo extends AbstractResponse {

    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("奖池信息")
    public List<SuperStarPoolInfo> poolList;

    public ResSuperStarConfigInfo(int code) {
        super(code);
    }
}
