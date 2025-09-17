package com.jjg.game.hall.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/9/17 16:23
 */
@ProtobufMessage
@ProtoDesc("商品信息")
public class ShopProductInfo {
    @ProtoDesc("商品id")
    public int id;
    @ProtoDesc("价格")
    public int money;
    @ProtoDesc("结束时间")
    public int endTime;
}
