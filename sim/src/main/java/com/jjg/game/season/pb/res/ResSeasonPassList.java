package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;
import com.jjg.game.season.pb.struct.SeasonPassInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_PASS_LIST, resp = true)
@ProtoDesc("当前赛季通行证列表")
public class ResSeasonPassList extends AbstractResponse {
    public List<SeasonPassInfo> passes;

    public ResSeasonPassList(int code) {
        super(code);
    }
}
