package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/10/20 18:13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTIFY_EXCEL_CHANGE, resp = true,toPbFile = false)
@ProtoDesc("通知excel更新")
public class NotifyExcelChange extends AbstractNotice {
    public List<String> nameList;
}
