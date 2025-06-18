package com.jjg.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/12 17:11
 */
@ProtobufMessage(messageType = DollarExpressConst.MSGBEAN.TYPE, cmd = DollarExpressConst.MSGBEAN.RES_START_GAME,resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("中奖信息")
    public List<ResultLineInfo> resultLineInfoList;
    @ProtoDesc("累计中奖金币")
    public long allWinGold;
    @ProtoDesc("特殊游戏id")
    public int specialId;

    public ResStartGame(int code) {
        super(code);
    }
}
