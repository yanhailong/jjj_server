package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/19
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.RES_AIR_RAID_RECORD, resp = true)
@ProtoDesc("返回个人历史记录")
public class ResAirRaidRecord extends AbstractResponse {
    @ProtoDesc("历史记录列表(按时间倒序)")
    public List<AirRaidRecordInfo> records;
    @ProtoDesc("页码")
    public int pageIndex;
    @ProtoDesc("总页码")
    public int totalPages;

    public ResAirRaidRecord(int code) {
        super(code);
    }
}
