package com.jjg.game.hall.levelpack.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.levelpack.message.bean.PlayerLevelPackDetailInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/3 17:47
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.NOTIFY_PLAYER_LEVEL_PACK_DETAIL_INFO, resp = true)
@ProtoDesc("等级礼包详细信息")
public class NotifyPlayerLevelPackDetailInfo extends AbstractNotice {
    @ProtoDesc("活动详细信息")
    public List<PlayerLevelPackDetailInfo> detailInfo;
}
