package com.jjg.game.hall.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/6/27 15:51
 */
@ProtobufMessage
@ProtoDesc("游戏列表配置")
public class GameListConfig {
    @ProtoDesc("配置id")
    public int sid;
    @ProtoDesc("名称")
    public String name;
    @ProtoDesc("状态  1.开启  2.关闭 ")
    public int status;
    @ProtoDesc("状态  0.大  1.小")
    public int iconType;
    @ProtoDesc("角标")
    public String rightTopIcon;
}
