
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
	public int column_1_1;
	@Tag(4)
	@ProtoDesc("轴1_2")
	public int column_1_2;
	@Tag(5)
	@ProtoDesc("轴1_3")
	public int column_1_3;
	@Tag(6)
	@ProtoDesc("轴1_4")
	public int column_1_4;
	@Tag(7)
	@ProtoDesc("轴1_5")
	public int column_1_5;
	@Tag(8)
	@ProtoDesc("轴2_1")
	public int column_2_1;
	@Tag(9)
	@ProtoDesc("轴2_2")
	public int column_2_2;
	@Tag(10)
	@ProtoDesc("轴2_3")
	public int column_2_3;
	@Tag(11)
	@ProtoDesc("轴2_4")
	public int column_2_4;
	@Tag(12)
	@ProtoDesc("轴2_5")
	public int column_2_5;
	@Tag(13)
	@ProtoDesc("轴3_1")
	public int column_3_1;
	@Tag(14)
	@ProtoDesc("轴3_2")
	public int column_3_2;
	@Tag(15)
	@ProtoDesc("轴3_3")
	public int column_3_3;
	@Tag(16)
	@ProtoDesc("轴3_4")
	public int column_3_4;
	@Tag(17)
	@ProtoDesc("轴3_5")
	public int column_3_5;
	@Tag(18)
	@ProtoDesc("轴4_1")
	public int column_4_1;
	@Tag(19)
	@ProtoDesc("轴4_2")
	public int column_4_2;
	@Tag(20)
	@ProtoDesc("轴4_3")
	public int column_4_3;
	@Tag(21)
	@ProtoDesc("轴4_4")
	public int column_4_4;
	@Tag(22)
	@ProtoDesc("轴4_5")
	public int column_4_5;
	@Tag(23)
	@ProtoDesc("轴5_1")
	public int column_5_1;
	@Tag(24)
	@ProtoDesc("轴5_2")
	public int column_5_2;
	@Tag(25)
	@ProtoDesc("轴5_3")
	public int column_5_3;
	@Tag(26)
	@ProtoDesc("轴5_4")
	public int column_5_4;
	@Tag(27)
	@ProtoDesc("轴5_5")
	public int column_5_5;
	@Tag(28)
	@ProtoDesc("轴6_1")
	public int column_6_1;
	@Tag(29)
	@ProtoDesc("轴6_2")
	public int column_6_2;
	@Tag(30)
	@ProtoDesc("轴6_3")
	public int column_6_3;
	@Tag(31)
	@ProtoDesc("轴6_4")
	public int column_6_4;
	@Tag(32)
	@ProtoDesc("轴6_5")
	public int column_6_5;
	@Tag(33)
	@ProtoDesc("轴7_1")
	public int column_7_1;
	@Tag(34)
	@ProtoDesc("轴7_2")
	public int column_7_2;
	@Tag(35)
	@ProtoDesc("轴7_3")
	public int column_7_3;
	@Tag(36)
	@ProtoDesc("轴7_4")
	public int column_7_4;
	@Tag(37)
	@ProtoDesc("轴7_5")
	public int column_7_5;

 }
