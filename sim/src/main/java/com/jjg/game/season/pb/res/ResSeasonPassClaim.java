package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;
import com.jjg.game.season.pb.struct.SeasonPassInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_PASS_CLAIM, resp = true)
@ProtoDesc("一键领取通行证奖励返回")
public class ResSeasonPassClaim extends AbstractResponse {
    public int passId;
    public List<ItemInfo> rewards;
    public SeasonPassInfo pass;

    public ResSeasonPassClaim(int code) {
        super(code);
    }
}
