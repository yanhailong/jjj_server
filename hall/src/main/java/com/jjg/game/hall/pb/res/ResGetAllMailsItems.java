package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.common.pb.ItemInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/14 10:13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_GET_ALL_MAILS_ITEMS,resp = true)
@ProtoDesc("一键领取邮件内的道具返回")
public class ResGetAllMailsItems extends AbstractResponse {
    @ProtoDesc("增加的道具")
    public List<ItemInfo> items;

    public ResGetAllMailsItems(int code) {
        super(code);
    }
}
