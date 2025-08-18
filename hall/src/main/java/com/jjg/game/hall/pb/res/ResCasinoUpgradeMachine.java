package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.pb.ItemInfo;

/**
 * @author lm
 * @date 2025/8/18 14:37
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_CASINO_UPGRADE_MACHINE, resp = true)
@ProtoDesc("响应机台升级")
public class ResCasinoUpgradeMachine extends AbstractResponse {
    @ProtoDesc("机台id")
    public long machineId;
    @ProtoDesc("机台建造升级结束时间")
    public long buildLvUpEndTime;
    @ProtoDesc("加速花费道具")
    public ItemInfo itemInfo;
    @ProtoDesc("BuildingFunction配置表id")
    public int configId;
    public ResCasinoUpgradeMachine() {
        super(Code.SUCCESS);
    }
}
