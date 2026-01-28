package com.jjg.game.core.pb.excel;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2026/1/26 15:31
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.REQ_EXCEL_INFOS)
public class ReqExcelInfos {
    @ProtoDesc("表名")
    public String tableName;
    @ProtoDesc("数据id -1为全部")
    public int dataId;
}
