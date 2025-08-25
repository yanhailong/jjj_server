package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.pb.struct.ItemInfo;
import com.jjg.game.hall.pb.struct.PackItemInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/11 15:45
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_USE_ITEM,resp = true)
@ProtoDesc("使用道具返回")
public class ResUseItem extends AbstractResponse {
    @ProtoDesc("背包中的道具")
    public List<PackItemInfo> packItemInfos;
    @ProtoDesc("增加的道具")
    public List<ItemInfo> addItemInfos;

    public ResUseItem(int code) {
        super(code);
    }
}
