package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.slots.constant.SlotsConst;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/12 17:11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE, cmd = SlotsConst.MsgBean.RES_START_GAME,resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("中奖信息")
    public List<ResultLineInfo> resultLineInfoList;
    @ProtoDesc("累计中奖金币")
    public long allWinGold;
    @ProtoDesc("当前状态 0.正常  1.普通二选一  2.黄金列车二选一  3.二选一之拉普通火车  4.二选一之拉黄金火车  5.二选一之免费模式")
    public int status;
    @ProtoDesc("火车信息")
    public List<TrainInfo> trainInfoList;
    @ProtoDesc("美元信息")
    public DollarsInfo dollarsInfo;
    @ProtoDesc("累计的美元数量，进度条")
    public int totalDollars;

    public ResStartGame(int code) {
        super(code);
    }
}
