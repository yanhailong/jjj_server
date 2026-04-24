package com.jjg.game.slots.game.hulk.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2026/4/23
 */
@ProtobufMessage
@ProtoDesc("小游戏")
public class HulkMiniGame {
    @ProtoDesc("汽车游戏是否结束")
    public boolean carOver;
    @ProtoDesc("汽车信息")
    public List<HulkCarInfo> miniGameCarList;
}
