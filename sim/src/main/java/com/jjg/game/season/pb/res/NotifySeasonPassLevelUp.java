package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON,
        cmd = SeasonConstant.MsgBean.NOTIFY_SEASON_PASS_LEVEL_UP, resp = true)
@ProtoDesc("通知通行证等级达成")
public class NotifySeasonPassLevelUp extends AbstractResponse {
    @ProtoDesc("PassList.id")
    public int passId;
    @ProtoDesc("已达成等级")
    public int level;

    public NotifySeasonPassLevelUp(int code) {
        super(code);
    }
}
