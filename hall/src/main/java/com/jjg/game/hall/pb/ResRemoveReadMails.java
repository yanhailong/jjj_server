package com.jjg.game.hall.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/14 10:06
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_REMOVE_READ_MAILS,resp = true)
@ProtoDesc("删除已读邮件返回")
public class ResRemoveReadMails extends AbstractResponse {
    public ResRemoveReadMails(int code) {
        super(code);
    }
}
