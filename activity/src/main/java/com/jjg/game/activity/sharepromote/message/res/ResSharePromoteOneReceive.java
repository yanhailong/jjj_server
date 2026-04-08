package com.jjg.game.activity.sharepromote.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/16 15:45
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_SHARE_PROMOTE_ONE_RECEIVE, resp = true)
@ProtoDesc("一键领取绑定相关收益")
public class ResSharePromoteOneReceive extends AbstractResponse {
    @ProtoDesc("获得道具信息")
    public List<ItemInfo> itemInfos;
    public ResSharePromoteOneReceive(int code) {
        super(code);
    }
}
