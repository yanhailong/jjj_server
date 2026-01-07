package com.jjg.game.table.common.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.common.message.TableRoomMessageConstant;
import com.jjg.game.table.common.message.bean.TablePlayerInfo;

/**
 * room请求玩家信息
 *
 * @author lm
 * @date 2026/1/6 15:26
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE, cmd = TableRoomMessageConstant.RespMsgBean.RES_PLAYER_INFO, resp = true)
@ProtoDesc("请求获取玩家信息")
public class ResPlayerInfo extends AbstractResponse {
    public ResPlayerInfo(int code) {
        super(code);
    }
    @ProtoDesc("玩家信息")
    public TablePlayerInfo tablePlayerInfo;
}
