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
    @ProtoDesc("特殊游戏id  0.正常模式  1.拉火车  2.保险箱  3.免费  4.金火车")
    public int specialType;
    @ProtoDesc("免费次数")
    public int freeCount;
    @ProtoDesc("免费游戏中是否触发了金火车")
    public boolean goldTrainInFree;
    @ProtoDesc("火车模式")
    public List<TrainInfo> trainInfoList;
    @ProtoDesc("保险箱信息")
    public List<SafeBoxInfo> safeBoxInfoList;

    public ResStartGame(int code) {
        super(code);
    }
}
