package com.jjg.game.ploy.games.hillo.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("HILLO choose info")
public class HilloChooseInfo {
    @ProtoDesc("choose id")
    public int chooseId;
    @ProtoDesc("choose name")
    public String chooseName;
    @ProtoDesc("odd")
    public String odd;
    @ProtoDesc("win rate")
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
