
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
 public class DollarExpressWinShowConfig extends Sample{
    public static SampleFactory<DollarExpressWinShowConfig> factory = new SampleFactoryImpl<>();
    public static DollarExpressWinShowConfig getDollarExpressWinShowConfig(int sid) {
        return (DollarExpressWinShowConfig)factory.getSample(sid);
    }

    public static DollarExpressWinShowConfig newDollarExpressWinShowConfig(int sid) {
        return (DollarExpressWinShowConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("倍率=奖金/底注")
	public int payout_min;
	@Tag(4)
	@ProtoDesc("特效资源")
	public String icon;

 }
