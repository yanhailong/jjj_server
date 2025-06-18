
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
 public class DollarExpressLineConfig extends Sample{
    public static SampleFactory<DollarExpressLineConfig> factory = new SampleFactoryImpl<>();
    public static DollarExpressLineConfig getDollarExpressLineConfig(int sid) {
        return (DollarExpressLineConfig)factory.getSample(sid);
    }

    public static DollarExpressLineConfig newDollarExpressLineConfig(int sid) {
        return (DollarExpressLineConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("中奖线轴1")
	public int yLine1;
	@Tag(4)
	@ProtoDesc("中奖线轴2")
	public int yLine2;
	@Tag(5)
	@ProtoDesc("中奖线轴3")
	public int yLine3;
	@Tag(6)
	@ProtoDesc("中奖线轴4")
	public int yLine4;
	@Tag(7)
	@ProtoDesc("中奖线轴5")
	public int yLine5;

 }
