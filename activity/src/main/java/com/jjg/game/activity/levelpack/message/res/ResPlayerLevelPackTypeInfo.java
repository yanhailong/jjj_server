package com.jjg.game.activity.levelpack.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.levelpack.message.bean.PlayerLevelPackActivity;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY,cmd = ActivityConstant.MsgBean.RES_PLAYER_LEVEL_PACK_TYPE_INFO,resp = true)
@ProtoDesc("等级礼包类型信息")
public class ResPlayerLevelPackTypeInfo extends AbstractResponse {
    @ProtoDesc("活动信息")
    public List<PlayerLevelPackActivity> activityData;

    public ResPlayerLevelPackTypeInfo(int code) {
        super(code);
    }
}
