package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.pb.struct.NoticeInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/11/10 11:16
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_ALL_NOTICE,resp = true)
@ProtoDesc("公告信息返回")
public class ResAllNotice extends AbstractResponse {
    @ProtoDesc("列表")
    public List<NoticeInfo> noticeList;

    public ResAllNotice(int code) {
        super(code);
    }
}
