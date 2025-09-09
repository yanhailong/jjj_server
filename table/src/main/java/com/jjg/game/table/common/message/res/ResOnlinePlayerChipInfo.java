package com.jjg.game.table.common.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.common.message.TableRoomMessageConstant;
import com.jjg.game.table.common.message.bean.PlayerChip;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/8 18:22
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
        cmd = TableRoomMessageConstant.RespMsgBean.RES_ONLINE_PLAYER_CHIP_INFO
        ,resp = true)
@ProtoDesc("请求在线玩家筹码皮肤id")
public class ResOnlinePlayerChipInfo extends AbstractResponse {
    @ProtoDesc("筹码信息")
    public List<PlayerChip> chips;

    public ResOnlinePlayerChipInfo(int code) {
        super(code);
    }
}
