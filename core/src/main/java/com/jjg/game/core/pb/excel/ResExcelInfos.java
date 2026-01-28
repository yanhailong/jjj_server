package com.jjg.game.core.pb.excel;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2026/1/26 15:31
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.RES_EXCEL_INFOS, resp = true)
public class ResExcelInfos extends AbstractResponse {
    @ProtoDesc("数据")
    public String dataJsons;
    @ProtoDesc("数据id")
    public int dataId;

    public ResExcelInfos(int code) {
        super(code);
    }
}