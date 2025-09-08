package com.jjg.game.hall.casino.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.Code;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.casino.pb.bean.CasinoFloorInfo;
import com.jjg.game.common.pb.ItemInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/18 14:54
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_CASINO_INFO, resp = true)
@ProtoDesc("响应赌场信息")
public class ResCasinoInfo extends AbstractResponse {
    @ProtoDesc("赌场id")
    public int casinoId;
    @ProtoDesc("一键领取结束时间")
    public long claimAllRewardsEndTime;
    @ProtoDesc("一键领取购买消耗")
    public ItemInfo itemInfo;
    @ProtoDesc("楼层信息")
    public List<CasinoFloorInfo> casinoFloorInfos;
    @ProtoDesc("当前最高解锁楼层")
    public int maxUnlockFloorId;

    public ResCasinoInfo() {
        super(Code.SUCCESS);
    }
}
