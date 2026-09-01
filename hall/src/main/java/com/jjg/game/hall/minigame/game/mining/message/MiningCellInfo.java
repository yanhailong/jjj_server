package com.jjg.game.hall.minigame.game.mining.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.ItemInfo;
import java.util.List;

@ProtobufMessage
@ProtoDesc("矿格")
public class MiningCellInfo {
    @ProtoDesc("绝对行，从1开始")
    public int row;
    @ProtoDesc("列，从1开始")
    public int column;
    @ProtoDesc("MiningCellType ID")
    public int typeId;
    @ProtoDesc("剩余耐久，0已挖开")
    public int hp;
    @ProtoDesc("是否可用镐挖掘")
    public boolean connected;
    @ProtoDesc("秘境ID，0无")
    public long secretId;
}
