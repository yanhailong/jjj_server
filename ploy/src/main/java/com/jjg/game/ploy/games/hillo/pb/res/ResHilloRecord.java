package com.jjg.game.ploy.games.hillo.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;
import com.jjg.game.ploy.games.hillo.pb.bean.HilloRecordInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HILLO, cmd = HilloConstant.MsgBean.RES_HILLO_RECORD, resp = true)
@ProtoDesc("HILLO 历史记录结果")
public class ResHilloRecord extends AbstractResponse {
    @ProtoDesc("历史记录列表")
    public List<HilloRecordInfo> historyInfoList;

    public ResHilloRecord(int code) {
        super(code);
    }
}
