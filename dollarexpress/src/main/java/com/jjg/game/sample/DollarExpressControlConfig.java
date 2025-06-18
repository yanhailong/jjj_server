
 package com.jjg.game.sample;

 import com.jjg.game.core.sample.Sample;
 import com.jjg.game.core.sample.SampleFactory;
 import com.jjg.game.core.sample.SampleFactoryImpl;
 import com.jjg.game.common.proto.ProtoDesc;
 import io.protostuff.Tag;
 import com.jjg.game.common.proto.ProtobufMessage;

/**
 * Auto generate by "Python tools"
 * @Date 2025-06-17 17:20:40
 */
 @ProtobufMessage
 public class DollarExpressControlConfig extends Sample{
    public static SampleFactory<DollarExpressControlConfig> factory = new SampleFactoryImpl<>();
    public static DollarExpressControlConfig getDollarExpressControlConfig(int sid) {
        return (DollarExpressControlConfig)factory.getSample(sid);
    }

    public static DollarExpressControlConfig newDollarExpressControlConfig(int sid) {
        return (DollarExpressControlConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("进入条件最小值")
	public long entryConditionMin;
	@Tag(4)
	@ProtoDesc("进入条件最大值")
	public long entryConditionMax;
	@Tag(5)
	@ProtoDesc("轴_1权重")
	public int axle_1;
	@Tag(6)
	@ProtoDesc("轴_2权重")
	public int axle_2;
	@Tag(7)
	@ProtoDesc("轴_3权重")
	public int axle_3;
	@Tag(8)
	@ProtoDesc("轴_4权重")
	public int axle_4;
	@Tag(9)
	@ProtoDesc("轴_5权重")
	public int axle_5;
	@Tag(10)
	@ProtoDesc("轴_6权重")
	public int axle_6;
	@Tag(11)
	@ProtoDesc("轴_7权重")
	public int axle_7;
	@Tag(12)
	@ProtoDesc("拉火车")
	public int special_1;
	@Tag(13)
	@ProtoDesc("保险箱")
	public int special_2;
	@Tag(14)
	@ProtoDesc("免费")
	public int special_3;
	@Tag(15)
	@ProtoDesc("金火车")
	public int special_4;

 }
