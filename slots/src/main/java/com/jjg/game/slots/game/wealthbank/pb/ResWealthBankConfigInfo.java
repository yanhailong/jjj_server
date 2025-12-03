package com.jjg.game.slots.game.wealthbank.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wealthbank.WealthBankConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/30 15:21
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WEALTH_BANK, cmd = WealthBankConstant.MsgBean.RES_WEALTH_BANK_CONFIG_INFO, resp = true)
@ProtoDesc("请求配置信息")
public class ResWealthBankConfigInfo extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("奖池信息")
    public List<WealthBankPoolInfo> poolList;
    @ProtoDesc("美元收集目标值")
    public int dollarTargetCount;
    @ProtoDesc("达到收集美元的最低押注")
    public long collectMinStake;
    @ProtoDesc("当前已收集美元个数")
    public int dollarCollectedCount;

    public ResWealthBankConfigInfo(int code) {
        super(code);
    }
}
