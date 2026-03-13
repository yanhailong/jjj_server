package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.hall.constant.HallConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_ALL_NEW_GAMES, resp = true)
@ProtoDesc("所有的新游期待榜数据返回")
public class ResAllNewGames extends AbstractResponse {
    public List<KVInfo> list;

    public ResAllNewGames(int code) {
        super(code);
    }
}
