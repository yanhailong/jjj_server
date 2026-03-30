package com.jjg.game.slots.game.acedj.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/9/9 13:39
 */
@ProtobufMessage
@ProtoDesc("消除后补齐图标的信息")
public class AceDjCascade {
    @ProtoDesc("中奖的图标信息")
    public AceDjIconInfo rewardIconInfo;
    @ProtoDesc("补齐的图标id")
    public List<KVInfo> addIconInfos;
    @ProtoDesc("中奖倍数 key位置 1、2、3、4  value 倍数 （没中奖 可能为空list）")
    public List<KVInfo> winTimes;
    @ProtoDesc("中奖变更后的倍数  key位置 1、2、3、4 value 倍数")
    public List<KVInfo> times;
}
