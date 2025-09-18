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
    @ProtoDesc("类型  1.热卖   2.金币   3.钻石")
    public int type;
    @ProtoDesc("结束时间")
    public int endTime;
    @ProtoDesc("货币的itemId")
    public int currencyItemId;
    @ProtoDesc("原价数量")
    public long originalCount;
    @ProtoDesc("当前数量")
    public long currentCount;
    @ProtoDesc("价格")
    public int money;
    @ProtoDesc("标签1   1.best   2.most popular")
    public int label1;
    @ProtoDesc("标签2")
    public int label2;
    @ProtoDesc("图片地址")
    public String pic;
}
