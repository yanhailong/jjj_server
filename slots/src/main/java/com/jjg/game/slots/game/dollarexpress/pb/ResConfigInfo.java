package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/30 15:21
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE, cmd = DollarExpressConstant.MsgBean.RES_CONFIG_INFO, resp = true)
@ProtoDesc("请求配置信息")
public class ResConfigInfo extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("奖池信息")
    public List<PoolInfo> poolList;
    @ProtoDesc("美元收集目标值")
    public int dollarTargetCount;
    @ProtoDesc("达到收集美元的最低押注")
    public long collectMinStake;
    @ProtoDesc("当前已收集美元个数")
    public int dollarCollectedCount;
    @ProtoDesc("二选一免费次数")
    public int freeCount;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;

    public ResConfigInfo(int code) {
        super(code);
    }
}
