
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
	private int yLine1;
	@Tag(4)
	@ProtoDesc("中奖线轴2")
	private int yLine2;
	@Tag(5)
	@ProtoDesc("中奖线轴3")
	private int yLine3;
	@Tag(6)
	@ProtoDesc("中奖线轴4")
	private int yLine4;
	@Tag(7)
	@ProtoDesc("中奖线轴5")
	private int yLine5;

 	public int getYLine1() {
		return this.yLine1;
	}

	public void setYLine1(int yLine1) {
		this.yLine1 = yLine1;
	}

	public int getYLine2() {
		return this.yLine2;
	}

	public void setYLine2(int yLine2) {
		this.yLine2 = yLine2;
	}

	public int getYLine3() {
		return this.yLine3;
	}

	public void setYLine3(int yLine3) {
		this.yLine3 = yLine3;
	}

	public int getYLine4() {
		return this.yLine4;
	}

	public void setYLine4(int yLine4) {
		this.yLine4 = yLine4;
	}

	public int getYLine5() {
		return this.yLine5;
	}

	public void setYLine5(int yLine5) {
		this.yLine5 = yLine5;
	}


 }
