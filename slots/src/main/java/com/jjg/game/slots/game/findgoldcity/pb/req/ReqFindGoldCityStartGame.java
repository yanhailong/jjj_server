package com.jjg.game.slots.game.findgoldcity.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.findgoldcity.constant.FindGoldCityConstant;

/**
 * @author 11
 * @date 2025/8/1 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.FIND_GOLD_CITY, cmd = FindGoldCityConstant.MsgBean.REQ_FIND_GOLD_CITY_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqFindGoldCityStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
