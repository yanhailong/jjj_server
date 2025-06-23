
 package com.jjg.game.sample;

 import com.jjg.game.core.sample.Sample;
 import com.jjg.game.core.sample.SampleFactory;
 import com.jjg.game.core.sample.SampleFactoryImpl;
 import com.jjg.game.common.proto.ProtoDesc;
 import io.protostuff.Tag;
 import com.jjg.game.common.proto.ProtobufMessage;

/**
 * Auto generate by "Python tools"
 * @Date 2025-06-23 12:47:43
 */
 @ProtobufMessage
 public class DollarExpressGolbalConfig extends Sample{
    public static SampleFactory<DollarExpressGolbalConfig> factory = new SampleFactoryImpl<>();
    public static DollarExpressGolbalConfig getDollarExpressGolbalConfig(int sid) {
        return (DollarExpressGolbalConfig)factory.getSample(sid);
    }

    public static DollarExpressGolbalConfig newDollarExpressGolbalConfig(int sid) {
        return (DollarExpressGolbalConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("值")
	private String value;

 	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}


 }
