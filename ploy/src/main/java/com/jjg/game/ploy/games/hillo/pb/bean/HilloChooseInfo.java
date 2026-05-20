package com.jjg.game.ploy.games.hillo.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("HILLO 投注项信息")
public class HilloChooseInfo {
    @ProtoDesc("投注项 id")
    public int chooseId;
    @ProtoDesc("投注项名称")
    public String chooseName;
    @ProtoDesc("赔率")
    public String odd;
    @ProtoDesc("命中概率")
    public String winRate;

    public HilloChooseInfo() {
    }

    public HilloChooseInfo(int chooseId, String chooseName, String odd, String winRate) {
        this.chooseId = chooseId;
        this.chooseName = chooseName;
        this.odd = odd;
        this.winRate = winRate;
    }
}
