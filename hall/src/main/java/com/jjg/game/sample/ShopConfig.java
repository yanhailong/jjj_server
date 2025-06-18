
 package com.jjg.game.sample;

 import com.jjg.game.core.sample.Sample;
 import com.jjg.game.core.sample.SampleFactory;
 import com.jjg.game.core.sample.SampleFactoryImpl;
 import com.jjg.game.common.proto.ProtoDesc;
 import io.protostuff.Tag;
 import com.jjg.game.common.proto.ProtobufMessage;

/**
 * Auto generate by "Python tools"
 * @Date 2025-06-11 13:46:05
 */
 @ProtobufMessage
 public class ShopConfig extends Sample{
    public static SampleFactory<ShopConfig> factory = new SampleFactoryImpl<>();
    public static ShopConfig getShopConfig(int sid) {
        return (ShopConfig)factory.getSample(sid);
    }

    public static ShopConfig newShopConfig(int sid) {
        return (ShopConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("图标")
	public String icon1;
	@Tag(4)
	@ProtoDesc("标签")
	public String icon2;
	@Tag(5)
	@ProtoDesc("道具奖励")
	public String item;
	@Tag(6)
	@ProtoDesc("购买价格")
	public int money;
	@Tag(7)
	@ProtoDesc("分类")
	public int type;
	@Tag(8)
	@ProtoDesc("天数规则")
	public int day;
	@Tag(9)
	@ProtoDesc("等级条件")
	public int level;
	@Tag(10)
	@ProtoDesc("指定日期开放")
	public String yymmdd;
	@Tag(11)
	@ProtoDesc("存在时间")
	public String duration;
	@Tag(12)
	@ProtoDesc("折扣原价")
	public String discount;
	@Tag(13)
	@ProtoDesc("折扣")
	public String discountratio;

 }
