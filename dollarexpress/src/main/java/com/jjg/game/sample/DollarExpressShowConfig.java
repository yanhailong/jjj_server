
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
 public class DollarExpressShowConfig extends Sample{
    public static SampleFactory<DollarExpressShowConfig> factory = new SampleFactoryImpl<>();
    public static DollarExpressShowConfig getDollarExpressShowConfig(int sid) {
        return (DollarExpressShowConfig)factory.getSample(sid);
    }

    public static DollarExpressShowConfig newDollarExpressShowConfig(int sid) {
        return (DollarExpressShowConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("轴1_1")
	private int column_1_1;
	@Tag(4)
	@ProtoDesc("轴1_2")
	private int column_1_2;
	@Tag(5)
	@ProtoDesc("轴1_3")
	private int column_1_3;
	@Tag(6)
	@ProtoDesc("轴1_4")
	private int column_1_4;
	@Tag(7)
	@ProtoDesc("轴1_5")
	private int column_1_5;
	@Tag(8)
	@ProtoDesc("轴2_1")
	private int column_2_1;
	@Tag(9)
	@ProtoDesc("轴2_2")
	private int column_2_2;
	@Tag(10)
	@ProtoDesc("轴2_3")
	private int column_2_3;
	@Tag(11)
	@ProtoDesc("轴2_4")
	private int column_2_4;
	@Tag(12)
	@ProtoDesc("轴2_5")
	private int column_2_5;
	@Tag(13)
	@ProtoDesc("轴3_1")
	private int column_3_1;
	@Tag(14)
	@ProtoDesc("轴3_2")
	private int column_3_2;
	@Tag(15)
	@ProtoDesc("轴3_3")
	private int column_3_3;
	@Tag(16)
	@ProtoDesc("轴3_4")
	private int column_3_4;
	@Tag(17)
	@ProtoDesc("轴3_5")
	private int column_3_5;
	@Tag(18)
	@ProtoDesc("轴4_1")
	private int column_4_1;
	@Tag(19)
	@ProtoDesc("轴4_2")
	private int column_4_2;
	@Tag(20)
	@ProtoDesc("轴4_3")
	private int column_4_3;
	@Tag(21)
	@ProtoDesc("轴4_4")
	private int column_4_4;
	@Tag(22)
	@ProtoDesc("轴4_5")
	private int column_4_5;
	@Tag(23)
	@ProtoDesc("轴5_1")
	private int column_5_1;
	@Tag(24)
	@ProtoDesc("轴5_2")
	private int column_5_2;
	@Tag(25)
	@ProtoDesc("轴5_3")
	private int column_5_3;
	@Tag(26)
	@ProtoDesc("轴5_4")
	private int column_5_4;
	@Tag(27)
	@ProtoDesc("轴5_5")
	private int column_5_5;
	@Tag(28)
	@ProtoDesc("轴6_1")
	private int column_6_1;
	@Tag(29)
	@ProtoDesc("轴6_2")
	private int column_6_2;
	@Tag(30)
	@ProtoDesc("轴6_3")
	private int column_6_3;
	@Tag(31)
	@ProtoDesc("轴6_4")
	private int column_6_4;
	@Tag(32)
	@ProtoDesc("轴6_5")
	private int column_6_5;
	@Tag(33)
	@ProtoDesc("轴7_1")
	private int column_7_1;
	@Tag(34)
	@ProtoDesc("轴7_2")
	private int column_7_2;
	@Tag(35)
	@ProtoDesc("轴7_3")
	private int column_7_3;
	@Tag(36)
	@ProtoDesc("轴7_4")
	private int column_7_4;
	@Tag(37)
	@ProtoDesc("轴7_5")
	private int column_7_5;

 	public int getColumn_1_1() {
		return this.column_1_1;
	}

	public void setColumn_1_1(int column_1_1) {
		this.column_1_1 = column_1_1;
	}

	public int getColumn_1_2() {
		return this.column_1_2;
	}

	public void setColumn_1_2(int column_1_2) {
		this.column_1_2 = column_1_2;
	}

	public int getColumn_1_3() {
		return this.column_1_3;
	}

	public void setColumn_1_3(int column_1_3) {
		this.column_1_3 = column_1_3;
	}

	public int getColumn_1_4() {
		return this.column_1_4;
	}

	public void setColumn_1_4(int column_1_4) {
		this.column_1_4 = column_1_4;
	}

	public int getColumn_1_5() {
		return this.column_1_5;
	}

	public void setColumn_1_5(int column_1_5) {
		this.column_1_5 = column_1_5;
	}

	public int getColumn_2_1() {
		return this.column_2_1;
	}

	public void setColumn_2_1(int column_2_1) {
		this.column_2_1 = column_2_1;
	}

	public int getColumn_2_2() {
		return this.column_2_2;
	}

	public void setColumn_2_2(int column_2_2) {
		this.column_2_2 = column_2_2;
	}

	public int getColumn_2_3() {
		return this.column_2_3;
	}

	public void setColumn_2_3(int column_2_3) {
		this.column_2_3 = column_2_3;
	}

	public int getColumn_2_4() {
		return this.column_2_4;
	}

	public void setColumn_2_4(int column_2_4) {
		this.column_2_4 = column_2_4;
	}

	public int getColumn_2_5() {
		return this.column_2_5;
	}

	public void setColumn_2_5(int column_2_5) {
		this.column_2_5 = column_2_5;
	}

	public int getColumn_3_1() {
		return this.column_3_1;
	}

	public void setColumn_3_1(int column_3_1) {
		this.column_3_1 = column_3_1;
	}

	public int getColumn_3_2() {
		return this.column_3_2;
	}

	public void setColumn_3_2(int column_3_2) {
		this.column_3_2 = column_3_2;
	}

	public int getColumn_3_3() {
		return this.column_3_3;
	}

	public void setColumn_3_3(int column_3_3) {
		this.column_3_3 = column_3_3;
	}

	public int getColumn_3_4() {
		return this.column_3_4;
	}

	public void setColumn_3_4(int column_3_4) {
		this.column_3_4 = column_3_4;
	}

	public int getColumn_3_5() {
		return this.column_3_5;
	}

	public void setColumn_3_5(int column_3_5) {
		this.column_3_5 = column_3_5;
	}

	public int getColumn_4_1() {
		return this.column_4_1;
	}

	public void setColumn_4_1(int column_4_1) {
		this.column_4_1 = column_4_1;
	}

	public int getColumn_4_2() {
		return this.column_4_2;
	}

	public void setColumn_4_2(int column_4_2) {
		this.column_4_2 = column_4_2;
	}

	public int getColumn_4_3() {
		return this.column_4_3;
	}

	public void setColumn_4_3(int column_4_3) {
		this.column_4_3 = column_4_3;
	}

	public int getColumn_4_4() {
		return this.column_4_4;
	}

	public void setColumn_4_4(int column_4_4) {
		this.column_4_4 = column_4_4;
	}

	public int getColumn_4_5() {
		return this.column_4_5;
	}

	public void setColumn_4_5(int column_4_5) {
		this.column_4_5 = column_4_5;
	}

	public int getColumn_5_1() {
		return this.column_5_1;
	}

	public void setColumn_5_1(int column_5_1) {
		this.column_5_1 = column_5_1;
	}

	public int getColumn_5_2() {
		return this.column_5_2;
	}

	public void setColumn_5_2(int column_5_2) {
		this.column_5_2 = column_5_2;
	}

	public int getColumn_5_3() {
		return this.column_5_3;
	}

	public void setColumn_5_3(int column_5_3) {
		this.column_5_3 = column_5_3;
	}

	public int getColumn_5_4() {
		return this.column_5_4;
	}

	public void setColumn_5_4(int column_5_4) {
		this.column_5_4 = column_5_4;
	}

	public int getColumn_5_5() {
		return this.column_5_5;
	}

	public void setColumn_5_5(int column_5_5) {
		this.column_5_5 = column_5_5;
	}

	public int getColumn_6_1() {
		return this.column_6_1;
	}

	public void setColumn_6_1(int column_6_1) {
		this.column_6_1 = column_6_1;
	}

	public int getColumn_6_2() {
		return this.column_6_2;
	}

	public void setColumn_6_2(int column_6_2) {
		this.column_6_2 = column_6_2;
	}

	public int getColumn_6_3() {
		return this.column_6_3;
	}

	public void setColumn_6_3(int column_6_3) {
		this.column_6_3 = column_6_3;
	}

	public int getColumn_6_4() {
		return this.column_6_4;
	}

	public void setColumn_6_4(int column_6_4) {
		this.column_6_4 = column_6_4;
	}

	public int getColumn_6_5() {
		return this.column_6_5;
	}

	public void setColumn_6_5(int column_6_5) {
		this.column_6_5 = column_6_5;
	}

	public int getColumn_7_1() {
		return this.column_7_1;
	}

	public void setColumn_7_1(int column_7_1) {
		this.column_7_1 = column_7_1;
	}

	public int getColumn_7_2() {
		return this.column_7_2;
	}

	public void setColumn_7_2(int column_7_2) {
		this.column_7_2 = column_7_2;
	}

	public int getColumn_7_3() {
		return this.column_7_3;
	}

	public void setColumn_7_3(int column_7_3) {
		this.column_7_3 = column_7_3;
	}

	public int getColumn_7_4() {
		return this.column_7_4;
	}

	public void setColumn_7_4(int column_7_4) {
		this.column_7_4 = column_7_4;
	}

	public int getColumn_7_5() {
		return this.column_7_5;
	}

	public void setColumn_7_5(int column_7_5) {
		this.column_7_5 = column_7_5;
	}


 }
