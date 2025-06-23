
 package com.jjg.game.sample;

 import com.jjg.game.core.sample.Sample;
 import com.jjg.game.core.sample.SampleFactory;
 import com.jjg.game.core.sample.SampleFactoryImpl;
 import com.jjg.game.common.proto.ProtoDesc;
 import io.protostuff.Tag;
 import com.jjg.game.common.proto.ProtobufMessage;

/**
 * Auto generate by "Python tools"
 * @Date 2025-06-23 12:47:30
 */
 @ProtobufMessage
 public class GolbalConfig extends Sample{
    public static SampleFactory<GolbalConfig> factory = new SampleFactoryImpl<>();
    public static GolbalConfig getGolbalConfig(int sid) {
        return (GolbalConfig)factory.getSample(sid);
    }

    public static GolbalConfig newGolbalConfig(int sid) {
        return (GolbalConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("值")
	private int value;
	@Tag(4)
	@ProtoDesc("备注")
	private String mark;

 	public int getValue() {
		return this.value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public String getMark() {
		return this.mark;
	}

	public void setMark(String mark) {
		this.mark = mark;
	}


 }
