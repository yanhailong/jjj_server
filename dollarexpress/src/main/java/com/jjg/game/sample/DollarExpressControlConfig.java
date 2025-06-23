
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
	private long entryConditionMin;
	@Tag(4)
	@ProtoDesc("进入条件最大值")
	private long entryConditionMax;
	@Tag(5)
	@ProtoDesc("轴_1权重")
	private int axle_1;
	@Tag(6)
	@ProtoDesc("轴_2权重")
	private int axle_2;
	@Tag(7)
	@ProtoDesc("轴_3权重")
	private int axle_3;
	@Tag(8)
	@ProtoDesc("轴_4权重")
	private int axle_4;
	@Tag(9)
	@ProtoDesc("轴_5权重")
	private int axle_5;
	@Tag(10)
	@ProtoDesc("轴_6权重")
	private int axle_6;
	@Tag(11)
	@ProtoDesc("轴_7权重")
	private int axle_7;
	@Tag(12)
	@ProtoDesc("拉火车")
	private int special_1;
	@Tag(13)
	@ProtoDesc("保险箱")
	private int special_2;
	@Tag(14)
	@ProtoDesc("免费")
	private int special_3;
	@Tag(15)
	@ProtoDesc("金火车")
	private int special_4;

 	public long getEntryConditionMin() {
		return this.entryConditionMin;
	}

	public void setEntryConditionMin(long entryConditionMin) {
		this.entryConditionMin = entryConditionMin;
	}

	public long getEntryConditionMax() {
		return this.entryConditionMax;
	}

	public void setEntryConditionMax(long entryConditionMax) {
		this.entryConditionMax = entryConditionMax;
	}

	public int getAxle_1() {
		return this.axle_1;
	}

	public void setAxle_1(int axle_1) {
		this.axle_1 = axle_1;
	}

	public int getAxle_2() {
		return this.axle_2;
	}

	public void setAxle_2(int axle_2) {
		this.axle_2 = axle_2;
	}

	public int getAxle_3() {
		return this.axle_3;
	}

	public void setAxle_3(int axle_3) {
		this.axle_3 = axle_3;
	}

	public int getAxle_4() {
		return this.axle_4;
	}

	public void setAxle_4(int axle_4) {
		this.axle_4 = axle_4;
	}

	public int getAxle_5() {
		return this.axle_5;
	}

	public void setAxle_5(int axle_5) {
		this.axle_5 = axle_5;
	}

	public int getAxle_6() {
		return this.axle_6;
	}

	public void setAxle_6(int axle_6) {
		this.axle_6 = axle_6;
	}

	public int getAxle_7() {
		return this.axle_7;
	}

	public void setAxle_7(int axle_7) {
		this.axle_7 = axle_7;
	}

	public int getSpecial_1() {
		return this.special_1;
	}

	public void setSpecial_1(int special_1) {
		this.special_1 = special_1;
	}

	public int getSpecial_2() {
		return this.special_2;
	}

	public void setSpecial_2(int special_2) {
		this.special_2 = special_2;
	}

	public int getSpecial_3() {
		return this.special_3;
	}

	public void setSpecial_3(int special_3) {
		this.special_3 = special_3;
	}

	public int getSpecial_4() {
		return this.special_4;
	}

	public void setSpecial_4(int special_4) {
		this.special_4 = special_4;
	}


 }
