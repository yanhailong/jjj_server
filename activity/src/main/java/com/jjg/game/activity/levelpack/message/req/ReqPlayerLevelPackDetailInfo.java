package com.jjg.game.activity.levelpack.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/3 17:47
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = ActivityConstant.MsgBean.REQ_PLAYER_LEVEL_PACK_DETAIL_INFO)
@ProtoDesc("等级礼包详细信息")
public class ReqPlayerLevelPackDetailInfo extends AbstractNotice {
}
