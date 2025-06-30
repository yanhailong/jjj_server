package com.jjg.game.hall.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.data.WareHouseConfigInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/10 17:03
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_ENTER_GAME,resp = true)
@ProtoDesc("进入游戏返回")
public class ResChooseGame extends AbstractResponse {
    @ProtoDesc("场次列表")
    public List<WareHouseConfigInfo> wareHouseList;

    public ResChooseGame(int code) {
        super(code);
    }
}
