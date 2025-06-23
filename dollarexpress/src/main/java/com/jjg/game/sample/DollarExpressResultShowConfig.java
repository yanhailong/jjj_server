
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
 public class DollarExpressResultShowConfig extends Sample{
    public static SampleFactory<DollarExpressResultShowConfig> factory = new SampleFactoryImpl<>();
    public static DollarExpressResultShowConfig getDollarExpressResultShowConfig(int sid) {
        return (DollarExpressResultShowConfig)factory.getSample(sid);
    }

    public static DollarExpressResultShowConfig newDollarExpressResultShowConfig(int sid) {
        return (DollarExpressResultShowConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("类型")
	private int type;
	@Tag(4)
	@ProtoDesc("权重")
	private int weight;
	@Tag(5)
	@ProtoDesc("免费次数")
	private int freetime;
	@Tag(6)
	@ProtoDesc("位置0")
	private int icon_0;
	@Tag(7)
	@ProtoDesc("位置1")
	private int icon_1;
	@Tag(8)
	@ProtoDesc("位置2")
	private int icon_2;
	@Tag(9)
	@ProtoDesc("位置3")
	private int icon_3;
	@Tag(10)
	@ProtoDesc("位置4")
	private int icon_4;
	@Tag(11)
	@ProtoDesc("位置5")
	private int icon_5;
	@Tag(12)
	@ProtoDesc("位置6")
	private int icon_6;
	@Tag(13)
	@ProtoDesc("位置7")
	private int icon_7;
	@Tag(14)
	@ProtoDesc("位置8")
	private int icon_8;
	@Tag(15)
	@ProtoDesc("位置9")
	private int icon_9;
	@Tag(16)
	@ProtoDesc("位置10")
	private int icon_10;
	@Tag(17)
	@ProtoDesc("位置11")
	private int icon_11;
	@Tag(18)
	@ProtoDesc("位置12")
	private int icon_12;
	@Tag(19)
	@ProtoDesc("位置13")
	private int icon_13;
	@Tag(20)
	@ProtoDesc("位置14")
	private int icon_14;
	@Tag(21)
	@ProtoDesc("位置15")
	private int icon_15;
	@Tag(22)
	@ProtoDesc("位置16")
	private int icon_16;
	@Tag(23)
	@ProtoDesc("位置17")
	private int icon_17;
	@Tag(24)
	@ProtoDesc("位置18")
	private int icon_18;
	@Tag(25)
	@ProtoDesc("位置19")
	private int icon_19;

 	public int getType() {
		return this.type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getWeight() {
		return this.weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public int getFreetime() {
		return this.freetime;
	}

	public void setFreetime(int freetime) {
		this.freetime = freetime;
	}

	public int getIcon_0() {
		return this.icon_0;
	}

	public void setIcon_0(int icon_0) {
		this.icon_0 = icon_0;
	}

	public int getIcon_1() {
		return this.icon_1;
	}

	public void setIcon_1(int icon_1) {
		this.icon_1 = icon_1;
	}

	public int getIcon_2() {
		return this.icon_2;
	}

	public void setIcon_2(int icon_2) {
		this.icon_2 = icon_2;
	}

	public int getIcon_3() {
		return this.icon_3;
	}

	public void setIcon_3(int icon_3) {
		this.icon_3 = icon_3;
	}

	public int getIcon_4() {
		return this.icon_4;
	}

	public void setIcon_4(int icon_4) {
		this.icon_4 = icon_4;
	}

	public int getIcon_5() {
		return this.icon_5;
	}

	public void setIcon_5(int icon_5) {
		this.icon_5 = icon_5;
	}

	public int getIcon_6() {
		return this.icon_6;
	}

	public void setIcon_6(int icon_6) {
		this.icon_6 = icon_6;
	}

	public int getIcon_7() {
		return this.icon_7;
	}

	public void setIcon_7(int icon_7) {
		this.icon_7 = icon_7;
	}

	public int getIcon_8() {
		return this.icon_8;
	}

	public void setIcon_8(int icon_8) {
		this.icon_8 = icon_8;
	}

	public int getIcon_9() {
		return this.icon_9;
	}

	public void setIcon_9(int icon_9) {
		this.icon_9 = icon_9;
	}

	public int getIcon_10() {
		return this.icon_10;
	}

	public void setIcon_10(int icon_10) {
		this.icon_10 = icon_10;
	}

	public int getIcon_11() {
		return this.icon_11;
	}

	public void setIcon_11(int icon_11) {
		this.icon_11 = icon_11;
	}

	public int getIcon_12() {
		return this.icon_12;
	}

	public void setIcon_12(int icon_12) {
		this.icon_12 = icon_12;
	}

	public int getIcon_13() {
		return this.icon_13;
	}

	public void setIcon_13(int icon_13) {
		this.icon_13 = icon_13;
	}

	public int getIcon_14() {
		return this.icon_14;
	}

	public void setIcon_14(int icon_14) {
		this.icon_14 = icon_14;
	}

	public int getIcon_15() {
		return this.icon_15;
	}

	public void setIcon_15(int icon_15) {
		this.icon_15 = icon_15;
	}

	public int getIcon_16() {
		return this.icon_16;
	}

	public void setIcon_16(int icon_16) {
		this.icon_16 = icon_16;
	}

	public int getIcon_17() {
		return this.icon_17;
	}

	public void setIcon_17(int icon_17) {
		this.icon_17 = icon_17;
	}

	public int getIcon_18() {
		return this.icon_18;
	}

	public void setIcon_18(int icon_18) {
		this.icon_18 = icon_18;
	}

	public int getIcon_19() {
		return this.icon_19;
	}

	public void setIcon_19(int icon_19) {
		this.icon_19 = icon_19;
	}


 }
