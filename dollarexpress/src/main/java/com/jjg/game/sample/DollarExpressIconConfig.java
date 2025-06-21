
 package com.jjg.game.sample;

 import com.jjg.game.core.sample.Sample;
 import com.jjg.game.core.sample.SampleFactory;
 import com.jjg.game.core.sample.SampleFactoryImpl;
 import com.jjg.game.common.proto.ProtoDesc;
 import io.protostuff.Tag;
 import com.jjg.game.common.proto.ProtobufMessage;

/**
 * Auto generate by "Python tools"
 * @Date 2025-06-20 11:50:22
 */
 @ProtobufMessage
 public class DollarExpressIconConfig extends Sample{
    public static SampleFactory<DollarExpressIconConfig> factory = new SampleFactoryImpl<>();
    public static DollarExpressIconConfig getDollarExpressIconConfig(int sid) {
        return (DollarExpressIconConfig)factory.getSample(sid);
    }

    public static DollarExpressIconConfig newDollarExpressIconConfig(int sid) {
        return (DollarExpressIconConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("资源名称")
	public String icon;
	@Tag(4)
	@ProtoDesc("1连倍率")
	public int payout_1;
	@Tag(5)
	@ProtoDesc("2连倍率")
	public int payout_2;
	@Tag(6)
	@ProtoDesc("3连倍率")
	public int payout_3;
	@Tag(7)
	@ProtoDesc("4连倍率")
	public int payout_4;
	@Tag(8)
	@ProtoDesc("5连倍率")
	public int payout_5;
	@Tag(9)
	@ProtoDesc("图标类型")
	public int type;
	@Tag(10)
	@ProtoDesc("乘以额外倍数")
	public int doubling;
	@Tag(11)
	@ProtoDesc("不中奖时用于填充的权重")
	public int noWinning;

 }
