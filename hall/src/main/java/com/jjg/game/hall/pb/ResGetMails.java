package com.jjg.game.hall.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/13 18:11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_GET_MAILS,resp = true)
@ProtoDesc("返回邮件数据")
public class ResGetMails extends AbstractResponse {
    @ProtoDesc("邮件列表")
    public List<MailInfo> mails;
    public ResGetMails(int code) {
        super(code);
    }
}
