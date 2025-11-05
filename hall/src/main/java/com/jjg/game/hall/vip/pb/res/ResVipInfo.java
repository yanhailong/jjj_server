package com.jjg.game.hall.vip.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.vip.pb.bean.VipGiftInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/28 09:31
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = HallConstant.MsgBean.RES_VIP_INFO, resp = true)
@ProtoDesc("响应vip信息")
public class ResVipInfo extends AbstractResponse {
    @ProtoDesc("当前vip等级")
    public int vipLevel;
    @ProtoDesc("当前经验")
    public long nowExp;
    @ProtoDesc("当前充值金额")
    public String recharge;
    @ProtoDesc("到下级所需金额")
    public long needExp;
    @ProtoDesc("礼包信息")
    public List<VipGiftInfo> vipGiftInfo;
    @ProtoDesc("当前已领取的最大vip等级")
    public int claimMaxLv;
    public ResVipInfo(int code) {
        super(code);
    }
}
